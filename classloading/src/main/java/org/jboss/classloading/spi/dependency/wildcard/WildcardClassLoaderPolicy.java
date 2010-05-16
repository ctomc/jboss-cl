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
package org.jboss.classloading.spi.dependency.wildcard;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.base.ClassLoadingTask;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.*;
import org.jboss.util.collection.ConcurrentSet;

/**
 * WildcardClassLoaderPolicy.
 *
 * TODO -- lookup order might be wrong when some Module's are resolved lazily.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class WildcardClassLoaderPolicy extends ClassLoaderPolicy implements ModuleRegistry
{
   /** The domain */
   private Domain domain;

   /** The wildcard requirement dependency item */
   private WildcardRequirementDependencyItem item;

   /** The package requirement */
   private PackageRequirement requirement;

   /** The module */
   private Module module;

   /** The matching imported modules */
   private List<Module> modules = new CopyOnWriteArrayList<Module>();

   /** The parents before index */
   private int parentsBefore;

   /** The resources cache */
   private Map<String, Module> resourceCache = new ConcurrentHashMap<String, Module>();

   /** The used modules - track what we're using, so we know when to bounce */
   private Set<Module> used = new ConcurrentSet<Module>();

   public WildcardClassLoaderPolicy(Domain domain, WildcardRequirementDependencyItem item)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      if (item == null)
         throw new IllegalArgumentException("Null item");

      this.domain = domain;
      this.item = item;
      
      this.requirement = item.getRequirement();
      this.module = item.getModule();

      ClassLoading classLoading = domain.getClassLoading();      
      synchronized (this)
      {
         // Make sure we don't miss some Module
         // hence installing listener before doing the initial scan
         classLoading.addModuleRegistry(this);
         // Find existing matching modules
         fillModules(domain);
      }
   }

   /**
    * Is the module resolved.
    *
    * @param m the module
    * @return true if resolved, false otherwise
    */
   protected boolean isResolved(Module m)
   {
      ClassLoader cl = ClassLoading.getClassLoaderForModule(m);
      return cl != null;
   }

   /**
    * Add used.
    *
    * @param m the module
    */
   protected void addUsed(Module m)
   {
      // Add refresh info if the module is not cascade shutdown.
      if (used.add(m) && m.isCascadeShutdown() == false)
         item.addIDependOn(m);
   }

   /**
    * Find module which has a resource parameter.
    *
    * @param resource the resource
    * @return found module or null
    */
   protected Module findModule(String resource)
   {
      Module cached = resourceCache.get(resource);
      if (cached != null)
         return cached;

      ClassFilter filter = requirement.toClassFilter();
      if (filter.matchesResourcePath(resource))
      {
         for (Module m : modules)
         {
            if (isResolved(m))
            {
               URL url = m.getResource(resource);
               if (url != null)
               {
                  resourceCache.put(resource, m);
                  addUsed(m);
                  return m;
               }
            }
         }
      }
      return null;
   }

   public URL getResource(String path)
   {
      Module cached = resourceCache.get(path);
      if (cached != null)
         return cached.getResource(path);

      ClassFilter filter = requirement.toClassFilter();
      if (filter.matchesResourcePath(path))
      {
         for (Module m : modules)
         {
            if (isResolved(m))
            {
               URL url = m.getResource(path);
               if (url != null)
               {
                  resourceCache.put(path, m);
                  addUsed(m);
                  return url;
               }
            }
         }
      }
      return null;
   }

   public void getResources(String name, Set<URL> urls) throws IOException
   {
      ClassFilter filter = requirement.toClassFilter();
      if (filter.matchesResourcePath(name))
      {
         for (Module m : modules)
         {
            if (isResolved(m))
            {
               boolean visited = false;
               Enumeration<URL> eu = m.getResources(name);
               while (eu.hasMoreElements())
               {
                  if (visited == false)
                  {
                     addUsed(m);
                     visited = true;
                  }
                  urls.add(eu.nextElement());
               }
            }
         }
      }
   }

   @Override
   protected boolean isCacheable()
   {
      return false; // don't cache
   }

   /**
    * Get module's domain if it's connected to ours, null otherwise.
    *
    * @param module the module
    * @return module's domain if we're connected, null otherwise
    */
   protected Domain getDomain(Module module)
   {
      String domainName = module.getDeterminedDomainName();
      Domain current = domain;
      while (current != null && domainName.equals(domain.getName()) == false)
         current = current.getParentDomain();

      return current;
   }

   /**
    * Clear this policy.
    */
   protected void reset()
   {
      resourceCache.clear();
   }

   public void addModule(Module current)
   {
      Domain md = getDomain(current);
      if (md != null && current.canResolve(requirement))
      {
         boolean isAncestor = (domain != md); // not the same domain, so it must be ancestor
         synchronized (this)
         {
            boolean isParentFirst = domain.isParentFirst();
            addModule(current, isAncestor, isParentFirst);
         }
      }
   }

   /**
    * Add module, following order rules.
    * This method needs to be part of synch block or done in ctor.
    *
    * @param current the current module
    * @param isAncestor is ancestor
    * @param isParentFirst is parent first
    */
   private void addModule(Module current, boolean isAncestor, boolean isParentFirst)
   {
      if (isAncestor)
      {
         if (isParentFirst)
         {
            // leave previous parents infront of current module
            modules.add(parentsBefore, current);
            parentsBefore++;
         }
         else
            modules.add(current);
      }
      else
         modules.add(parentsBefore, current);
   }

   public void removeModule(Module current)
   {
      boolean sameModule = module == current;
      boolean resolvedModule = false;

      synchronized (this)
      {
         if (modules.remove(current))
         {
            if (sameModule == false)
            {
               resolvedModule = true; // we were part of matching modules, but not our module
               Domain md = getDomain(current);
               boolean isAncestor = (domain != md);
               if (isAncestor && domain.isParentFirst())
                  parentsBefore--;

            }
            reset();
         }
      }

      // Unregister this policy as module listener
      if (sameModule)
      {
         ClassLoading classLoading = domain.getClassLoading();
         classLoading.removeModuleRegistry(this);
         this.module = null;
      }

      // It's not us (we're already uninstalling) and we used this, let's bounce.
      if (resolvedModule && used.remove(current))
      {
         LifeCycle lifeCycle = module.getLifeCycle();
         // Non-cascade is updated / bounced via refresh
         if (lifeCycle != null && current.isCascadeShutdown())
         {
            try
            {
               lifeCycle.bounce(); // let's refresh the wired resources
            }
            catch (Exception e)
            {
               throw new RuntimeException("Error bouncing module: " + this.module, e);
            }
         }
      }
   }

   /**
    * Fill modules according to domain rules.
    *
    * @param current the current domain
    */
   protected void fillModules(Domain current)
   {
      Domain parent = current.getParentDomain();
      boolean parentFirst = current.isParentFirst();

      if (parent != null && parentFirst)
         fillModules(parent);

      Collection<ExportPackage> eps = current.getExportedPackages(requirement.getName(), requirement.getVersionRange());
      if (eps != null && eps.isEmpty() == false)
      {
         boolean isAncestor = (current != domain);
         boolean isParentFirst = domain.isParentFirst();
         for (ExportPackage ep : eps)
         {
            Module m = ep.getModule();
            addModule(m, isAncestor, isParentFirst);
         }
      }

      if (parent != null && parentFirst == false)
         fillModules(parent);
   }

   /**
    * Get BaseClassLoader from module.
    *
    * @param context the context
    * @return matching classloader or null
    */
   BaseClassLoader getBaseClassLoader(String context)
   {
      Module m = findModule(context);
      if (m != null)
      {
         ClassLoader cl = ClassLoading.getClassLoaderForModule(m);
         if (cl instanceof BaseClassLoader)
            return BaseClassLoader.class.cast(cl);
      }
      return null;
   }

   @Override
   protected BaseClassLoader getClassLoader(ClassLoadingTask task)
   {
      if (task == null)
         throw new IllegalArgumentException("Null task");

      String path = ClassLoaderUtils.classNameToPath(task.getClassName());
      return getBaseClassLoader(path);
   }
}
