/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import java.net.URL;
import java.util.Enumeration;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

/**
 * ModuleClassLoadingUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ModuleClassLoadingUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(ModuleClassLoadingUnitTestCase.class);
   }

   public ModuleClassLoadingUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testLoadClassFromThisModule() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         Module moduleA = assertModule(contextA);
         Class<?> result = moduleA.loadClass(A.class.getName());
         assertEquals(clA, result.getClassLoader());
         
         Module other = moduleA.getModuleForClass(A.class.getName());
         assertEquals(moduleA, other);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
   
   public void testLoadClassFromOtherModule() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         Module moduleA = assertModule(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("ModuleA"));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);
         try
         {
            assertClassLoader(contextB);
            Module moduleB = assertModule(contextB);
            Class<?> result = moduleB.loadClass(A.class.getName());
            assertEquals(clA, result.getClassLoader());
            
            Module other = moduleB.getModuleForClass(A.class.getName());
            assertEquals(moduleA, other);
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
   
   public void testLoadClassNotFound() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         assertClassLoader(contextA);
         Module moduleA = assertModule(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("ModuleA"));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);
         try
         {
            assertClassLoader(contextB);
            try
            {
               moduleA.loadClass(C.class.getName());
               fail("Should not be here!");
            }
            catch (Exception e)
            {
               checkThrowable(ClassNotFoundException.class, e);
            }
            try
            {
               moduleA.getModuleForClass(C.class.getName());
               fail("Should not be here!");
            }
            catch (Exception e)
            {
               checkThrowable(ClassNotFoundException.class, e);
            }
            Module moduleB = assertModule(contextB);
            try
            {
               moduleB.loadClass(C.class.getName());
               fail("Should not be here!");
            }
            catch (Exception e)
            {
               checkThrowable(ClassNotFoundException.class, e);
            }
            try
            {
               moduleB.getModuleForClass(C.class.getName());
               fail("Should not be here!");
            }
            catch (Exception e)
            {
               checkThrowable(ClassNotFoundException.class, e);
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
   
   public void testLoadClassNotAModule() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         assertClassLoader(contextA);
         Module moduleA = assertModule(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("ModuleA"));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);
         try
         {
            assertClassLoader(contextB);
            Module moduleB = assertModule(contextB);
            moduleA.loadClass(Object.class.getName());
            assertNull(moduleA.getModuleForClass(Object.class.getName()));
            moduleB.loadClass(Object.class.getName());
            assertNull(moduleB.getModuleForClass(Object.class.getName()));
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
   
   public void testGetResourceFromThisModule() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         assertClassLoader(contextA);
         String path = ClassLoaderUtils.classNameToPath(A.class);
         URL expected = getResource("/" + path);
         Module moduleA = assertModule(contextA);
         URL actual = moduleA.getResource(path);
         assertEquals(expected, actual);
         
         Enumeration<URL> actuals = moduleA.getResources(path);
         assertTrue(actuals.hasMoreElements());
         actual = actuals.nextElement();
         assertEquals(expected, actual);
         assertFalse(actuals.hasMoreElements());
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
}
