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

import javax.management.ObjectName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.*;
import org.jboss.classloader.spi.filter.ClassFilterUtils;
import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.logging.Logger;
import org.jboss.util.collection.ConcurrentSet;
import org.jboss.util.collection.Iterators;

/**
 * BaseClassLoader.
 * 
 * [TODO] Add meaningful javadoc
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class BaseClassLoader extends SecureClassLoader implements BaseClassLoaderMBean, RealClassLoader
{
   /** The log */
   private static final Logger log = Logger.getLogger(BaseClassLoader.class);

   /** The lock object */
   private ReentrantLock lock = new ReentrantLock(true);

   /** The policy for this classloader */
   private ClassLoaderPolicy policy;

   /** Our Loader front end */
   private DelegateLoader loader;

   /** The loaded classes */
   private Set<String> loadedClasses = new ConcurrentSet<String>();

   /** Our resource cache */
   private Map<String, URL> resourceCache;

   /** Our black list */
   private Set<String> blackList;

   /**
    * Create a new ClassLoader with no parent.
    *
    * @param policy the policy
    * @throws IllegalArgumentException for a null policy
    * @throws IllegalStateException if the policy is already associated with a classloader
    */
   public BaseClassLoader(ClassLoaderPolicy policy)
   {
      super(null);
      if (policy == null)
         throw new IllegalArgumentException("Null policy");
      this.policy = policy;

      BaseClassLoaderPolicy basePolicy = policy;
      basePolicy.setClassLoader(this);

      loader = new DelegateLoader(policy);

      if (basePolicy.isCacheable())
         resourceCache = new ConcurrentHashMap<String, URL>();

      if (basePolicy.isBlackListable())
         blackList = new ConcurrentSet<String>();

      log.debugf("Created %1s with policy %2s", this, policy);
   }

   public ObjectName getObjectName()
   {
      return policy.getObjectName();
   }

   public ObjectName getClassLoaderDomain()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      ClassLoaderDomain domain = (ClassLoaderDomain) basePolicy.getClassLoaderDomain();
      return domain.getObjectName();
   }

   public String getName()
   {
      return policy.getName();
   }

   public boolean isBlackListable()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      return basePolicy.isBlackListable();
   }

   public boolean isCacheable()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      return basePolicy.isCacheable();
   }

   public boolean isImportAll()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      return basePolicy.isImportAll();
   }

   public Set<String> listExportedPackages()
   {
      HashSet<String> result = new HashSet<String>();
      String[] packageNames = policy.getPackageNames();
      if (packageNames != null)
         Collections.addAll(result, packageNames);
      return result;
   }

   public List<ObjectName> listImports()
   {
      ArrayList<ObjectName> result = new ArrayList<ObjectName>();
      BaseClassLoaderPolicy basePolicy = policy;
      List<? extends DelegateLoader> delegates = basePolicy.getDelegates();
      if (delegates != null)
      {
         for (DelegateLoader delegate : delegates)
         {
            BaseDelegateLoader baseDelegate = delegate;
            BaseClassLoaderPolicy otherPolicy = baseDelegate.getPolicy();
            if (otherPolicy != null)
               result.add(otherPolicy.getObjectName());
         }
      }
      return result;
   }

   public String listPolicyDetails()
   {
      return policy.toLongString();
   }

   public ObjectName findClassLoaderForClass(String name) throws ClassNotFoundException
   {
      final Class<?> clazz = loadClass(name);
      if (clazz == null)
         return null;

      ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return clazz.getClassLoader();
         }
      });

      if (cl != null && cl instanceof RealClassLoader)
         return ((RealClassLoader) cl).getObjectName();

      return null;
   }

   public Set<String> listLoadedClasses()
   {
      return Collections.unmodifiableSet(loadedClasses);
   }

   public Set<String> listLoadedResourceNames()
   {
      if (resourceCache == null)
         return Collections.emptySet();
      return new HashSet<String>(resourceCache.keySet());
   }

   // FindBugs: The Set doesn't use equals/hashCode
   public Set<URL> listLoadedResources()
   {
      if (resourceCache == null)
         return Collections.emptySet();
      Set<URL> result = new TreeSet<URL>(ClassLoaderUtils.URLComparator.INSTANCE);
      result.addAll(resourceCache.values());
      return result;
   }

   public ShutdownPolicy getShutdownPolicy()
   {
      BaseClassLoaderPolicy basePolicy = getPolicy();
      return basePolicy.determineShutdownPolicy();
   }

   /**
    * Get the policy.
    *
    * @return the policy.
    */
   ClassLoaderPolicy getPolicy()
   {
      return policy;
   }

   /**
    * Get the loader.
    *
    * @return the loader.
    */
   DelegateLoader getLoader()
   {
      return loader;
   }

   @Override
   protected Package getPackage(String name)
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " getPackage " + name);

      if (name == null)
         return null;

      // Did we already load this package?
      Package result = super.getPackage(name);
      if (result != null && trace)
         log.trace(this + " already loaded package " + name + " " + result.getName());

      // Not already loaded use the domain
      if (result == null)
      {
         BaseClassLoaderPolicy basePolicy = policy;
         BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
         if (domain == null)
            return null;
         if (trace)
            log.trace(this + " getPackage " + name + " domain=" + domain);
         result = domain.getPackage(this, name);
      }

      // Still not found
      if (result == null)
      {
         if (trace)
            log.trace(this + " package not found " + name);
      }

      return result;
   }

   @Override
   protected Package[] getPackages()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
      if (domain == null)
         return super.getPackages();
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " getPackages domain=" + domain);

      Set<Package> packages = new HashSet<Package>();
      domain.getPackages(this, packages);
      return packages.toArray(new Package[packages.size()]);
   }

   /**
    * Get a package locally
    *
    * @param name the package name
    * @return the package
    */
   Package getPackageLocally(String name)
   {
      return super.getPackage(name);
   }

   /**
    * Get the packages locally
    *
    * @param packages the packages
    */
   void getPackagesLocally(Set<Package> packages)
   {
      Package[] pkgs = super.getPackages();
      for (Package pkg : pkgs)
         packages.add(pkg);
   }

   /**
    * Check to see if the class is already loaded
    *
    * @param name the name of the class
    * @param trace whether trace is enabled
    * @return the class is if it is already loaded, null otherwise
    */
   protected Class<?> isLoadedClass(String name, boolean trace)
   {
      Class<?> result = findLoadedClass(name);
      if (result != null)
      {
         // Has this classloader been undeployed?
         ClassLoader otherClassLoader = getClassLoader(result);
         if (otherClassLoader != null && otherClassLoader != this && otherClassLoader instanceof RealClassLoader)
         {
            RealClassLoader rcl = (RealClassLoader) otherClassLoader;
            // Ignore when undeployed
            if (rcl.isValid() == false)
            {
               if (trace)
                  log.trace(this + " ignoring already loaded class from undeployed classloader " + ClassLoaderUtils.classToString(result));
               result = null;
            }
         }
      }
      if (result != null && trace)
         log.trace(this + " already loaded class " + ClassLoaderUtils.classToString(result));
      return result;
   }

   /**
    * Check the cache and blacklist
    *
    * @param name the name of the class
    * @param failIfBlackListed <code>true</code> if a blacklisted class should
    *                          result in ClassNotFoundException; <code>false</code>
    *                          if a <code>null</code> return value is acceptable
    * @param trace whether trace is enabled
    * @return the class is if it is already loaded, null otherwise
    * @throws ClassNotFoundException when the class is blacklisted and 
    *                               <code>failIfBlackListed</code> is <code>true</code>
    */
   protected Class<?> checkCacheAndBlackList(String name, boolean failIfBlackListed, boolean trace) throws ClassNotFoundException
   {
      BaseClassLoaderPolicy basePolicy = policy;
      BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
      if (domain == null)
         return null;

      return domain.checkClassCacheAndBlackList(this, name, null, basePolicy.isImportAll(), failIfBlackListed);
   }

   @Override
   protected String findLibrary(String libname)
   {
      String libraryPath = null;

      if (policy != null)
         libraryPath = policy.findLibrary(libname);

      if (libraryPath == null)
         libraryPath = super.findLibrary(libname);

      return libraryPath;
   }
   
   /**
    * Find the classloader for a class but don't load the class
    *
    * @param className the class name
    * @return the classloader
    * @throws ClassNotFoundException if the class is not found
    * @throws IllegalStateException if the classloader is not installed
    * @throws SecurityException if the called doesn't have the getClassLoader runtime permission
    */
   public ClassLoader findClassLoader(String className) throws ClassNotFoundException
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkPermission(new RuntimePermission("getClassLoader"));

      BaseClassLoaderPolicy basePolicy = policy;
      BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
      if (domain == null)
         throw new IllegalStateException(this + " classLoader is not connected to a domain (probably undeployed?) for class " + getName());

      ClassLoaderUtils.checkClassName(className);
      String path = ClassLoaderUtils.classNameToPath(className);

      Loader loader = domain.findLoader(this, path, basePolicy.isImportAll(), true);
      if (loader == null)
         throw new ClassNotFoundException("Class " + className + " not found from " + this);

      // This is a bit ugly but we can't abstract this behind an interface because
      // that would make the methods public
      if (loader instanceof BaseClassLoaderSource)
         return ((BaseClassLoaderSource) loader).getClassLoader();
      if (loader instanceof BaseDelegateLoader)
         return ((BaseDelegateLoader) loader).getPolicy().getClassLoader();
      return null;
   }

   @Override
   protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " loadClass " + name + " resolve=" + resolve);

      // Validate the class name makes sense
      ClassLoaderUtils.checkClassName(name);

      // Did we already load this class?
      Class<?> result = isLoadedClass(name, trace);

      // If this is an array, use Class.forName() to resolve it
      if (result == null && name.charAt(0) == '[')
      {
         if (trace)
            log.trace(this + " resolving array class " + name + " using Class.forName()");
         result = Class.forName(name, true, this);
         if (trace)
            log.trace(this + " resolved array "  + ClassLoaderUtils.classToString(result));
      }
      if (result != null)
         return result;

      result = checkCacheAndBlackList(name, false, trace);
      if (result != null)
         return result;

      // Try to load the class
      ClassNotFoundException exception = null;
      try
      {
         result = doLoadClass(name, resolve, trace);
      }
      catch (ClassNotFoundException e)
      {
         exception = e;
      }
      
      // If we failed, try any class not found handlers and retry if one says it is resolved
      if (result == null)
      {
         ClassLoaderPolicy policy = getPolicy();
         if (policy != null && policy.classNotFound(new ClassNotFoundEvent(this, name)))
         {
            try
            {
               result = doLoadClass(name, resolve, trace);
            }
            catch (ClassNotFoundException e)
            {
               exception = e;
            }
         }
      }
      
      if (result == null)
      {
         if (trace)
            log.trace(this + " class not found " + name);
         if (exception != null)
            throw exception;
         throw new ClassNotFoundException(name + " from " + toString());
      }
      return result;
   }

   /**
    * Do the load class
    * 
    * @param name the name
    * @param resolve whether to resolve
    * @param trace whether trace is enabled
    * @return the class or null if not found
    * @throws ClassNotFoundException if a problem is raised
    */
   protected Class<?> doLoadClass(String name, boolean resolve, boolean trace) throws ClassNotFoundException
   {
      Class<?> result;
      
      synchronized (this)
      {
         // JBCL-114: did we lose the race to the synchronized?
         result = isLoadedClass(name, trace);
         
         // Not already loaded use the domain
         if (result == null)
            result = loadClassFromDomain(name, trace);

         // Still not found
         if (result == null)
            return null;

         // Link the class if requested
         if (resolve)
         {
            if (trace)
               log.trace(this + " resolveClass " + ClassLoaderUtils.classToString(result));
            resolveClass(result);
         }

         return result;
      }
   }

   @Override
   public URL getResource(String name)
   {
      BaseClassLoaderPolicy basePolicy = policy;
      BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " getResource " + name + " domain=" + domain);

      if (domain != null)
         return domain.getResource(this, name);
      URL url = getResourceLocally(name, trace);
      if (url != null)
         return url;
      if (ClassFilterUtils.JAVA_ONLY.matchesResourcePath(name))
         return getSystemResource(name);
      return null;
   }

   @Override
   @SuppressWarnings("unchecked")
   protected Enumeration<URL> findResources(String name) throws IOException
   {
      Set<URL> resourceURLs = loadResources(name);
      return Iterators.toEnumeration(resourceURLs.iterator());
   }

   // FindBugs: The Set doesn't use equals/hashCode
   public Set<URL> loadResources(String name) throws IOException
   {
      BaseClassLoaderPolicy basePolicy = policy;
      BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();
      boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace(this + " findResources " + name + " domain=" + domain);

      Set<URL> resourceURLs = new TreeSet<URL>(ClassLoaderUtils.URLComparator.INSTANCE);
      if (domain != null)
      {
         domain.getResources(this, name, resourceURLs);
      }
      else
      {
         if (ClassFilterUtils.JAVA_ONLY.matchesResourcePath(name))
         {
            Enumeration<URL> urls = getSystemResources(name);
            while (urls.hasMoreElements())
               resourceURLs.add(urls.nextElement());
         }
         getResourcesLocally(name, resourceURLs, trace);
      }
      return resourceURLs;
   }

   /**
    * Try to load the class locally
    *
    * @param name the class name
    * @return the class
    */
   Class<?> loadClassLocally(String name)
   {
      return loadClassLocally(name, log.isTraceEnabled());
   }

   /**
    * Try to load the class locally
    *
    * @param name the class name
    * @param trace whether trace is enabled
    * @return the class if found
    */
   synchronized Class<?> loadClassLocally(final String name, final boolean trace)
   {
      if (trace)
         log.trace(this + " load class locally " + name);

      // This is really a double check but the request may not have entered through loadClass on this classloader
      Class<?> result = isLoadedClass(name, trace);
      if (result != null)
         return result;

      // Look for the resource
      final String resourcePath = ClassLoaderUtils.classNameToPath(name);

      result = AccessController.doPrivileged(new PrivilegedAction<Class<?>>()
      {
         public Class<?> run()
         {
            InputStream is = policy.getResourceAsStream(resourcePath);
            if (is == null)
            {
               if (trace)
                  BaseClassLoader.log.trace(BaseClassLoader.this + " resource not found locally " + resourcePath + " for " + name);
               return null;
            }

            // Load the bytecode
            byte[] byteCode = ClassLoaderUtils.loadByteCode(name, is);

            // Let the policy do things before we define the class
            BaseClassLoaderPolicy basePolicy = policy;
            ProtectionDomain protectionDomain = basePolicy.getProtectionDomain(name, resourcePath);
            try
            {
               byte[] transformed = policy.transform(name, byteCode, protectionDomain);
               if (transformed != null)
                  byteCode = transformed;
            }
            catch (Throwable t)
            {
               throw new RuntimeException("Unexpected error transforming class " + name, t);
            }

            // Create the package if necessary
            URL codeSourceURL = null;
            if (protectionDomain != null)
            {
               CodeSource codeSource = protectionDomain.getCodeSource();
               if (codeSource != null)
                  codeSourceURL = codeSource.getLocation();
            }
            definePackage(name, codeSourceURL);

            // Finally we can define the class
            Class<?> result;
            if (protectionDomain != null)
               result = defineClass(name, byteCode, 0, byteCode.length, protectionDomain);
            else
               result = defineClass(name, byteCode, 0, byteCode.length);
            if (trace)
               BaseClassLoader.log.trace(BaseClassLoader.this + " loaded class locally " + ClassLoaderUtils.classToString(result));
            return result;
         }
      }, policy.getAccessControlContext());

      if (result != null)
      {
         loadedClasses.add(name);
         policy.classFound(new ClassFoundEvent(this, result));
      }

      return result;
   }

   /**
    * Try to find the resource locally
    *
    * @param name the resource name
    * @return the url if found
    */
   public URL getResourceLocally(String name)
   {
      return getResourceLocally(name, log.isTraceEnabled());
   }

   /**
    * Try to find the resource locally
    *
    * @param name the resource name
    * @param trace whether trace is enabled
    * @return the URL if found
    */
   URL getResourceLocally(final String name, final boolean trace)
   {
      if (trace)
         log.trace(this + " get resource locally " + name);

      // Do we already know the answer?
      if (resourceCache != null)
      {
         URL url = resourceCache.get(name);
         if (url != null)
         {
            if (trace)
               log.trace(this + " got resource from cache " + name);
            return url;
         }
      }

      // Is this resource blacklisted?
      if (blackList != null && blackList.contains(name))
      {
         if (trace)
            log.trace(this + " resource is blacklisted " + name);
         return null;
      }

      // Ask the policy for the resource
      URL result = AccessController.doPrivileged(new PrivilegedAction<URL>()
      {
         public URL run()
         {
            // Look for the resource
            URL result = policy.getResource(name);
            if (result == null)
            {
               if (trace)
                  BaseClassLoader.log.trace(BaseClassLoader.this + " resource not found locally " + name);
               return null;
            }
            if (trace)
               BaseClassLoader.log.trace(BaseClassLoader.this + " got resource locally " + name);
            return result;
         }
      }, policy.getAccessControlContext());

      // Cache what we found
      if (resourceCache != null && result != null)
         resourceCache.put(name, result);

      // Blacklist when not found
      if (blackList != null && result == null)
         blackList.add(name);

      return result;
   }

   /**
    * Try to find the resource locally
    *
    * @param name the resource name
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResourcesLocally(String name, Set<URL> urls) throws IOException
   {
      getResourcesLocally(name, urls, log.isTraceEnabled());
   }

   /**
    * Try to find the resources locally
    *
    * @param name the resource name
    * @param urls the urls to add to
    * @param trace whether trace is enabled
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   void getResourcesLocally(final String name, final Set<URL> urls, boolean trace) throws IOException
   {
      if (trace)
         log.trace(this + " get resources locally " + name);

      // Look for the resources
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()
         {
            public Object run() throws Exception
            {
               policy.getResources(name, urls);
               return null;
            }
         }, policy.getAccessControlContext());
      }
      catch (PrivilegedActionException e)
      {
         Exception e1 = e.getException();
         if (e1 instanceof RuntimeException)
            throw (RuntimeException) e1;
         if (e1 instanceof IOException)
            throw (IOException) e1;
         IOException e2 = new IOException("Unexpected error");
         e2.initCause(e1);
         throw e2;
      }
   }

   /**
    * Define the package for the class if not already done
    *
    * @param className the class name
    * @param codeSourceURL the code source url
    */
   protected void definePackage(String className, URL codeSourceURL)
   {
      String packageName = ClassLoaderUtils.getClassPackageName(className);
      if (packageName.length() == 0)
         return;

      // Ask the policy for the information
      PackageInformation pi = policy.getClassPackageInformation(className, packageName);

      // Already defined?
      Package pkge = getPackage(packageName);
      if (pkge != null)
      {
         // Check sealing
         if (pkge.isSealed())
         {
            // Are we trying to add outside the seal?
            if (codeSourceURL == null || pkge.isSealed(codeSourceURL) == false)
               throw new SecurityException("Sealing violation for package " + packageName);
         }
         // Can we seal now?
         else if (pi != null && pi.sealBase != null)
         {
            throw new SecurityException("Can't seal package " + packageName +" it is already loaded");
         }
         return;
      }

      try
      {
         if (pi != null)
            definePackage(packageName, pi.specTitle, pi.specVersion, pi.specVendor, pi.implTitle, pi.implVersion, pi.implVendor, pi.sealBase);
         else
            definePackage(packageName, null, null, null, null, null, null, null);
         if (log.isTraceEnabled())
            log.trace(this + " defined package: " + packageName);
      }
      catch (IllegalArgumentException alreadyDone)
      {
         // The package has already been defined
      }
   }

   /**
    * Try to load the class from the domain
    *
    * @param name the class name
    * @param trace whether trace is enabled
    * @return the class if found in the parent
    * @throws ClassNotFoundException for any error
    */
   protected Class<?> loadClassFromDomain(String name, boolean trace) throws ClassNotFoundException
   {
      // Because of the broken classloading in the Sun JDK we need to
      // serialize access to the classloader.

      // Additionally, acquiring the lock on the policy for this classloader
      // ensures that we don't race with somebody undeploying the classloader
      // which could cause leaks
      acquireLockFairly(trace);
      try
      {
         // Here we have synchronized with the policy
         BaseClassLoaderPolicy basePolicy = policy;
         BaseClassLoaderDomain domain = basePolicy.getClassLoaderDomain();

         if (trace)
            log.trace(this + " load from domain " + name + " domain=" + domain);

         // No domain, try to load the class locally
         // this could happen during undeployment of the classloader
         // where something needs a local class after it is has been unhooked from the system
         if (domain == null)
         {
            Class<?> result = loadClassLocally(name, trace);

            // Try the classpath for java classes or requests from the classpath
            if (result == null && (ClassFilterUtils.JAVA_ONLY.matchesClassName(name) || basePolicy.isJDKRequest(name) != null))
               result = getSystemClassLoader().loadClass(name);
            
            // So this is almost certainly a classloader leak
            if (result == null)
               throw new IllegalStateException(this + " classLoader is not connected to a domain (probably undeployed?) for class " + name);
            return result;
         }

         // Ask the domain to load the class in the context of our classloader/policy
         Class<?> result = domain.loadClass(this, name);
         if (result != null && trace)
            log.trace(this + " got class from domain " + ClassLoaderUtils.classToString(result));
         return result;
      }
      catch (ClassNotFoundException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new ClassNotFoundException("Class not found " + name, e);
      }
      finally
      {
         unlock(trace, true);
      }
   }

   public boolean isValid()
   {
      BaseClassLoaderPolicy basePolicy = policy;
      return basePolicy.getClassLoaderUnchecked() != null;
   }

   public Class<?> getCachedClass(String name)
   {
      try
      {
         return checkCacheAndBlackList(name, false, log.isTraceEnabled());
      }
      catch (ClassNotFoundException ignored)
      {
         return null;
      }
   }

   public URL getCachedResource(String name)
   {
      // TODO look in global and/or local cache
      return null;
   }

   public int getResourceBlackListSize()
   {
      Set<String> blackList = this.blackList;
      if (blackList == null)
         return 0;
      return blackList.size();
   }

   public int getResourceCacheSize()
   {
      Map<String, URL> resourceCache = this.resourceCache;
      if (resourceCache == null)
         return 0;
      return resourceCache.size();
   }

   public Set<String> listResourceBlackList()
   {
      Set<String> blackList = this.blackList;
      if (blackList == null)
         return Collections.emptySet();
      return Collections.unmodifiableSet(blackList);
   }

   public Map<String, URL> listResourceCache()
   {
      Map<String, URL> resourceCache = this.resourceCache;
      if (resourceCache == null)
         return Collections.emptyMap();
      return Collections.unmodifiableMap(resourceCache);
   }

   public void clearBlackList()
   {
      if (blackList != null)
      {
         for (String name : blackList)
            clearBlackList(name);
      }
   }

   public void clearBlackList(String name)
   {
      if (blackList != null)
      {
         boolean trace = log.isTraceEnabled();
         if (trace)
            log.trace(this + " removing from blacklist " + name);
         blackList.remove(name);
         policy.clearBlackList(name);
      }
   }

   /**
    * A long version of the classloader
    *
    * @return the long string
    */
   public String toLongString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append('@').append(Integer.toHexString(System.identityHashCode(this)));
      builder.append('{').append(getPolicy().toLongString());
      toLongString(builder);
      builder.append('}');
      return builder.toString();
   }

   /**
    * Shutdown the classloader
    */
   protected void shutdownClassLoader()
   {
      log.debug(toString() + " shutdown!");
      loadedClasses.clear();
      if (resourceCache != null)
         resourceCache.clear();
      if (blackList != null)
         blackList.clear();
   }

   /**
    * For subclasses to add things to the long string
    *
    * @param builder the builder
    */
   protected void toLongString(StringBuilder builder)
   {
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append('@').append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{").append(policy.getName()).append("}");
      return builder.toString();
   }

   /**
    * Attempt to lock, but don't wait
    *
    * @return true when the lock was obtained
    */
   boolean attemptLock()
   {
      return attemptLock(log.isTraceEnabled());
   }

   /**
    * Lock
    *
    * This method must be invoked with the monitor held
    */
   void lock()
   {
      acquireLockFairly(log.isTraceEnabled());
   }

   /**
    * Unlock
    *
    * @param rescheduleTasks whether to reschedule tasks
    */
   void unlock(boolean rescheduleTasks)
   {
      unlock(log.isTraceEnabled(), rescheduleTasks);
   }

   /**
    * Attempt to get the lock but don't wait
    *
    * @param trace whether trace is enabled
    * @return true when obtained the lock
    */
   private boolean attemptLock(boolean trace)
   {
      Thread thread = Thread.currentThread();
      if (trace)
         log.trace(this + " attemptLock " + thread);

      boolean interrupted = Thread.interrupted();

      boolean result = false;
      try
      {
         result = lock.tryLock(0, TimeUnit.MICROSECONDS);
      }
      catch (InterruptedException ignored)
      {
         interrupted = true;
      }
      finally
      {
         if (interrupted)
            thread.interrupt();
      }
      if (trace)
      {
         if (result)
            log.trace(this + " locked " + thread + " holding=" + lock.getHoldCount());
         else
            log.trace(this + " did NOT get the lock " + thread);
      }

      // We got the lock so we own it
      if (result && lock.getHoldCount() == 1)
         ClassLoaderManager.registerLoaderThread(this, thread);

      return result;
   }

   /**
    * Acquire the lock on the classloader fairly<p>
    *
    * This must be invoked with the monitor held
    *
    * @param trace whether trace is enabled
    */
   private void acquireLockFairly(boolean trace)
   {
      Thread thread = Thread.currentThread();
      if (trace)
         log.trace(this + " aquireLockFairly " + thread);

      boolean interrupted = Thread.interrupted();

      int waits = 0;

      try
      {
         while (true)
         {
            try
            {
               if (lock.tryLock(0, TimeUnit.MICROSECONDS) == false)
               {
                  // Two minutes should be long enough?
                  if (waits++ == 12)
                     throw new IllegalStateException("Waiting too long to get the classloader lock: " + this);
                  // Wait 10 seconds
                  if (trace)
                     log.trace(this + " waiting for lock " + thread);
                  this.wait(10000);
               }
               else
               {
                  if (trace)
                     log.trace(this + " aquiredLock " + thread + " holding=" + lock.getHoldCount());
                  break;
               }
            }
            catch (InterruptedException ignored)
            {
               interrupted = true;
            }
         }
      }
      finally
      {
         if (interrupted)
            thread.interrupt();
      }

      if (lock.getHoldCount() == 1)
         ClassLoaderManager.registerLoaderThread(this, thread);
   }

   /**
    * Unlock
    *
    * @param trace whether trace is enabled
    * @param rescheduleTasks whether to reschedule tasks
    */
   private void unlock(boolean trace, boolean rescheduleTasks)
   {
      Thread thread = Thread.currentThread();
      if (trace)
         log.trace(this + " unlock " + thread + " holding=" + lock.getHoldCount());

      synchronized (this)
      {
         lock.unlock();

         if (lock.getHoldCount() == 0)
         {
            ClassLoaderManager.unregisterLoaderThread(this, thread, rescheduleTasks);
            notifyAll();
         }
      }
   }
   
   /**
    * Get the classloader for a class
    *
    * @param clazz the class
    * @return the classloader or null if it doesn't have one
    */
   private static final ClassLoader getClassLoader(final Class<?> clazz)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return clazz.getClassLoader();

      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return clazz.getClassLoader();
         }
      });
   }
}
