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
package org.jboss.classloader.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.classloader.spi.base.BaseClassLoaderPolicy;
import org.jboss.classloader.spi.filter.FilteredDelegateLoader;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.classloader.spi.jdk.JDKChecker;
import org.jboss.classloader.spi.jdk.JDKCheckerFactory;
import org.jboss.logging.Logger;

/**
 * ClassLoader policy.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class ClassLoaderPolicy extends BaseClassLoaderPolicy implements ClassNotFoundHandler, ClassFoundHandler
{
   /** The log */
   private static final Logger log = Logger.getLogger(ClassLoaderPolicy.class);
   
   /** The shutdown policy */
   private ShutdownPolicy shutdownPolicy;

   /** The class not found handlers */
   private List<ClassNotFoundHandler> classNotFoundHandlers;

   /** The class found handlers */
   private List<ClassFoundHandler> classFoundHandlers;

   /** Maps native library to its provider */
   private volatile Map<String, NativeLibraryProvider> libraryMap;
   
   /**
    * Get the set of registered native library names.
    * 
    * @return Null if there are no native libraries registered.
    */
   public Set<String> getNativeLibraryNames()
   {
      Map<String, NativeLibraryProvider> map = libraryMap;

      if (map == null)
         return Collections.emptySet();
      
      return Collections.unmodifiableSet(libraryMap.keySet()); 
   }
   
   /**
    * Get the native library provider for the given name.
    * 
    * @param libname The library name 
    * @return Null if there is no library with that name.
    */
   public NativeLibraryProvider getNativeLibrary(String libname)
   {
      Map<String, NativeLibraryProvider> map = libraryMap;
      
      return (map != null ? map.get(libname) : null);
   }
   
   /**
    * Add a native library provider.
    * @param libname The library name 
    * @param provider The library file provider
    */
   public void addNativeLibrary(String libname, NativeLibraryProvider provider)
   {
      if (libraryMap == null)
         libraryMap = new ConcurrentHashMap<String, NativeLibraryProvider>();
      
      libraryMap.put(libname, provider);
   }
   
   /**
    * Remove the native library provider for the given name.
    * 
    * @param libname The library name 
    * @return Null if there is no library with that name.
    */
   public NativeLibraryProvider removeNativeLibrary(String libname)
   {
      return (libraryMap != null ? libraryMap.remove(libname) : null);
   }
   
   /**
    * Returns the absolute path name of a native library.
    * 
    * @param libname The library name 
    * @return The absolute path of the native library, or null
    */
   public String findLibrary(String libname)
   {
      Map<String, NativeLibraryProvider> map = libraryMap;
      if (map == null)
         return null;
      
      NativeLibraryProvider libProvider = map.get(libname);
      
      // [TODO] why does the TCK use 'Native' to mean 'libNative' ? 
      if (libProvider == null)
         libProvider = map.get("lib" + libname);
         
      if (libProvider == null)
         return null;
      
      File libfile;
      try
      {
         libfile = libProvider.getLibraryLocation();
      }
      catch (IOException ex)
      {
         log.error("Cannot privide native library location for: " + libname, ex);
         return null;
      }
      
      return libfile.getAbsolutePath();
   }
   
   /**
    * Get the delegate loader for exported stuff<p>
    *
    * By default this uses {@link #getPackageNames()} to create a {@link FilteredDelegateLoader}
    * 
    * @return the delegate loader
    */
   public DelegateLoader getExported()
   {
      String[] packageNames = getPackageNames();
      if (packageNames == null)
         return null;
      return new FilteredDelegateLoader(this, PackageClassFilter.createPackageClassFilter(packageNames));
   }

   /**
    * Get the exported packages<p>
    *
    * Provides a hint for indexing<p>
    * 
    * No packages are exported by default<p>
    * 
    * The returned package names can be null to indicate
    * nothing is exported, but if an array is returned
    * it should not include a null package element.
    * 
    * @return the package names
    */
   public String[] getPackageNames()
   {
      return null;
   }
   
   /**
    * Get the delegate loaders for imported stuff<p>
    * 
    * There are no imports by default<p>
    * 
    * NOTE: Protected access for security reasons
    * 
    * @return the delegate loaders
    */
   protected List<? extends DelegateLoader> getDelegates()
   {
      return Collections.emptyList();
   }

   /**
    * Whether to import all exports from other classloaders in the domain<p>
    *
    * False by default
    * 
    * @return true to import all
    */
   protected boolean isImportAll()
   {
      return false;
   }

   /**
    * Whether to cache<p>
    * 
    * True by default
    * 
    * @return true to cache
    */
   protected boolean isCacheable()
   {
      return true;
   }

   /**
    * Whether to cache misses<p>
    * 
    * True by default
    * 
    * @return true to cache misses
    */
   protected boolean isBlackListable()
   {
      return true;
   }
   
   /**
    * Get the resource
    * 
    * @param path the path
    * @return the url or null if not found
    */
   // FindBugs: The Set doesn't use equals/hashCode
   public abstract URL getResource(String path);

   /**
    * Get the resource as a stream<p>
    * 
    * Uses {@link #getResource(String)} by default
    * 
    * @param path the path
    * @return the stream or null if not found
    */
   public InputStream getResourceAsStream(String path)
   {
      URL url = getResource(path);
      if (url != null)
      {
         try
         {
            return url.openStream();
         }
         catch (IOException e)
         {
            log.debug("Unable to open URL: " + url + " for path " + path + " from " + toLongString());
         }
      }
      return null;
   }

   /**
    * Get resources
    * 
    * @param name the resource name
    * @param urls the list of urls to add to
    * @throws IOException for any error
    */
   public abstract void getResources(String name, Set<URL> urls) throws IOException;

   /**
    * Get the shutdownPolicy.
    * 
    * @return the shutdownPolicy.
    */
   public ShutdownPolicy getShutdownPolicy()
   {
      return shutdownPolicy;
   }

   /**
    * Set the shutdownPolicy.
    * 
    * @param shutdownPolicy the shutdownPolicy.
    */
   public void setShutdownPolicy(ShutdownPolicy shutdownPolicy)
   {
      this.shutdownPolicy = shutdownPolicy;
   }

   /**
    * Get the protection domain<p>
    * 
    * By default there is no protection domain<p>
    * 
    * NOTE: Protected access for security reasons
    * 
    * @param className the class name
    * @param path the path
    * @return the protection domain
    */
   protected ProtectionDomain getProtectionDomain(String className, String path)
   {
      return null;
   }
   
   /**
    * Get the package information<p>
    * 
    * There is no package information by default
    * 
    * @param packageName the package information
    * @return the information or null if there is none
    */
   public PackageInformation getPackageInformation(String packageName)
   {
      return null;
   }
   
   /**
    * Get the package information for a class<p>
    * 
    * The default is to invoke getPackageInformation for the class's package
    * 
    * @param className name the class name
    * @param packageName the package information
    * @return the information or null if there is none
    */
   public PackageInformation getClassPackageInformation(String className, String packageName)
   {
      return getPackageInformation(packageName);
   }

   /**
    * Check whether this a request from the jdk if it is return the relevant classloader<p>
    * 
    * By default this uses the {@link JDKCheckerFactory} and returns the system classloader if true.
    * 
    * @param name the class name
    * @return the classloader
    */
   protected ClassLoader isJDKRequest(String name)
   {
      JDKChecker checker = JDKCheckerFactory.getChecker();
      if (checker.isJDKRequest(name))
         return getSystemClassLoader();
      return null;
   }
   
   /**
    * Add a ClassNotFoundHandler
    * 
    * @param handler the handler
    */
   public void addClassNotFoundHandler(ClassNotFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classNotFoundHandlers == null)
         classNotFoundHandlers = new CopyOnWriteArrayList<ClassNotFoundHandler>();
      
      classNotFoundHandlers.add(handler);
   }
   
   /**
    * Remove a ClassNotFoundHandler
    * 
    * @param handler the handler
    */
   public void removeClassNotFoundHandler(ClassNotFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classNotFoundHandlers == null)
         return;
      classNotFoundHandlers.remove(handler);
   }

   public boolean classNotFound(ClassNotFoundEvent event)
   {
      if (classNotFoundHandlers != null && classNotFoundHandlers.isEmpty() == false)
      {
         for (ClassNotFoundHandler handler : classNotFoundHandlers)
         {
            try
            {
               if (handler.classNotFound(event))
                  return true;
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classNotFoundHandler: " + handler, t);
            }
         }
      }
      
      ClassLoaderDomain domain = getDomain();
      return domain != null && domain.classNotFound(event);
   }
   
   /**
    * Add a ClassFoundHandler
    * 
    * @param handler the handler
    */
   public void addClassFoundHandler(ClassFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classFoundHandlers == null)
         classFoundHandlers = new CopyOnWriteArrayList<ClassFoundHandler>();
      
      classFoundHandlers.add(handler);
   }
   
   /**
    * Remove a ClassFoundHandler
    * 
    * @param handler the handler
    */
   public void removeClassFoundHandler(ClassFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classFoundHandlers == null)
         return;
      classFoundHandlers.remove(handler);
   }

   public void classFound(ClassFoundEvent event)
   {
      if (classFoundHandlers != null && classFoundHandlers.isEmpty() == false)
      {
         for (ClassFoundHandler handler : classFoundHandlers)
         {
            try
            {
               handler.classFound(event);
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classFoundHandler: " + handler, t);
            }
         }
      }
      
      ClassLoaderDomain domain = getDomain();
      if (domain != null)
         domain.classFound(event);
   }

   @Override
   public ObjectName getObjectName()
   {
      try
      {
         String name = getName();
         if (name != null && name.trim().length() > 0)
            return ObjectName.getInstance("jboss.classloader", "id", "\"" + name + "\"");
         return ObjectName.getInstance("jboss.classloader", "id", "" + System.identityHashCode(this));
      }
      catch (MalformedObjectNameException e)
      {
         throw new Error("Error creating object name", e);
      }
   }
   
   @Override
   protected void toLongString(StringBuilder builder)
   {
      builder.append(" delegates=").append(getDelegates());
      String[] packageNames = getPackageNames();
      if (packageNames != null)
         builder.append(" exported=").append(Arrays.asList(packageNames));
      boolean importAll = isImportAll();
      if (importAll)
         builder.append(" <IMPORT-ALL>");
   }

   /**
    * Get the system classloader
    * 
    * @return the classloader
    */
   private ClassLoader getSystemClassLoader()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return ClassLoader.getSystemClassLoader();
      
      return AccessController.doPrivileged(GetSystemClassLoader.INSTANCE, getAccessControlContext());
   }
   
   /**
    * GetSystemClassLoader.
    */
   private static class GetSystemClassLoader implements PrivilegedAction<ClassLoader>
   {
      private static GetSystemClassLoader INSTANCE = new GetSystemClassLoader();
      
      public ClassLoader run()
      {
         return ClassLoader.getSystemClassLoader();
      }
   }
}
