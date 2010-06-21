/*
* JBoss, Home of Professional Open Source
* Copyright 2007, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.classloading.spi.dependency;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.plugins.metadata.PackageCapability.SplitPackagePolicy;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.logging.Logger;
import org.jboss.util.collection.ConcurrentSet;

/**
 * ClassLoadingSpace. This class does two stage join/resolve<p>
 * 
 * join - work out a module's capabilities/requirements and validate they are not inconsistent with what is already there
 * resolve - resolve new requirements and potentially join with other spaces
 * unjoin - remove a module from the space
 * unresolve - work out the new state after a module splits
 * 
 * TODO JBCL-25 handle split packages
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoadingSpace
{
   /** The log */
   private static final Logger log = Logger.getLogger(ClassLoadingSpace.class);

   /** Whether trace is enabled */
   private static boolean trace = log.isTraceEnabled();
   
   /** The modules */
   private Set<Module> modules = new ConcurrentSet<Module>();

   /** The modules by package */
   private Map<String, Module> modulesByPackage = new ConcurrentHashMap<String, Module>();
   
   /** The requirements for all modules */
   private Map<Module, List<RequirementDependencyItem>> requirements = new ConcurrentHashMap<Module, List<RequirementDependencyItem>>();
   
   /**
    * Get an unmodifiable set of the collections
    * 
    * @return the modules
    */
   public Set<Module> getModules()
   {
      return Collections.unmodifiableSet(modules);
   }
   
   /**
    * Join and resolve a module
    * 
    * @param module the module to add
    * @throws IllegalArgumentException for a null module
    */
   synchronized void joinAndResolve(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");

      trace = log.isTraceEnabled();
   
      join(module);
      try
      {
         resolve(module);
      }
      catch (Throwable t)
      {
         split(module);
         if (t instanceof RuntimeException)
            throw (RuntimeException) t;
         if (t instanceof Error)
            throw (Error) t;
         throw new RuntimeException(module + " could not join " + this, t);
      }
   }
   
   /**
    * Join with a set of modules
    * 
    * @param modules the modules
    * @throws IllegalArgumentException for null modules
    */
   synchronized void joinAndResolve(Set<Module> modules)
   {
      if (modules == null)
         throw new IllegalArgumentException("Null modules");

      Map<Module, ClassLoadingSpace> previous = new HashMap<Module, ClassLoadingSpace>();
      try
      {
         for (Module module : modules)
         {
            ClassLoadingSpace space = module.getClassLoadingSpace();
            join(module);
            previous.put(module, space);
            resolve(module);
         }
      }
      catch (Throwable t)
      {
         // Revert the previous joins
         for (Entry<Module, ClassLoadingSpace> entry : previous.entrySet())
         {
            Module module = entry.getKey();
            ClassLoadingSpace space = entry.getValue();
            
            split(module);
            try
            {
               space.join(module);
            }
            catch (Throwable t2)
            {
               log.error(module + " could not join " + space, t);
               throw new RuntimeException("BUG: " + module + " could not rejoin " + space + " after failing to join " + this, t2);
            }
         }
         if (t instanceof RuntimeException)
            throw (RuntimeException) t;
         if (t instanceof Error)
            throw (Error) t;
         throw new RuntimeException(modules + " could not join " + this, t);
      }
   }

   /**
    * Join with a classloading space
    * 
    * @param space the classloading space
    * @throws IllegalArgumentException for null space
    */
   void joinAndResolve(ClassLoadingSpace space)
   {
      if (space == null)
         throw new IllegalArgumentException("Null space");
      if (space == this)
         return;
      
      int ourSize = getModules().size();
      int otherSize = space.getModules().size();
      
      if (ourSize >= otherSize)
         joinAndResolve(space.getModules());
      else
         space.joinAndResolve(getModules());
   }
   
   /**
    * Split with a module
    * 
    * @param module the module to remove
    * @throws IllegalArgumentException for a null module
    * @throws IllegalStateException if the module is not associated with this classloading space
    */
   synchronized void split(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");

      ClassLoadingSpace other = module.getClassLoadingSpace();
      if (other != this)
         throw new IllegalStateException(module + " has the wrong classloading space: expected=" + this + " was " + other);

      unjoin(module);
      unresolve(module);
   }
   
   /**
    * Join with a module
    * 
    * @param module the module to add
    * @throws IllegalArgumentException for a null module
    */
   synchronized void join(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");
      // Nothing to do
      ClassLoadingSpace other = module.getClassLoadingSpace();
      if (other == this)
         return;
      
      if (trace)
         log.trace(module + " joining " + this);

      // The packages exported by this module (excluding optional packages)
      List<String> exportedPackages = module.determinePackageNames(false);
      
      // Check there are no conflicting packages
      if (exportedPackages != null && exportedPackages.isEmpty() == false)
      {
         for (String exportedPackage : exportedPackages)
         {
            Module otherModule = modulesByPackage.get(exportedPackage);
            if (otherModule != null)
            {
               PackageCapability exportCapability = module.getExportCapability(exportedPackage);
               if (exportCapability.getSplitPackagePolicy() == SplitPackagePolicy.Error)
               {
                  // TODO JBCL-22 ERRORS
                  throw new IllegalStateException(module + " cannot be added because it is exports package " + exportedPackage + " which conflicts with " + otherModule);
               }
            }
         }
      }

      // Check our requirements are consistent with the other requirements
      List<RequirementDependencyItem> moduleDependencies = module.getDependencies();
      if (requirements.isEmpty() == false)
      {
         if (moduleDependencies != null && moduleDependencies.isEmpty() == false)
         {
            for (RequirementDependencyItem dependency : moduleDependencies)
            {
               Requirement requirement = dependency.getRequirement();
               for (Entry<Module, List<RequirementDependencyItem>> entry : requirements.entrySet())
               {
                  Module otherModule = entry.getKey();
                  List<RequirementDependencyItem> dependencies = entry.getValue();
                  for (RequirementDependencyItem otherDependency : dependencies)
                  {
                     Requirement otherRequirement = otherDependency.getRequirement();
                     // TODO JBCL-22 ERRORS
                     if (requirement.isConsistent(otherRequirement) == false)
                        throw new IllegalStateException(module + " has a requirement " + requirement + " which is inconsistent with " + otherRequirement + " from " + otherModule);
                  }
               }
            }
         }
      }
      
      // Update the exported packages
      if (exportedPackages != null && exportedPackages.isEmpty() == false)
      {
         for (String exportedPackage : exportedPackages)
         {
            Module firstModule = modulesByPackage.get(exportedPackage);
            PackageCapability exportCapability = module.getExportCapability(exportedPackage);
            if (firstModule == null || exportCapability.getSplitPackagePolicy() == SplitPackagePolicy.Last)
               modulesByPackage.put(exportedPackage, module);
         }
      }
      
      // Remember the module requirements
      if (moduleDependencies != null && moduleDependencies.isEmpty() == false)
         requirements.put(module, moduleDependencies);
      
      // Remove from any previous space
      if (other != null)
         other.split(module);
      
      // This module is now part of our space
      modules.add(module);
      module.setClassLoadingSpace(this);
   }
   
   /**
    * Unjoin a module
    * 
    * @param module the module to remove
    */
   private void unjoin(Module module)
   {
      if (trace)
         log.trace(module + " unjoining " + this);
      
      // Remove the exported packages for this module
      List<String> packageNames = module.determinePackageNames(false);
      if (packageNames != null)
      {
         for (String packageName : packageNames)
         {
            Module other = modulesByPackage.get(packageName);
            if (other == module)
               modulesByPackage.remove(packageName);
         }
      }

      // Remove the module requirements from the classloading space
      requirements.remove(module);
      
      // No longer part of this classloading space
      modules.remove(module);
      module.setClassLoadingSpace(null);
   }
   
   /**
    * Resolve a module
    * 
    * @param module the module to resolve
    */
   synchronized void resolve(Module module)
   {
      if (trace)
         log.trace(module + " resolving " + this);

      List<RequirementDependencyItem> moduleDependencies = requirements.get(module);
      if (moduleDependencies != null)
      {
         for (RequirementDependencyItem dependency : moduleDependencies)
         {
            if (dependency.isResolved() == false)
            {
               Module otherModule = module.resolveModule(dependency, false);
               if (otherModule != null)
               {
                  // Do we need to join with another classloading space?
                  ClassLoadingSpace space = otherModule.getClassLoadingSpace();
                  if (space != null && space != this)
                  {
                     space.joinAndResolve(this);
                  }
               }
            }
         }
      }
   }
   
   /**
    * Unresolve a module
    * 
    * @param module the module to resolve
    */
   private void unresolve(Module module)
   {
      if (trace)
         log.trace(module + " unresolving " + this);
      
      // Nothing yet. Could try to split classloading spaces if they now have disjoint subsets?
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder("ClassLoadingSpace: ");
      builder.append("modules: ").append(modules).append(", ");
      builder.append("modulesByPackages: ").append(modulesByPackage).append(", ");
      builder.append("requirements: ").append(requirements);
      return builder.toString();
   }
}
