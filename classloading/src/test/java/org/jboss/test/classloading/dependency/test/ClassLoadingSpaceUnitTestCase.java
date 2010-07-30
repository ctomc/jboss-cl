/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloading.dependency.test;

import java.lang.reflect.Method;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ClassLoaderCache;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

import junit.framework.Test;

/**
 * ClassLoadingSpaceUnitTestCase.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoadingSpaceUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   private Method getCache;

   public static Test suite()
   {
      return suite(ClassLoadingSpaceUnitTestCase.class);
   }

   public ClassLoadingSpaceUnitTestCase(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      getCache = Module.class.getDeclaredMethod("getCache");
      getCache.setAccessible(true);
   }

   @Override
   protected void tearDown() throws Exception
   {
      if (getCache != null)
      {
         getCache.setAccessible(false);
         getCache = null;
      }

      super.tearDown();
   }

   protected ClassLoaderCache getCache(ClassLoader cl)
   {
      try
      {
         Module module = ClassLoading.getModuleForClassLoader(cl);
         getCache.setAccessible(true);
         return (ClassLoaderCache) getCache.invoke(module);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void testCacheViaPackages() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
         b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);
         try
         {
            MockClassLoadingMetaData c = new MockClassLoadingMetaData("c");
            c.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
            c.getRequirements().addRequirement(factory.createRequirePackage(B.class.getPackage().getName()));
            c.getCapabilities().addCapability(factory.createPackage(C.class.getPackage().getName()));
            c.setPathsAndPackageNames(C.class);
            KernelControllerContext contextC = install(c);
            try
            {
               ClassLoader clA = assertClassLoader(contextA);
               ClassLoader clB = assertClassLoader(contextB);
               ClassLoader clC = assertClassLoader(contextC);

               ClassLoaderCache clcA = getCache(clA);
               ClassLoaderCache clcB = getCache(clB);
               ClassLoaderCache clcC = getCache(clC);

               assertSame(clcA, clcB);
               assertSame(clcB, clcC);

               String pathA = ClassLoaderUtils.classNameToPath(A.class);
               assertNull(clcA.getCachedLoader(pathA));
               assertLoadClass(A.class, clB, clA);
               assertNotNull(clcA.getCachedLoader(pathA));
               assertLoadClass(A.class, clC, clA);
            }
            finally
            {
               uninstall(contextC);
            }
         }
         finally
         {
            uninstall(contextB);
         }
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }

   public void testCacheViaModules() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.setPathsAndPackageNames(A.class);
      a.setExportedPackages(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("a"));
         b.setPathsAndPackageNames(B.class);
         b.setExportedPackages(B.class);
         KernelControllerContext contextB = install(b);
         try
         {
            MockClassLoadingMetaData c = new MockClassLoadingMetaData("c");
            c.getRequirements().addRequirement(factory.createRequireModule("a"));
            c.getRequirements().addRequirement(factory.createRequireModule("b"));
            c.setPathsAndPackageNames(C.class);
            c.setExportedPackages(C.class);
            KernelControllerContext contextC = install(c);
            try
            {
               ClassLoader clA = assertClassLoader(contextA);
               ClassLoader clB = assertClassLoader(contextB);
               ClassLoader clC = assertClassLoader(contextC);

               ClassLoaderCache clcA = getCache(clA);
               ClassLoaderCache clcB = getCache(clB);
               ClassLoaderCache clcC = getCache(clC);

               assertSame(clcA, clcB);
               assertSame(clcB, clcC);

               String pathA = ClassLoaderUtils.classNameToPath(A.class);
               assertNull(clcA.getCachedLoader(pathA));
               assertLoadClass(A.class, clB, clA);
               assertNotNull(clcA.getCachedLoader(pathA));
               assertLoadClass(A.class, clC, clA);
            }
            finally
            {
               uninstall(contextC);
            }
         }
         finally
         {
            uninstall(contextB);
         }
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
}
