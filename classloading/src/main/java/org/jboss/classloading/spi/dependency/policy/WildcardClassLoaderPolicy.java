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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.base.ClassLoadingTask;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.*;

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

   /** The moduke */
   private Module module;

   /** The matching imported modules */
   private volatile List<Module> modules;

   /** The resources cache */
   private Map<String, Module> resourceCache = new ConcurrentHashMap<String, Module>();

   public WildcardClassLoaderPolicy(Domain domain, PackageRequirement requirement, Module module)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      if (requirement == null)
         throw new IllegalArgumentException("Null reqirement");
      if (module == null)
         throw new IllegalArgumentException("Null module");
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
         for (Module m : getModules())
         {
            URL url = m.getResource(resource);
            if (url != null)
            {
               resourceCache.put(resource, m);
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
         for (Module m : getModules())
         {
            URL url = m.getResource(path);
            if (url != null)
            {
               resourceCache.put(path, m);
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
         for (Module m : getModules())
         {
            Enumeration<URL> eu = m.getResources(name);
            while (eu.hasMoreElements())
               urls.add(eu.nextElement());
         }
      }
   }

   @Override
   protected boolean isCacheable()
   {
      return false; // don't cache
   }

   protected void reset(Module module)
   {
      String domainName = module.getDeterminedDomainName();
      Domain current = domain;
      while (current != null && domainName.equals(domain.getName()) == false)
         current = current.getParentDomain();

      // We have a domain match, do reset
      if (current != null)
      {
         modules = null;
         resourceCache.clear();
      }
   }

   public void addModule(Module module)
   {
      reset(module);
   }

   public void removeModule(Module module)
   {
      reset(module);

      // Unregister this policy as module listener
      if (module == this.module)
      {
         ClassLoading classLoading = domain.getClassLoading();
         classLoading.removeModuleRegistry(this);
         this.module = null;
      }
   }

   /**
    * Lazy get modules.
    *
    * @return the matching modules
    */
   private List<Module> getModules()
   {
      if (modules == null)
      {
         List<Module> tmp = new ArrayList<Module>();
         List<ExportPackage> eps = getExportedPackages();
         for (ExportPackage ep : eps)
         {
            Module m = ep.getModule();
            if (m != module) // sanity check
               tmp.add(m);
         }
         modules = tmp;
      }
      return modules;
   }

   /**
    * Get matching imported modules.
    *
    * @return the matching import modules
    */
   private List<ExportPackage> getExportedPackages()
   {
      List<ExportPackage> modules = new ArrayList<ExportPackage>();
      fillModules(domain, modules);
      return modules;
   }

   /**
    * Fill modules according to domain rules.
    *
    * @param domain  the current domain
    * @param modules the modules to fill
    */
   private void fillModules(Domain domain, List<ExportPackage> modules)
   {
      Domain parent = domain.getParentDomain();
      boolean parentFirst = domain.isParentFirst();

      if (parent != null && parentFirst)
         fillModules(parent, modules);

      Collection<ExportPackage> eps = domain.getExportedPackages(requirement.getName(), requirement.getVersionRange());
      if (eps != null && eps.isEmpty() == false)
         modules.addAll(eps);

      if (parent != null && parentFirst == false)
         fillModules(parent, modules);
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
