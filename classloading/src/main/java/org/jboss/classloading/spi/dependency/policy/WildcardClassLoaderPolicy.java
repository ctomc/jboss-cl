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
package org.jboss.classloading.spi.dependency.policy;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class WildcardClassLoaderPolicy extends ClassLoaderPolicy implements ModuleRegistry
{
   /** The domain */
   private Domain domain;

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

   public WildcardClassLoaderPolicy(Domain domain, PackageRequirement requirement, Module module)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      if (requirement == null)
         throw new IllegalArgumentException("Null reqirement");
      if (module == null)
         throw new IllegalArgumentException("Null module");

      // Add the modules that can resolve the requirement
      for (Module aux : domain.getModules(null, null))
      {
         // The wildcard policy should not load from this module
         if (aux == module)
            continue;
         
         // Add the module if it can resolve the requirement
         if (aux.canResolve(requirement))
            modules.add(aux);
      }

      this.domain = domain;
      this.requirement = requirement;
      this.module = module;
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
            URL url = m.getResource(resource);
            if (url != null)
            {
               resourceCache.put(resource, m);
               used.add(m);
               return m;
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
            URL url = m.getResource(path);
            if (url != null)
            {
               resourceCache.put(path, m);
               used.add(m);
               return url;
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
            boolean visited = false;
            Enumeration<URL> eu = m.getResources(name);
            while (eu.hasMoreElements())
            {
               if (visited == false)
               {
                  used.add(m);
                  visited = true;
               }
               urls.add(eu.nextElement());
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
            if (isAncestor)
            {
               if (domain.isParentFirst())
               {
                  modules.add(0, current);
                  parentsBefore++;
               }
               else
                  modules.add(current);
            }
            else
               modules.add(parentsBefore, current);
         }

         reset();
      }
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
         if (lifeCycle != null)
         {
            if (current.isCascadeShutdown())
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
            else
            {
               // TODO -- make this module somehow available for refresh   
            }
         }
      }
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
