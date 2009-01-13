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
import org.jboss.classloading.spi.version.VersionRange;
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
   
   public void testSeveralModulesWithSamePackages() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();

      MockClassLoadingMetaData x = new MockClassLoadingMetaData("x");
      x.getCapabilities().addCapability(factory.createModule("ModuleX"));
      x.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      x.setPathsAndPackageNames(A.class);
      KernelControllerContext contextX = install(x);

      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);

      MockClassLoadingMetaData y = new MockClassLoadingMetaData("y");
      y.getCapabilities().addCapability(factory.createModule("ModuleY"));
      y.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      y.setPathsAndPackageNames(A.class);
      KernelControllerContext contextY = install(y);

      try
      {
         ClassLoader clX = assertClassLoader(contextX);
         ClassLoader clA = assertClassLoader(contextA);
         ClassLoader clY = assertClassLoader(contextY);

         assertLoadClass(A.class, clX);
         assertLoadClass(A.class, clA);
         assertLoadClass(A.class, clY);
         
         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("ModuleA"));
         b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);

         try
         {
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
            
            //Modules do not get the same corresponding module as when trying to load classes 
            Module moduleA = assertModule(contextA);
            Module moduleB = assertModule(contextB);
            Module result = moduleB.getModuleForClass(A.class.getName());
            assertSame(moduleA, result);
         }
         finally
         {
            uninstall(contextB);
         }
      }
      finally
      {
         uninstall(contextX);
         uninstall(contextA);
         uninstall(contextY);
      }
   }
   
   public void testSeveralModulesWithSameNamesDifferentVersions() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();

      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1");
      a1.getCapabilities().addCapability(factory.createModule("ModuleA", "1.0.0"));
      a1.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);

      MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2");
      a2.getCapabilities().addCapability(factory.createModule("ModuleA", "2.0.0"));
      a2.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a2.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA2 = install(a2);

      MockClassLoadingMetaData a3 = new MockClassLoadingMetaData("a3");
      a3.getCapabilities().addCapability(factory.createModule("ModuleA", "3.0.0"));
      a3.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a3.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA3 = install(a3);

      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         ClassLoader clA2 = assertClassLoader(contextA2);
         ClassLoader clA3 = assertClassLoader(contextA3);

         assertLoadClass(A.class, clA1);
         assertLoadClass(A.class, clA2);
         assertLoadClass(A.class, clA3);
         
         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequireModule("ModuleA", new VersionRange("2.0.0", true, "3.0.0", false)));
         b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);

         try
         {
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA2);
            
            //Modules do not get the same corresponding module as when trying to load classes 
            Module moduleA2 = assertModule(contextA2);
            Module moduleB = assertModule(contextB);
            Module result = moduleB.getModuleForClass(A.class.getName());
            assertSame(moduleA2, result);
         }
         finally
         {
            uninstall(contextB);
         }
      }
      finally
      {
         uninstall(contextA1);
         uninstall(contextA2);
         uninstall(contextA3);
      }
   }

   public void testSeveralModulesWithSamePackagesDifferentVersions() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();

      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1");
      a1.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a1.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName(), "1.0.0"));
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);

      MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2");
      a2.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a2.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName(), "2.0.0"));
      a2.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA2 = install(a2);

      MockClassLoadingMetaData a3 = new MockClassLoadingMetaData("a3");
      a3.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a3.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName(), "3.0.0"));
      a3.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA3 = install(a3);

      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         ClassLoader clA2 = assertClassLoader(contextA2);
         ClassLoader clA3 = assertClassLoader(contextA3);

         assertLoadClass(A.class, clA1);
         assertLoadClass(A.class, clA2);
         assertLoadClass(A.class, clA3);
         
         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName(), new VersionRange("2.0.0", true, "3.0.0", false)));
         b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
         b.setPathsAndPackageNames(B.class);
         KernelControllerContext contextB = install(b);

         try
         {
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA2);
            
            //Modules do not get the same corresponding module as when trying to load classes 
            Module moduleA2 = assertModule(contextA2);
            Module moduleB = assertModule(contextB);
            Module result = moduleB.getModuleForClass(A.class.getName());
            assertSame(moduleA2, result);
         }
         finally
         {
            uninstall(contextB);
         }
      }
      finally
      {
         uninstall(contextA1);
         uninstall(contextA2);
         uninstall(contextA3);
      }
   }

   public void testParentRedeployOtherDomain() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
      b.setDomain("ChildDomain");
      b.setParentDomain("ParentDomain");
      b.setJ2seClassLoadingCompliance(true);
      b.setPathsAndPackageNames(B.class);
      b.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
      KernelControllerContext contextB = install(b);
      try
      {
         assertNoClassLoader(contextB);

         MockClassLoadingMetaData aParent = new MockClassLoadingMetaData("aParent");
         aParent.setPathsAndPackageNames(A.class);
         aParent.setDomain("ParentDomain");
         KernelControllerContext contextParentA = install(aParent);
         try
         {
            ClassLoader clParentA = assertClassLoader(contextParentA);
            assertLoadClass(A.class, clParentA);
            assertLoadClassFail(B.class, clParentA);

            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(A.class, clParentA);
            assertLoadClass(B.class, clB);
            
            Module moduleParentA = assertModule(contextParentA);
            Module moduleB = assertModule(contextB);
            Module result = moduleB.getModuleForClass(A.class.getName());
            assertSame(moduleParentA, result);
         }
         finally
         {
            uninstall(contextParentA);
         }
         assertNoClassLoader(contextParentA);

         assertNoClassLoader(contextB);

         contextParentA = install(aParent);
         try
         {
            ClassLoader clParentA = assertClassLoader(contextParentA);
            assertLoadClass(A.class, clParentA);
            assertLoadClassFail(B.class, clParentA);

            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(A.class, clParentA);
            assertLoadClass(B.class, clB);

            Module moduleParentA = assertModule(contextParentA);
            Module moduleB = assertModule(contextB);
            Module result = moduleB.getModuleForClass(A.class.getName());
            assertSame(moduleParentA, result);
         }
         finally
         {
            uninstall(contextParentA);
         }
         assertNoClassLoader(contextParentA);
      }
      finally
      {
         uninstall(contextB);
      }
      assertNoClassLoader(contextB);
   }

   //   public void testParentRedeployOtherDomain() throws Exception
