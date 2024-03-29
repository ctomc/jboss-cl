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
package org.jboss.classloader.spi.base;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.CacheLoader;
import org.jboss.classloader.spi.ClassLoaderCache;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ImportType;
import org.jboss.classloader.spi.Loader;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloader.spi.translator.TranslatorUtils;
import org.jboss.logging.Logger;
import org.jboss.util.collection.ConcurrentSet;
import org.jboss.util.loading.Translator;

/**
 * BaseClassLoaderDomain.<p>
 * 
 * This class hides some of the implementation details and allows
 * package access to the protected methods.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class BaseClassLoaderDomain implements CacheLoader
{
   /** The log */
   private static final Logger log = Logger.getLogger(BaseClassLoaderDomain.class);

   /** The classloader system to which we belong */
   private BaseClassLoaderSystem system;
   
   /** The translators */
   private List<Translator> translators;

   /** The classloaders  in the order they were registered */
   private List<ClassLoaderInformation> classLoaders = new CopyOnWriteArrayList<ClassLoaderInformation>();
   
   /** The classloaders by package name */
   private Map<String, List<ClassLoaderInformation>> classLoadersByPackageName = new ConcurrentHashMap<String, List<ClassLoaderInformation>>();

   /** The global class cache */
   private Map<String, ClassCacheItem> globalClassCache = new ConcurrentHashMap<String, ClassCacheItem>();
   
   /** The global class black list */
   private Set<String> globalClassBlackList = new ConcurrentSet<String>();

   /** The global resource cache */
   private Map<String, URL> globalResourceCache = new ConcurrentHashMap<String, URL>();
   
   /** The global resource black list */
   private Set<String> globalResourceBlackList = new ConcurrentSet<String>();
   
   /** Keep track of the added order */
   private int order = 0;
   
   /**
    * Flush the internal caches
    */
   public void flushCaches()
   {
      globalClassCache.clear();
      globalClassBlackList.clear();
      globalResourceCache.clear();
      globalResourceBlackList.clear();

      for (ClassLoaderInformation info : classLoaders)
         info.flushCaches();
   }

   public int getClassBlackListSize()
   {
      return globalClassBlackList.size();
   }

   public int getClassCacheSize()
   {
      return globalClassCache.size();
   }

   public int getResourceBlackListSize()
   {
      return globalResourceBlackList.size();
   }

   public int getResourceCacheSize()
   {
      return globalResourceCache.size();
   }
   
   public Set<String> listClassBlackList()
   {
      return Collections.unmodifiableSet(globalClassBlackList);
   }

   public Map<String, String> listClassCache()
   {
      Map<String, String> result = new HashMap<String, String>(globalClassCache.size());
      for (Map.Entry<String, ClassCacheItem> entry : globalClassCache.entrySet())
         result.put(entry.getKey(), entry.getValue().toString());
      return result;
   }

   public Set<String> listResourceBlackList()
   {
      return Collections.unmodifiableSet(globalResourceBlackList);
   }

   public Map<String, URL> listResourceCache()
   {
      return Collections.unmodifiableMap(globalResourceCache);
   }

   /**
    * Get the classloader system
    * 
    * @return the classloader system
    */
   protected synchronized BaseClassLoaderSystem getClassLoaderSystem()
   {
      return system;
   }
   
   /**
    * Get the classloader system
    * 
    * @param system the classloader system
    */
   synchronized void setClassLoaderSystem(BaseClassLoaderSystem system)
   {
      if (system == null)
         shutdownDomain();
      this.system = system;
   }

   /**
    * Shutdown the domain<p>
    * 
    * The default implementation just unregisters all classloaders
    */
   protected void shutdownDomain()
   {
      log.debug(toString() + " shutdown!");

      // Unregister all classloaders
      while (true)
      {
         Iterator<ClassLoaderInformation> iterator = classLoaders.iterator();
         if (iterator.hasNext() == false)
            break;

         while (iterator.hasNext())
         {
            ClassLoaderInformation info = iterator.next();
            if (info != null)
               unregisterClassLoader(info.getClassLoader());
         }
      }
      
      flushCaches();
   }
   
   /**
    * Whether the domain has classloaders
    * 
    * @return true when the domain has classloaders
    */
   public boolean hasClassLoaders()
   {
      return classLoaders.isEmpty() == false;
   }
   
   /**
    * Whether to use load class for parent
    * 
    * @return true to load class on the parent loader
    */
   public abstract boolean isUseLoadClassForParent();

   /**
    * Get the shutdownPolicy.
    * 
    * @return the shutdownPolicy.
    */
   protected abstract ShutdownPolicy getShutdownPolicy();

   /**
    * Transform the byte code<p>
    * 
    * By default, this delegates to the classloader system
    * 
    * @param classLoader the classloader
    * @param className the class name
    * @param byteCode the byte code
    * @param protectionDomain the protection domain
    * @return the transformed byte code
    * @throws Exception for any error
    */
   protected byte[] transform(ClassLoader classLoader, String className, byte[] byteCode, ProtectionDomain protectionDomain) throws Exception
   {
      byte[] result = byteCode;

      BaseClassLoaderSystem system = getClassLoaderSystem();
      if (system != null)
         result = system.transform(classLoader, className, result, protectionDomain);

      return TranslatorUtils.applyTranslatorsOnTransform(getTranslators(), classLoader, className, result, protectionDomain);
   }

   /**
    * Load a class from the domain
    * 
    * @param classLoader the classloader
    * @param name the class name
    * @param allExports whether we should look at all exports
    * @return the class
    * @throws ClassNotFoundException for any error
    */
   protected Class<?> loadClass(BaseClassLoader classLoader, String name, boolean allExports) throws ClassNotFoundException
   {
      boolean trace = log.isTraceEnabled();
      
      String path = ClassLoaderUtils.classNameToPath(name);

      // JBCL-101 TODO need to rework the blacklist
      //checkClassBlackList(classLoader, name, path, allExports, true);
      
      boolean findInParent = (isUseLoadClassForParent() == false);
      
      // Should we directly load from the parent?
      if (findInParent == false)
      {
         Class<?> clazz = loadClassBefore(name);
         if (clazz != null)
            return clazz;
      }
      
      Loader loader = findLoader(classLoader, path, allExports, findInParent);
      if (loader != null)
      {
         Thread thread = Thread.currentThread();
         ClassLoadingTask task = new ClassLoadingTask(name, classLoader, thread);
         ClassLoaderManager.scheduleTask(task, loader, false);
         Class<?> result = ClassLoaderManager.process(thread, task);
         ClassCacheItem item = globalClassCache.get(path);
         if (item != null)
            item.clazz = result;
         return result;
      }
      
      // Should we directly load from the parent?
      if (findInParent == false)
      {
         Class<?> clazz = loadClassAfter(name);
         if (clazz != null)
            return clazz;
      }

      // Finally see whether this is the JDK assuming it can load its classes from any classloader
      if (classLoader != null)
      {
         BaseClassLoaderPolicy policy = classLoader.getPolicy();
         ClassLoader hack = policy.isJDKRequest(name);
         if (hack != null)
         {
            if (trace)
               log.trace(this + " trying to load " + name + " using hack " + hack);
            Class<?> result = Class.forName(name, false, hack);
            if (result != null)
            {
               if (trace)
                  log.trace(this + " loaded from hack " + hack + " " + ClassLoaderUtils.classToString(result));
               globalClassCache.put(path, new ClassCacheItem(result));
               return result;
            }
         }
      }
      
      // Didn't find it
      return null;
   }

   /**
    * Find a loader for a class
    * 
    * @param name the class resource name
    * @return the loader
    */
   protected Loader findLoader(String name)
   {
      return findLoader(null, name, true, true);
   }

   /**
    * Find a loader for a class
    * 
    * @param classLoader the classloader
    * @param path the class resource name
    * @param allExports whether we should look at all exports
    * @param findInParent should we try the parent
    * @return the loader
    */
   Loader findLoader(BaseClassLoader classLoader, String path, boolean allExports, boolean findInParent)
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " findLoader " + path + " classLoader=" + classLoader + " allExports=" + allExports + " findInParent=" + findInParent);
      
      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());
      
      // Try the before attempt (e.g. from the parent)
      Loader loader = null;
      if (findInParent)
         loader = findBeforeLoader(path);
      if (loader != null)
         return loader;

      // Work out the rules
      ClassLoaderCache cache = null;
      BaseClassLoaderPolicy policy;
      if (classLoader != null)
      {
         policy = classLoader.getPolicy();
         cache = policy.getCache();
         if (policy.isImportAll())
            allExports = true;
      }

      // Next we try the old "big ball of mud" model      
      if (allExports)
      {
         loader = findLoaderInExports(classLoader, path, trace);
         if (loader != null)
            return loader;
      }
      else if (trace)
         log.trace(this + " not loading " + path + " from all exports");
      
      // Next we try the before imports
      if (cache != null)
      {
         loader = findLoaderInImports(cache, path, ImportType.BEFORE, trace);
         if (loader != null)
            return loader;
      }

      // Next use any requesting classloader, this will look at everything not just what it exports
      if (classLoader != null)
      {
         if (trace)
            log.trace(this + " trying to load " + path + " from requesting " + classLoader);
         if (classLoader.getResourceLocally(path) != null)
         {
            loader = classLoader.getLoader();
            policy = classLoader.getPolicy();
            if (policy.isCacheable())
               globalClassCache.put(path, new ClassCacheItem(loader));
            return loader;
         }
      }

      // Next we try the after imports
      if (cache != null)
      {
         loader = findLoaderInImports(cache, path, ImportType.AFTER, trace);
         if (loader != null)
            return loader;
      }

      // Try the after attempt (e.g. from the parent)
      if (findInParent)
         return findAfterLoader(path);
      
      return null;
   }
   
   /**
    * Load a resource from the domain
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param allExports whether we should look at all exports
    * @return the url
    */
   URL getResource(BaseClassLoader classLoader, String name, boolean allExports)
   {
      boolean trace = log.isTraceEnabled();

      // Try the classloader first
      if (classLoader != null)
      {
         if (trace)
            log.trace(this + " trying to get resource " + name + " from requesting " + classLoader);
         URL result = classLoader.getResourceLocally(name);
         if (result != null)
         {
            if (trace)
               log.trace(this + " got resource from requesting " + classLoader + " " + result);
            return result;
         }
      }

      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());

      // Try the before attempt
      URL result = beforeGetResource(name);
      if (result != null)
         return result;

      // Work out the rules
      ClassLoaderCache cache = null;
      BaseClassLoaderPolicy policy;
      if (classLoader != null)
      {
         policy = classLoader.getPolicy();
         cache = policy.getCache();
         if (policy.isImportAll())
            allExports = true;
      }

      // Next we try the old "big ball of mud" model      
      if (allExports)
      {
         result = getResourceFromExports(classLoader, name, trace);
         if (result != null)
            return result;
      }
      else if (trace)
         log.trace(this + " not getting resource " + name + " from all exports");
      
      // Next we try the imports
      if (cache != null)
      {
         result = getResourceFromImports(cache, name, ImportType.ALL, trace);
         if (result != null)
            return result;
      }

      // Try the after attempt
      result = afterGetResource(name);
      if (result != null)
         return result;
      
      // Didn't find it
      return null;
   }
   
   /**
    * Load resources from the domain
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param allExports whether we should look at all exports
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResources(BaseClassLoader classLoader, String name, Set<URL> urls, boolean allExports) throws IOException
   {
      boolean trace = log.isTraceEnabled();

      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());

      // Try the before attempt
      beforeGetResources(name, urls);

      // Work out the rules
      ClassLoaderInformation info = null;
      BaseClassLoaderPolicy policy;
      if (classLoader != null)
      {
         policy = classLoader.getPolicy();
         info = policy.getInformation();
         if (policy.isImportAll())
            allExports = true;
      }

      // Next we try the old "big ball of mud" model      
      if (allExports)
         getResourcesFromExports(classLoader, name, urls, trace);
      else if (trace)
         log.trace(this + " not getting resource " + name + " from all exports");
      
      // Next we try the imports
      if (info != null)
         getResourcesFromImports(info, name, urls, ImportType.ALL, trace);

      // Finally use any requesting classloader
      if (classLoader != null)
      {
         if (trace)
            log.trace(this + " trying to get resources " + name + " from requesting " + classLoader);
         classLoader.getResourcesLocally(name, urls);
      }

      // Try the after attempt
      afterGetResources(name, urls);
   }
   
   /**
    * Load a package from the domain
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param allExports whether we should look at all exports
    * @return the package
    */
   Package getPackage(BaseClassLoader classLoader, String name, boolean allExports)
   {
      boolean trace = log.isTraceEnabled();

      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());

      // Try the before attempt
      Package result = beforeGetPackage(name);
      if (result != null)
         return result;

      // Work out the rules
      ClassLoaderInformation info = null;
      BaseClassLoaderPolicy policy;
      if (classLoader != null)
      {
         policy = classLoader.getPolicy();
         info = policy.getInformation();
         if (policy.isImportAll())
            allExports = true;
      }

      // Next we try the old "big ball of mud" model      
      if (allExports)
      {
         result = getPackageFromExports(classLoader, name, trace);
         if (result != null)
            return result;
      }
      else if (trace)
         log.trace(this + " not getting package " + name + " from all exports");
      
      // Next we try the before imports
      if (info != null)
      {
         result = getPackageFromImports(info, name, ImportType.BEFORE, trace);
         if (result != null)
            return result;
      }

      // Finally use any requesting classloader
      if (classLoader != null)
      {
         if (trace)
            log.trace(this + " trying to get package " + name + " from requesting " + classLoader);
         result = classLoader.getPackageLocally(name);
         if (result != null)
         {
            if (trace)
               log.trace(this + " got package from requesting " + classLoader + " " + result);
            return result;
         }
      }

      // Next we try the after imports
      if (info != null)
      {
         result = getPackageFromImports(info, name, ImportType.AFTER, trace);
         if (result != null)
            return result;
      }

      // Try the after attempt
      result = afterGetPackage(name);
      if (result != null)
         return result;
      
      // Didn't find it
      return null;
   }
   
   /**
    * Load packages from the domain
    * 
    * @param classLoader the classloader
    * @param packages the packages to add to
    * @param allExports whether we should look at all exports
    */
   void getPackages(BaseClassLoader classLoader, Set<Package> packages, boolean allExports)
   {
      boolean trace = log.isTraceEnabled();

      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());

      // Try the before attempt
      beforeGetPackages(packages);

      // Work out the rules
      ClassLoaderInformation info = null;
      BaseClassLoaderPolicy policy;
      if (classLoader != null)
      {
         policy = classLoader.getPolicy();
         info = policy.getInformation();
         if (policy.isImportAll())
            allExports = true;
      }

      // Next we try the old "big ball of mud" model      
      if (allExports)
         getPackagesFromExports(classLoader, packages, trace);
      else if (trace)
         log.trace(this + " not getting packages from all exports");
      
      // Next we try the imports
      if (info != null)
         getPackagesFromImports(info, packages, ImportType.ALL, trace);

      // Finally use any requesting classloader
      if (classLoader != null)
      {
         if (trace)
            log.trace(this + " trying to get packages from requesting " + classLoader);
         classLoader.getPackagesLocally(packages);
      }

      // Try the after attempt
      afterGetPackages(packages);
   }
   
   /**
    * Find a loader for class in exports
    * 
    * @param classLoader the classloader
    * @param name the class resource name
    * @param trace whether trace is enabled
    * @return the loader
    */
   private Loader findLoaderInExports(BaseClassLoader classLoader, String name, boolean trace)
   {
      ClassCacheItem item = globalClassCache.get(name);
      if (item != null)
      {
         Loader loader = item.loader;
         if (loader != null)
         {
            if (trace)
               log.trace(this + " found loader " + loader + " in global class cache " + name);
            return loader;
         }
      }

      if (isBlackListedClass(name))
      {
         if (trace)
            log.trace(this + " class is black listed " + name);
         return null;
      }

      boolean canCache = true;
      boolean canBlackList = true;
      
      String packageName = ClassLoaderUtils.getResourcePackageName(name);
      List<ClassLoaderInformation> list = classLoadersByPackageName.get(packageName);
      if (trace)
         log.trace(this + " trying to load " + name + " from all exports of package " + packageName + " " + list);
      if (list != null && list.isEmpty() == false)
      {
         for (ClassLoaderInformation info : list)
         {
            BaseDelegateLoader exported = info.getExported();
            
            // See whether the policies allow caching/blacklisting
            BaseClassLoaderPolicy loaderPolicy = exported.getPolicy();
            if (loaderPolicy == null || loaderPolicy.isCacheable() == false)
               canCache = false;
            if (loaderPolicy == null || loaderPolicy.isBlackListable() == false)
               canBlackList = false;

            if (exported.getResource(name) != null)
            {
               if (canCache)
                  globalClassCache.put(name, new ClassCacheItem(exported));
               return exported;
            }
         }
      }
      // Here is not found in the exports so can we blacklist it?
      if (canBlackList)
         globalClassBlackList.add(name);
      
      return null;
   }

   /**
    * Check whether this is a black listed class
    *
    * @param name the class name
    * @return true when black listed, false otherwise
    */
   protected boolean isBlackListedClass(String name)
   {
      return globalClassBlackList.contains(name);
   }

   /**
    * Load a resource from the exports
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param trace whether trace is enabled
    * @return the url
    */
   private URL getResourceFromExports(BaseClassLoader classLoader, String name, boolean trace)
   {
      URL result = globalResourceCache.get(name);
      if (result != null)
      {
         if (trace)
            log.trace(this + " got resource from cache " + name);
      }
      
      if (globalResourceBlackList.contains(name))
      {
         if (trace)
            log.trace(this + " resource is black listed, not looking at exports " + name);
         return null;
      }

      boolean canCache = true;
      boolean canBlackList = true;
      
      String packageName = ClassLoaderUtils.getResourcePackageName(name);
      List<ClassLoaderInformation> list = classLoadersByPackageName.get(packageName);
      if (trace)
         log.trace(this + " trying to get resource " + name + " from all exports " + list);
      if (list != null && list.isEmpty() == false)
      {
         for (ClassLoaderInformation info : list)
         {
            BaseDelegateLoader loader = info.getExported();
            
            // See whether the policies allow caching/blacklisting
            BaseClassLoaderPolicy loaderPolicy = loader.getPolicy();
            if (loaderPolicy == null || loaderPolicy.isCacheable() == false)
               canCache = false;
            if (loaderPolicy == null || loaderPolicy.isBlackListable() == false)
               canBlackList = false;

            result = loader.getResource(name);
            if (result != null)
            {
               if (canCache)
                  globalResourceCache.put(name, result);
               return result;
            }
         }
      }
      // Here is not found in the exports so can we blacklist it?
      if (canBlackList)
         globalResourceBlackList.add(name);
      
      return null;
   }
   
   /**
    * Load resources from the exports
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param urls the urls to add to
    * @param trace whether trace is enabled
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResourcesFromExports(BaseClassLoader classLoader, String name, Set<URL> urls, boolean trace) throws IOException
   {
      String packageName = ClassLoaderUtils.getResourcePackageName(name);
      List<ClassLoaderInformation> list = classLoadersByPackageName.get(packageName);
      if (trace)
         log.trace(this + " trying to get resources " + name + " from all exports " + list);
      if (list != null && list.isEmpty() == false)
      {
         for (ClassLoaderInformation info : list)
         {
            BaseDelegateLoader loader = info.getExported();
            loader.getResources(name, urls);
         }
      }
   }
   
   /**
    * Load a package from the exports
    * 
    * @param classLoader the classloader
    * @param name the package name
    * @param trace whether trace is enabled
    * @return the package
    */
   private Package getPackageFromExports(BaseClassLoader classLoader, String name, boolean trace)
   {
      List<ClassLoaderInformation> list = classLoadersByPackageName.get(name);
      if (trace)
         log.trace(this + " trying to get package " + name + " from all exports " + list);
      if (list != null && list.isEmpty() == false)
      {
         for (ClassLoaderInformation info : list)
         {
            BaseDelegateLoader loader = info.getExported();

            Package result = loader.getPackage(name);
            if (result != null)
               return result;
         }
      }
      return null;
   }
   
   /**
    * Load packages from the exports
    * 
    * @param classLoader the classloader
    * @param packages the packages to add to
    * @param trace whether trace is enabled
    */
   void getPackagesFromExports(BaseClassLoader classLoader, Set<Package> packages, boolean trace)
   {
      List<ClassLoaderInformation> list = classLoaders;
      if (trace)
         log.trace(this + " trying to get all packages from all exports " + list);
      if (list != null && list.isEmpty() == false)
      {
         for (ClassLoaderInformation info : list)
         {
            BaseDelegateLoader loader = info.getExported();
            loader.getPackages(packages);
         }
      }
   }

   /**
    * Find a loader for a class in imports
    *
    * @param cache the classloader cache
    * @param name the class resource name
    * @param type the import type
    * @param trace whether trace is enabled
    * @return the loader
    */
   Loader findLoaderInImports(ClassLoaderCache cache, String name, ImportType type, boolean trace)
   {
      boolean relevant = cache.isRelevant(type);
      if (relevant == false)
      {
         if (trace)
            log.trace(this + " not loading " + name + " from imports, it's not relevant: " + cache.getInfo(type));
         return null;
      }

      Loader loader = cache.getCachedLoader(name);
      if (loader != null)
      {
         if (trace)
            log.trace(this + " found in import cache " + name);
         return loader;
      }
      
      if (cache.isBlackListedClass(name))
      {
         if (trace)
            log.trace(this + " class is black listed in imports " + name);
         return null;
      }

      if (trace)
         log.trace(this + " trying to load " + name + " from imports: " + cache.getInfo(type));

      loader = cache.findLoader(type, name);
      if (loader != null)
         return loader;

      if (type == ImportType.AFTER) // TODO -- is this really OK?
         cache.blackListClass(name);
      return null;
   }
   
   /**
    * Load a resource from the imports
    * 
    * @param cache the classloader cache
    * @param name the resource name
    * @param type the import type
    * @param trace whether trace is enabled
    * @return the url
    */
   private URL getResourceFromImports(ClassLoaderCache cache, String name, ImportType type, boolean trace)
   {
      boolean relevant = cache.isRelevant(type);
      if (relevant == false)
      {
         if (trace)
            log.trace(this + " not getting resource " + name + " from imports, it's not relevant: " + cache.getInfo(type));
         return null;
      }

      URL url = cache.getCachedResource(name);
      if (url != null)
      {
         if (trace)
            log.trace(this + " found resource in import cache " + name);
         return url;
      }
      
      if (cache.isBlackListedResource(name))
      {
         if (trace)
            log.trace(this + " resource is black listed in imports " + name);
         return null;
      }

      if (trace)
         log.trace(this + " trying to get resource " + name + " from imports: " + cache.getInfo(type));


      url = cache.findResource(type, name);
      if (url != null)
         return url;

      if (type == ImportType.AFTER) // TODO -- check
         cache.blackListResource(name);
      return null;
   }
   
   /**
    * Load resources from the imports
    * 
    * @param info the classloader info
    * @param name the resource name
    * @param urls the urls to add to
    * @param type the import type
    * @param trace whether trace is enabled
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResourcesFromImports(ClassLoaderInformation info, String name, Set<URL> urls, ImportType type, boolean trace) throws IOException
   {
      List<? extends DelegateLoader> delegates = info.getDelegates(type);
      if (delegates == null || delegates.isEmpty())
      {
         if (trace)
            log.trace(this + " not getting resource " + name + " from imports it has no delegates");
         return;
      }
      if (trace)
         log.trace(this + " trying to get resources " + name + " from imports " + delegates + " for " + info.getClassLoader());
      for (DelegateLoader delegate : delegates)
         delegate.getResources(name, urls);
   }
   
   /**
    * Load a package from the imports
    * 
    * @param info the classloader information
    * @param name the pacakge name
    * @param type the import type
    * @param trace whether trace is enabled
    * @return the package
    */
   private Package getPackageFromImports(ClassLoaderInformation info, String name, ImportType type, boolean trace)
   {
      List<? extends DelegateLoader> delegates = info.getDelegates(type);
      if (delegates == null || delegates.isEmpty())
      {
         if (trace)
            log.trace(this + " not getting package " + name + " from imports it has no delegates");
         return null;
      }

      if (trace)
         log.trace(this + " trying to get package " + name + " from imports " + delegates + " for " + info.getClassLoader());

      for (DelegateLoader delegate : delegates)
      {
         Package result = delegate.getPackage(name);
         if (result != null)
            return result;
      }
      return null;
   }
   
   /**
    * Load packages from the imports
    * 
    * @param info the classloader info
    * @param packages the packages to add to
    * @param type the import type
    * @param trace whether trace is enabled
    */
   void getPackagesFromImports(ClassLoaderInformation info, Set<Package> packages, ImportType type, boolean trace)
   {
      List<? extends DelegateLoader> delegates = info.getDelegates(type);
      if (delegates == null || delegates.isEmpty())
      {
         if (trace)
            log.trace(this + " not getting all packages from imports it has no delegates");
         return;
      }
      if (trace)
         log.trace(this + " trying to get all pacakges from imports " + delegates + " for " + info.getClassLoader());
      for (DelegateLoader delegate : delegates)
         delegate.getPackages(packages);
   }

   /**
    * Invoked before classloading is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param name the class name
    * @return the loader if found or null otherwise
    */
   protected abstract Class<?> loadClassBefore(String name);
   
   /**
    * Invoked after classloading is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param name the class name
    * @return the loader if found or null otherwise
    */
   protected abstract Class<?> loadClassAfter(String name);

   /**
    * Invoked before classloading is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param name the class resource name
    * @return the loader if found or null otherwise
    */
   protected abstract Loader findBeforeLoader(String name);
   
   /**
    * Invoked after classloading is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param name the class resource name
    * @return the loader if found or null otherwise
    */
   protected abstract Loader findAfterLoader(String name);
   
   /**
    * Invoked before getResources is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param name the resource name
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   protected abstract void beforeGetResources(String name,  Set<URL> urls) throws IOException;
   
   /**
    * Invoked after getResources is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param name the resource name
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   protected abstract void afterGetResources(String name, Set<URL> urls) throws IOException;
   
   /**
    * Invoked before getResource is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param name the resource name
    * @return the url if found or null otherwise
    */
   protected abstract URL beforeGetResource(String name);
   
   /**
    * Invoked after getResource is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param name the resource name
    * @return the url if found or null otherwise
    */
   protected abstract URL afterGetResource(String name);
   
   /**
    * Invoked before getPackages is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param packages the packages to add to
    */
   protected abstract void beforeGetPackages(Set<Package> packages);
   
   /**
    * Invoked after getPackages is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param packages the packages to add to
    */
   protected abstract void afterGetPackages(Set<Package> packages);
   
   /**
    * Invoked before getPackage is attempted to allow a preload attempt, e.g. from the parent
    * 
    * @param name the package name
    * @return the package if found or null otherwise
    */
   protected abstract Package beforeGetPackage(String name);
   
   /**
    * Invoked after getPackage is attempted to allow a postload attempt, e.g. from the parent
    * 
    * @param name the package name
    * @return the url if found or null otherwise
    */
   protected abstract Package afterGetPackage(String name);
   
   public Class<?> loadClass(String name)
   {
      try
      {
         return loadClass(null, name, true);
      }
      catch (ClassNotFoundException e)
      {
         return null;
      }
   }
   
   /**
    * Load a class from the domain
    * 
    * @param classLoader the classloader
    * @param name the class name
    * @return the class
    * @throws ClassNotFoundException for any error
    */
   Class<?> loadClass(BaseClassLoader classLoader, String name) throws ClassNotFoundException
   {
      return loadClass(classLoader, name, false);
   }
   
   public URL getResource(String name)
   {
      return getResource(null, name, true);
   }
   
   /**
    * Get a resource from the domain
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @return the url
    */
   URL getResource(BaseClassLoader classLoader, String name)
   {
      return getResource(classLoader, name, false);
   }
   
   // FindBugs: The Set doesn't use equals/hashCode
   public void getResources(String name, Set<URL> urls) throws IOException
   {
      getResources(null, name, urls, true);
   }
   
   /**
    * Get a resource from the domain
    * 
    * @param classLoader the classloader
    * @param name the resource name
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResources(BaseClassLoader classLoader, String name, Set<URL> urls) throws IOException
   {
      getResources(classLoader, name, urls, false);
   }
   
   public Package getPackage(String name)
   {
      return getPackage(null, name, true);
   }
   
   /**
    * Get a package from the specified classloader
    * 
    * @param classLoader the classloader
    * @param name the package name
    * @return the package
    */
   Package getPackage(BaseClassLoader classLoader, String name)
   {
      return getPackage(classLoader, name, false);
   }
   
   public void getPackages(Set<Package> packages)
   {
      getPackages(null, packages, true);
   }
   
   /**
    * Get the packages from a specified classloader 
    * 
    * @param classLoader the classloader
    * @param packages the packages
    */
   void getPackages(BaseClassLoader classLoader, Set<Package> packages)
   {
      getPackages(classLoader, packages, false);
   }

   /**
    * Get the classloader policy associated with an object
    * 
    * @param object the  object
    * @return the classloader policy or null if one is not associated
    */
   protected ClassLoaderPolicy getClassLoaderPolicy(Object object)
   {
      if (object instanceof BaseClassLoader)
         return ((BaseClassLoader) object).getPolicy();
      
      if (object instanceof BaseClassLoaderSource)
         return getClassLoaderPolicy(((BaseClassLoaderSource) object).getClassLoader());
      
      return null;
   }
   
   /**
    * A long version of toString()
    * 
    * @return the long string
    */
   public String toLongString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@").append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{");
      toLongString(builder);
      builder.append('}');
      return builder.toString();
   }
   
   /**
    * For subclasses to add information for toLongString()
    * 
    * @param builder the builder
    */
   protected void toLongString(StringBuilder builder)
   {
   }
   
   /**
    * Invoked before adding a classloader policy 
    * 
    * @param classLoader the classloader
    * @param policy the classloader policy
    */
   protected void beforeRegisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      // nothing
   }

   /**
    * Invoked after adding a classloader policy 
    * 
    * @param classLoader the classloader
    * @param policy the classloader policy
    */
   protected void afterRegisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      // nothing
   }
   
   /**
    * Invoked before adding a classloader policy 
    * 
    * @param classLoader the classloader
    * @param policy the classloader policy
    */
   protected void beforeUnregisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      // nothing
   }
   
   /**
    * Invoked after adding a classloader policy 
    * 
    * @param classLoader the classloader
    * @param policy the classloader policy
    */
   protected void afterUnregisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      // nothing
   }

   /**
    * Get the parent classloader
    * 
    * @return the parent classloader
    */
   protected ClassLoader getParentClassLoader()
   {
      return getClass().getClassLoader();
   }

   /**
    * Register a classloader 
    * 
    * @param classLoader the classloader
    */
   void registerClassLoader(BaseClassLoader classLoader)
   {
      log.debug(this + " registerClassLoader " + classLoader.toString());

      if (getClassLoaderSystem() == null)
         throw new IllegalStateException("Domain is not registered with a classloader system: " + toLongString());
      
      ClassLoaderPolicy policy = classLoader.getPolicy();
      BaseDelegateLoader exported = policy.getExported();
      if (exported != null && exported.getPolicy() == null)
         throw new IllegalStateException("The exported delegate " + exported + " is too lazy for " + policy.toLongString());
      
      try
      {
         beforeRegisterClassLoader(classLoader, policy);
      }
      catch (Throwable t)
      {
         log.warn("Error in beforeRegisterClassLoader: " + this + " classLoader=" + classLoader.toLongString(), t);
      }
      
      BaseClassLoaderPolicy basePolicy = classLoader.getPolicy();
      basePolicy.setClassLoaderDomain(this);

      // FINDBUGS: This synchronization is correct - more than addIfNotPresent behaviour
      synchronized (classLoaders)
      {
         // Create the information
         ClassLoaderInformation info = new ClassLoaderInformation(classLoader, policy, order++);
         classLoaders.add(info);
         basePolicy.setInformation(info);

         // Index the packages
         String[] packageNames = policy.getPackageNames();
         if (packageNames != null && info.getExported() != null)
         {
            for (String packageName : packageNames)
            {
               List<ClassLoaderInformation> list = classLoadersByPackageName.get(packageName);
               if (list == null)
               {
                  list = new CopyOnWriteArrayList<ClassLoaderInformation>();
                  classLoadersByPackageName.put(packageName, list);
               }
               list.add(info);
               log.trace("Registered " + policy + " as providing package=" + packageName);
            }
         }
         
         flushCaches();
      }

      ClassLoaderCache cache = policy.getCache();
      if (cache != null)
         cache.flushCaches();

      try
      {
         afterRegisterClassLoader(classLoader, classLoader.getPolicy());
      }
      catch (Throwable t)
      {
         log.warn("Error in afterRegisterClassLoader: " + this + " classLoader=" + classLoader.toLongString(), t);
      }
   }
   
   /**
    * Remove a classloader 
    * 
    * @param classLoader the classloader
    */
   void unregisterClassLoader(BaseClassLoader classLoader)
   {
      log.debug(this + " unregisterClassLoader " + classLoader.toString());

      try
      {
         beforeUnregisterClassLoader(classLoader, classLoader.getPolicy());
      }
      catch (Throwable t)
      {
         log.warn("Error in beforeUnegisterClassLoader: " + this + " classLoader=" + classLoader.toLongString(), t);
      }

      BaseClassLoaderPolicy policy = classLoader.getPolicy();
      ShutdownPolicy shutdownPolicy = determineShutdownPolicy(policy);

      boolean shutdownNow = (ShutdownPolicy.UNREGISTER == shutdownPolicy);
      if (shutdownNow)
         policy.unsetClassLoaderDomain(this);

      // FINDBUGS: This synchronization is correct - more than addIfNotPresent behaviour
      synchronized (classLoaders)
      {
         // Remove the classloader
         ClassLoaderInformation info = policy.getInformation();
         classLoaders.remove(info);
         if (shutdownNow)
            policy.setInformation(null);
         
         // Remove the package index
         String[] packageNames = policy.getPackageNames();
         if (packageNames != null && info.getExported() != null)
         {
            for (String packageName : packageNames)
            {
               List<ClassLoaderInformation> list = classLoadersByPackageName.get(packageName);
               if (list != null)
               {
                  list.remove(info);
                  log.trace("Unregistered " + policy + " as providing package=" + packageName);
                  if (list.isEmpty())
                     classLoadersByPackageName.remove(packageName);
               }
            }
         }

         flushCaches();
      }

      ClassLoaderCache cache = policy.getCache();
      if (cache != null)
         cache.flushCaches();

      try
      {
         afterUnregisterClassLoader(classLoader, classLoader.getPolicy());
      }
      catch (Throwable t)
      {
         log.warn("Error in afterUnegisterClassLoader: " + this + " classLoader=" + classLoader.toLongString(), t);
      }
   }

   /**
    * Determine the shutdown policy for the classloader policy
    * 
    * @param policy the classloader policy
    * @return the shutdown policy
    */
   ShutdownPolicy determineShutdownPolicy(BaseClassLoaderPolicy policy)
   {
      if (policy == null)
         throw new IllegalArgumentException("Null policy");

      // From the policy
      ShutdownPolicy shutdownPolicy = policy.getShutdownPolicy();
      
      // From the domain (us)
      if (shutdownPolicy == null)
         shutdownPolicy = this.getShutdownPolicy();
      
      // From the clasloader system
      if (shutdownPolicy == null)
      {
         BaseClassLoaderSystem system = getClassLoaderSystem();
         if (system != null)
            shutdownPolicy = system.getShutdownPolicy();
      }
      
      // The default behaviour
      if (shutdownPolicy == null)
         shutdownPolicy = ShutdownPolicy.UNREGISTER;
      
      return shutdownPolicy;
   }
   
   /**
    * Get all the classloaders
    * 
    * @return the list of classloaders
    */
   protected List<ClassLoader> getAllClassLoaders()
   {
      List<ClassLoader> result = new ArrayList<ClassLoader>();
      for (ClassLoaderInformation info : classLoaders)
         result.add(info.getClassLoader());
      return result;
   }

   /**
    * Get a map of packages to classloader
    * 
    * @return a map of packages to a list of classloaders for that package
    */
   protected Map<String, List<ClassLoader>> getClassLoadersByPackage()
   {
      HashMap<String, List<ClassLoader>> result = new HashMap<String, List<ClassLoader>>();
      for (Entry<String, List<ClassLoaderInformation>> entry : classLoadersByPackageName.entrySet())
      {
         List<ClassLoader> cls = new ArrayList<ClassLoader>();
         for (ClassLoaderInformation info : entry.getValue())
            cls.add(info.getClassLoader());
         result.put(entry.getKey(), cls);
      }
      return result;
   }

   protected List<ClassLoader> getClassLoaders(String packageName)
   {
      if (packageName == null)
         throw new IllegalArgumentException("Null package name");
      
      List<ClassLoader> result = new ArrayList<ClassLoader>();
      List<ClassLoaderInformation> infos = classLoadersByPackageName.get(packageName);
      if (infos != null)
      {
         for (ClassLoaderInformation info : infos)
            result.add(info.getClassLoader());
      }
      return result;
   }

   public Class<?> checkClassCache(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      Class<?> result = checkCacheBefore(classLoader, name, path, allExports);
      if (result != null)
         return result;

      result = checkClassCacheLocally(classLoader, name, path, allExports);
      if (result != null)
         return result;

      result = checkCacheAfter(classLoader, name, path, allExports);
      if (result != null)
         return result;

      return null;
   }

   /**
    * Check the class cache
    * 
    * @param classLoader the reference classloader (possibly null)
    * @param name the name of the class
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @return the class if cached
    */
   Class<?> checkClassCacheLocally(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      if (allExports)
      {
         ClassCacheItem item = globalClassCache.get(path);
         if (item != null)
         {
            if (log.isTraceEnabled())
               log.trace("Found " + name + " in global cache: " + this);

            return item.clazz;
         }
      }
      else
      {
         BaseClassLoaderPolicy policy = classLoader.getPolicy();
         if (policy != null)
         {
            ClassLoaderCache cache = policy.getCache();
            if (cache != null)
            {
               Loader loader = cache.getCachedLoader(path);
               return (loader != null) ? loader.loadClass(name) : null;
            }
         }
      }
      return null;
   }

   /**
    * Check the class blacklist
    * 
    * @param classLoader the classloader (possibly null)
    * @param name the name
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @param failIfBlackListed <code>true</code> if a blacklisted class should
    *                          result in ClassNotFoundException; <code>false</code>
    *                          if a <code>null</code> return value is acceptable
    * @throws ClassNotFoundException when the class is blacklisted
    */
   void checkClassBlackList(BaseClassLoader classLoader, String name, String path, boolean allExports, boolean failIfBlackListed) throws ClassNotFoundException
   {
      if (failIfBlackListed)
      {
         if (allExports)
         {
            if (isBlackListedClass(path))
            {
               if (log.isTraceEnabled())
                  log.trace("Found " + name + " in global blacklist");
               throw new ClassNotFoundException(name + " not found - blacklisted");
            }
         }
         else
         {
            BaseClassLoaderPolicy policy = classLoader.getPolicy();
            if (policy != null)
            {
               ClassLoaderCache cache = policy.getCache();
               if (cache != null && cache.isBlackListedClass(path))
               {
                  if (log.isTraceEnabled())
                     log.trace("Found " + name + " in policy cache blacklist: " + cache.getInfo(ImportType.ALL));
                  throw new ClassNotFoundException(name + " not found - blacklisted");
               }
            }
         }
      }
   }
   
   /**
    * Check the cache and blacklist
    * 
    * @param classLoader the classloader (possibly null)
    * @param name the name
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @param failIfBlackListed <code>true</code> if a blacklisted class should
    *                          result in ClassNotFoundException; <code>false</code>
    *                          if a <code>null</code> return value is acceptable
    * @return the class when found in the cache
    * @throws ClassNotFoundException when the class is blacklisted and 
    *                               <code>failIfBlackListed</code> is <code>true</code>
    */
   protected Class<?> checkClassCacheAndBlackList(BaseClassLoader classLoader, String name, String path, boolean allExports, boolean failIfBlackListed) throws ClassNotFoundException
   {
      if (path == null)
         path = ClassLoaderUtils.classNameToPath(name);

      Class<?> cached = checkClassCache(classLoader, name, path, allExports);
      if (cached != null)
         return cached;

      checkClassBlackList(classLoader, name, path, allExports, failIfBlackListed);
      
      return null;
   }

   /**
    * Check the cache before checking this domain.
    * e.g. check parent's domain cache
    *
    * @param classLoader the classloader (possibly null)
    * @param name the name
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @return the class when found in the cache
    */
   protected Class<?> checkCacheBefore(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      return null;
   }

   /**
    * Check the cache after checking before domain.
    * e.g. check parent's domain cache only if this one blacklisted the resource
    *
    * @param classLoader the classloader (possibly null)
    * @param name the name
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @return the class when found in the cache
    */
   protected Class<?> checkCacheAfter(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      return null;
   }

   /**
    * Cleans the entry with the given name from the blackList
    *
    * @param name the name of the resource to clear from the blackList
    */
   protected void clearBlackList(String name)
   {
      if (globalClassBlackList != null)
      {
         globalClassBlackList.remove(name);
      }
      if (globalResourceBlackList != null)
      {
         globalResourceBlackList.remove(name);
      }

      // Need to clear the import caches as well
      List<ClassLoaderInformation> infos = classLoaders;
      for (ClassLoaderInformation info : infos)
         info.clearBlackList(name);
   }

   /**
    * Get the policy's translators.
    *
    * @return the translators
    */
   public synchronized List<Translator> getTranslators()
   {
      if (translators == null || translators.isEmpty())
         return Collections.emptyList();
      else
         return Collections.unmodifiableList(translators);
   }

   /**
    * Set the translators.
    *
    * @param translators the translators
    */
   public synchronized void setTranslators(List<Translator> translators)
   {
      this.translators = translators;
   }

   /**
    * Add the translator.
    *
    * @param translator the translator to add
    * @throws IllegalArgumentException for null translator
    */
   public synchronized void addTranslator(Translator translator)
   {
      if (translator == null)
         throw new IllegalArgumentException("Null translator");

      if (translators == null)
         translators = new ArrayList<Translator>();

      translators.add(translator);
   }

   /**
    * Remove the translator.
    *
    * @param translator the translator to remove
    * @throws IllegalArgumentException for null translator
    */
   public synchronized void removeTranslator(Translator translator)
   {
      if (translator == null)
         throw new IllegalArgumentException("Null translator");

      if (translators != null)
         translators.remove(translator);
   }

   /**
    * ClassCacheItem.
    */
   static class ClassCacheItem
   {
      Loader loader;
      Class<?> clazz;
      
      public ClassCacheItem(Loader loader)
      {
         this.loader = loader;
      }
      
      public ClassCacheItem(Class<?> clazz)
      {
         this.clazz = clazz;
      }

      @Override
      public String toString()
      {
         if (loader != null)
            return loader.toString();
         return "";
      }
   }
}
