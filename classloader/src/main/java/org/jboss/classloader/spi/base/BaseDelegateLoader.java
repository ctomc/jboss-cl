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
import java.util.Set;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.CacheLoader;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderPolicyFactory;
import org.jboss.logging.Logger;

/**
 * Base DelegateLoader.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class BaseDelegateLoader implements CacheLoader
{
   /** The log */
   private static final Logger log = Logger.getLogger(BaseDelegateLoader.class);

   /** The class to path */
   private static final ResourcePathAdapter CLASS_TO_PATH = new ClassToPathResourceAdapter();

   /** The package to path */
   private static final ResourcePathAdapter PACKAGE_TO_PATH = new PackageToPathResourceAdapter();

   /** The delegate loader policy */
   private volatile BaseClassLoaderPolicy delegate;

   /** The policy factory */
   private ClassLoaderPolicyFactory factory;
   
   /**
    * Create a new BaseDelegateLoader.
    * 
    * @param delegate the delegate
    * @throws IllegalArgumentException for a null delegate
    */
   public BaseDelegateLoader(BaseClassLoaderPolicy delegate)
   {
      if (delegate == null)
         throw new IllegalArgumentException("Null delegate");
      this.delegate = delegate;
   }
   
   /**
    * Create a new BaseDelegateLoader.
    * 
    * @param factory the factory
    * @throws IllegalArgumentException for a null factory
    */
   public BaseDelegateLoader(ClassLoaderPolicyFactory factory)
   {
      if (factory == null)
         throw new IllegalArgumentException("Null factory");
      this.factory = factory;
   }
   
   protected BaseClassLoaderPolicy getPolicy()
   {
      BaseClassLoaderPolicy delegate = this.delegate;
      if (delegate == null)
      {
         try
         {
            delegate = factory.createClassLoaderPolicy();
            if (delegate == null)
            {
               log.trace("Factory did not create a delegate: " + factory);
            }
            else
            {
               ClassLoaderPolicy policy = (ClassLoaderPolicy) delegate;
               initialise(policy);
               this.delegate = delegate;
            }
         }
         catch (Throwable t)
         {
            log.warn("Unexpected error creating policy from factory: " + factory, t);
         }
      }
      return delegate;
   }

   /**
    * Callback to initialise policy
    * 
    * @param policy the policy
    */
   protected void initialise(ClassLoaderPolicy policy)
   {
      // Nothing by default
   }

   /**
    * Get BaseClassLoader.
    *
    * @param message the msg
    * @param context the context; make sure this is always resource path
    * @return policy's BaseClassLoader
    */
   BaseClassLoader getBaseClassLoader(String message, String context)
   {
      BaseClassLoader result = null;
      BaseClassLoaderPolicy policy = getPolicy();
      if (policy != null)
         result = policy.getClassLoaderUnchecked();
      if (result == null)
         log.warn("Not " + message + context + " from policy that has no classLoader: " + toLongString());
      return result;
   }

   /**
    * Transform, if needed, to resource path.
    *
    * @param context the context
    * @param adapter the adapter
    * @return potential transformation result
    */
   protected String toResourcePath(String context, ResourcePathAdapter adapter)
   {
      return context; // do nothing
   }

   public Class<?> loadClass(String className)
   {
      String path = toResourcePath(className, CLASS_TO_PATH);
      BaseClassLoader classLoader = getBaseClassLoader("loading class ", path);
      if (classLoader != null)
         return classLoader.loadClassLocally(className);
      return null;
   }
   
   public URL getResource(String name)
   {
      BaseClassLoader classLoader = getBaseClassLoader("getting resource ", name);
      if (classLoader != null)
         return classLoader.getResourceLocally(name);
      return null;
   }

   // FindBugs: The Set doesn't use equals/hashCode
   public void getResources(String name, Set<URL> urls) throws IOException
   {
      BaseClassLoader classLoader = getBaseClassLoader("getting resources ", name);
      if (classLoader != null)
         classLoader.getResourcesLocally(name, urls);
   }

   public Package getPackage(String name)
   {
      String path = toResourcePath(name, PACKAGE_TO_PATH);
      BaseClassLoader classLoader = getBaseClassLoader("getting package ", path);
      if (classLoader != null)
         return classLoader.getPackageLocally(name);
      return null;
   }

   public void getPackages(Set<Package> packages)
   {
      if (delegate == null)
         return;

      BaseClassLoader classLoader = delegate.getClassLoaderUnchecked();
      if (classLoader != null)
         classLoader.getPackagesLocally(packages);
   }

   public Class<?> checkClassCache(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      BaseClassLoaderPolicy policy = getPolicy();
      if (policy != null)
      {
         BaseClassLoaderDomain domain = policy.getClassLoaderDomain();
         if (domain != null)
            return domain.checkClassCache(classLoader, name, path, allExports);
      }
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
      if (delegate != null)
         builder.append("{delegate=").append(delegate);
      else
         builder.append("{factory=").append(factory);
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

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@").append(Integer.toHexString(System.identityHashCode(this)));
      if (delegate != null)
         builder.append("{delegate=").append(delegate);
      else
         builder.append("{factory=").append(factory);
      builder.append('}');
      return builder.toString();
   }

   protected static class ClassToPathResourceAdapter implements ResourcePathAdapter
   {
      public String toResourcePath(String context)
      {
         return ClassLoaderUtils.classNameToPath(context);
      }
   }

   protected static class PackageToPathResourceAdapter implements ResourcePathAdapter
   {
      public String toResourcePath(String context)
      {
         return ClassLoaderUtils.packageToPath(context);
      }
   }
}