//   {
//      ClassPool poolB = null;
//      Result resultB = new Result();
//      final String parentDomainName = "ParentDomain";
//      final String childDomainName = "ChildDomain";
//      try
//      {
//         BundleInfoBuilder builderB = BundleInfoBuilder.getBuilder().
//            createRequirePackage(PACKAGE_A);
//         try
//         {
//            poolB = createChildDomainParentFirstClassPool(resultB, "b", childDomainName, parentDomainName, builderB, JAR_B_1);
//            fail("Should be no loader");
//         }
//         catch(NoSuchClassLoaderException e)
//         {
//         }
//         assertNoClassPool(resultB);
//         
//         ClassPool clParentA = null;
//         Result resultParentA = new Result();
//         try
//         {
//            clParentA = createChildDomainParentFirstClassPool(resultParentA, "aParent", parentDomainName, true, JAR_A_1);
//            assertLoadCtClass(CLASS_A, clParentA);
//            assertCannotLoadCtClass(CLASS_B, clParentA);
//
//            poolB = assertClassPool(resultB.getFactory());
//            assertLoadCtClass(CLASS_A, clParentA);
//            assertCannotLoadCtClass(CLASS_B, clParentA);
//            assertLoadCtClass(CLASS_A, poolB, clParentA);
//         }
//         finally
//         {
//            unregisterClassPool(clParentA);
//            unregisterDomain(parentDomainName);
//         }
//         assertNoClassPool(resultParentA);
//         assertNoClassPool(resultB);
//
//         try
//         {
//            clParentA = createChildDomainParentFirstClassPool(resultParentA, "aParent", parentDomainName, true, JAR_A_1);
//            assertLoadCtClass(CLASS_A, clParentA);
//            assertCannotLoadCtClass(CLASS_B, clParentA);
//
//            poolB = assertClassPool(resultB.getFactory());
//            assertLoadCtClass(CLASS_A, clParentA);
//            assertCannotLoadCtClass(CLASS_B, clParentA);
//            assertLoadCtClass(CLASS_A, poolB, clParentA);
//         }
//         finally
//         {
//            unregisterClassPool(clParentA);
//            unregisterDomain(parentDomainName);
//         }
//      }
//      finally
//      {
//         unregisterClassPool(poolB);
//         unregisterDomain(childDomainName);
//      }
//      assertNoClassPool(resultB);
//   }   
}
