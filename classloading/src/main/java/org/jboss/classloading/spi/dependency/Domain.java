/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloading.plugins.metadata.ModuleRequirement;
import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.logging.Logger;

/**
 * Domain.
 * 
 * @author <a href="adrian@jboss.org">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class Domain implements ClassLoadingAdmin
{
   /** The log */
   private static final Logger log = Logger.getLogger(Domain.class);
   
   /** The domain name */
   private String name;

   /** The classloading */
   private ClassLoading classLoading;

   /** The parent domain name */
   private String parentDomainName;
   
   /** Whether we are parent first */
   private boolean parentFirst;
   
   /** The registered modules in registration order */
   private List<Module> modules = new CopyOnWriteArrayList<Module>();
   
   /** The registered modules by name */
   private Map<String, Module> modulesByName = new ConcurrentHashMap<String, Module>();
   
   /**
    * Create a new Domain.
    * 
    * @param classLoading the classloading 
    * @param name the name
    * @param parentDomainName  the parent domain name
    * @param parentFirst whether to check the parent first
    * @throws IllegalArgumentException for a null domain or classloading
    */
   public Domain(ClassLoading classLoading, String name, String parentDomainName, boolean parentFirst)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      if (classLoading == null)
         throw new IllegalArgumentException("Null classLoading");
      this.classLoading = classLoading;
      this.name = name;
      this.parentDomainName = parentDomainName;
      this.parentFirst = parentFirst;
   }

   /**
    * Get the name.
    * 
    * @return the name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get ClassLoading.
    *
    * @return the classloading
    */
   public ClassLoading getClassLoading()
   {
      return classLoading;
   }

   /**
    * Get the parentDomainName.
    * 
    * @return the parentDomainName.
    */
   public String getParentDomainName()
   {
      return parentDomainName;
   }

   public Domain getParentDomain()
   {
      if (parentDomainName != null)
         return classLoading.getDomain(parentDomainName);
      return null;
   }
   
   /**
    * Get the parentFirst.
    * 
    * @return the parentFirst.
    */
   public boolean isParentFirst()
   {
      return parentFirst;
   }

   /**
    * Add a module
    * 
    * @param module the module
    * @throws IllegalStateException if the module is already registered
    * @throws IllegalArgumentException for a null parameter
    */
   public synchronized void addModule(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");
      Domain domain = module.getDomain();
      if (domain != null)
         throw new IllegalArgumentException("The module is already registered with the domain " + domain.getName());
      String contextName = module.getContextName();
      if (modulesByName.containsKey(contextName))
         throw new IllegalArgumentException("The context " + contextName + " is already registered in domain " + getName());

      log.debug(this + " add module " + module);
      
      module.setDomain(this);
      modulesByName.put(contextName, module);
      modules.add(module);
      try
      {
         module.createDependencies();

         // Skip the classloader space checking when it is import all
         if (module.isImportAll() == false)
         {
            ClassLoadingSpace space = new ClassLoadingSpace();
            space.join(module);
         }
      }
      catch (Throwable t)
      {
         removeModule(module);
         if (t instanceof RuntimeException)
            throw (RuntimeException) t;
         else if (t instanceof Error)
            throw (Error) t;
         else
            throw new RuntimeException("Error adding module " + module, t);
      }
   }
   
   /**
    * Remove a deployment
    * 
    * @param module the module
    * @throws IllegalArgumentException for a null parameter
    */
   protected synchronized void removeModule(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");

      log.debug(this + " remove module " + module);

      ClassLoadingSpace space = module.getClassLoadingSpace();
      if (space != null)
         space.split(module);

      module.removeDependencies();
      modules.remove(module);
      modulesByName.remove(module.getContextName());
      module.setDomain(null);
   }

   /**
    * Get a module for a context name
    * 
    * @param name the context name
    * @return the module
    */
   public Module getModule(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null module name");

      Module module = modulesByName.get(name);
      if (module != null)
         return module;
      Domain parent = getParentDomain();
      if (parent != null)
         return parent.getModule(name);
      return null;
   }
   
   /**
    * Merges the capabilities provided by our global capabilities provider with the passed in capabilities.
    *
    * @param capabilities the capabilities list into which we want to add the global capabilities
    * @return the passed in capabilities with the global capabilities merged in
    */
   List<Capability> mergeGlobalCapabilities(List<Capability> capabilities)
   {
      return classLoading.mergeGlobalCapabilities(capabilities);
   }

   /**
    * Resolve a requirement to a module
    * 
    * @param module the module
    * @param requirement the requirement
    * @return the resolved name or null if not resolved
    */
   protected Module resolveModule(Module module, Requirement requirement)
   {
      // Try to resolve the module
      Module result = doResolveModule(module, requirement);
      if (result == null)
      {
         // If we have resolvers, try again if they find it
         if (classLoading.resolve(new ResolutionContext(this, module, requirement)))
            result = doResolveModule(module, requirement);
      }
      
      // If there is a result, check to see whether we need to resolve it
      if (result != null)
      {
         LifeCycle lifeCycle = result.getLifeCycle();
         if (lifeCycle != null && lifeCycle.isLazyResolve() && lifeCycle.isResolved() == false)
         {
            try
            {
               lifeCycle.resolve();
            }
            catch (Throwable t)
            {
               log.warn("Error in resolve for " + result, t);
            }
         }
      }
      
      return result;
   }   
   
   /**
    * Resolve a requirement to a module
    * 
    * @param module the module
    * @param requirement the requirement
    * @return the resolved name or null if not resolved
    */
   protected Module doResolveModule(Module module, Requirement requirement)
   {
      // First check the parent domain has been setup
      Domain parentDomain = null;
      if (parentDomainName != null)
      {
         parentDomain = getParentDomain();
         if (parentDomain == null)
            return null;
      }

      // Check the parent first when required
      if (parentDomain != null && parentFirst == true)
      {
         Module result = parentDomain.resolveModule(module, requirement);
         if (result != null)
            return result;
      }
      
      Module firstMatch = null;
      for (Module other : modules)
      {
         List<Capability> capabilities = other.getCapabilities();
         if (capabilities != null)
         {
            for (Capability capability : capabilities)
            {
               if (capability.resolves(module, requirement))
               {
                  if (firstMatch != null)
                  {
                     String otherName = other.getName() + ":" + other.getVersion(); 
                     String firstName = firstMatch.getName() + ":" + firstMatch.getVersion(); 
                     log.debug("Requirement " + requirement + " resolves agaist " + firstName + " and " + otherName + " - using first.");
                  }
                  if (firstMatch == null)
                     firstMatch = other;
               }
            }
         }
      }
      
      if (firstMatch != null)
         return firstMatch;

      // Check the parent afterwards when required
      if (parentDomain != null && parentFirst == false)
         return parentDomain.resolveModule(module, requirement);
      
      return null;
   }
   
   public Module getModuleForClass(Class<?> clazz)
   {
      return Module.getModuleForClass(clazz);
   }
   
   public Collection<Module> getModules(String name, VersionRange range)
   {
      Collection<Module> result = new HashSet<Module>();
      getModulesInternal(name, range, result);
      return result;
   }
   
   /**
    * Get the modules matching the name and range
    * 
    * If the given name is null, it matches all names. 
    * If the given range is null, it matches all versions. 
    * 
    * @param name the name or null
    * @param range the range or null
    * @param result the matching modules
    */
   void getModulesInternal(String name, VersionRange range, Collection<Module> result)
   {
      if (range == null)
         range = VersionRange.ALL_VERSIONS;
      
      for (Module module : modules)
      {
         List<Capability> capabilities = module.getCapabilitiesRaw();
         if (name != null && capabilities != null && capabilities.isEmpty() == false)
         {
            ModuleRequirement requirement = new ModuleRequirement(name, range);
            for (Capability capability : capabilities)
            {
               if (capability.resolves(module, requirement))
               {
                  result.add(module);
                  break;
               }
            }
         }
         else
         {
            boolean nameMatch = (name == null || name.equals(module.getName()));
            boolean versionMatch = range.isInRange(module.getVersion());
            if (nameMatch && versionMatch)
            {
               result.add(module);
            }
         }
      }
   }
   
   public Collection<ImportModule> getImportedModules(String name, VersionRange range)
   {
      Collection<ImportModule> result = new HashSet<ImportModule>();
      getImportingModulesInternal(name, range, result);
      return result;
   }
   
   /**
    * Get the importing modules matching the name and range
    * 
    * @param name the name
    * @param range the range
    * @param result the matching modules
    */
   void getImportingModulesInternal(String name, VersionRange range, Collection<ImportModule> result)
   {
      if (range == null)
         range = VersionRange.ALL_VERSIONS;
      
      for (Module module : modules)
      {
         List<RequirementDependencyItem> requirementDependencyItems = module.getDependencies();
         if (requirementDependencyItems != null && requirementDependencyItems.isEmpty() == false)
         {
            for (RequirementDependencyItem dependencyItem : requirementDependencyItems)
            {
               Requirement requirement = dependencyItem.getRequirement();
               if (requirement instanceof ModuleRequirement && dependencyItem.isResolved())
               {
                  ModuleRequirement moduleRequirement = (ModuleRequirement) requirement;
                  if (name == null || name.equals(moduleRequirement.getName()))
                  {
                     if (range.isConsistent(moduleRequirement.getVersionRange()))
                     {
                        Module other = module.resolveModule(dependencyItem, true);
                        ImportModule importModule = new ImportModule(other);
                        result.add(importModule);
                     }
                  }
               }
            }
         }
      }
   }
   
   public Collection<ExportPackage> getExportedPackages(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");
      return module.getExportedPackages();
   }
   
   public Collection<ExportPackage> getExportedPackages(String name, VersionRange range)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      
      Collection<ExportPackage> result = new HashSet<ExportPackage>();
      getExportedPackagesInternal(name, range, result);
      return result;
   }
   
   /**
    * Get the exported packages matching the name and range
    * 
    * @param name the name
    * @param range the range
    * @param result the matching modules
    */
   void getExportedPackagesInternal(String name, VersionRange range, Collection<ExportPackage> result)
   {
      if (range == null)
         range = VersionRange.ALL_VERSIONS;
      
      for (Module module : modules)
      {
         List<Capability> capabilities = module.getCapabilitiesRaw();
         if (capabilities != null && capabilities.isEmpty() == false)
         {
            PackageRequirement requirement = new PackageRequirement(name, range);
            for (Capability capability : capabilities)
            {
               if (capability instanceof PackageCapability && capability.resolves(module, requirement))
               {
                  ExportPackage exportPackage = new ExportPackage(module, (PackageCapability) capability);
                  result.add(exportPackage);
                  break;
               }
            }
         }
      }
   }

   public void refreshModules(Module... modules) throws Exception
   {
      Module.refreshModules(modules);
   }

   public boolean resolveModules(Module... modules) throws Exception
   {
      return Module.resolveModules(modules);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(super.toString());
      builder.append('{').append(getName()).append('}');
      return builder.toString();
   }
}
