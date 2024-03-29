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

import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.dependency.spi.DependencyInfo;
import org.jboss.dependency.spi.DependencyItem;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

/**
 * DependencyUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class DependencyUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(DependencyUnitTestCase.class);
   }

   public DependencyUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testSmoke() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
   
   public void testBDependsACorrectWay() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));
         KernelControllerContext contextB = install(b);
         try
         {
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
         assertNoClassLoader(contextB);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
   
   public void testBDependsAWrongWay() throws Exception
   {
      MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
      b.setPathsAndPackageNames(B.class);
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
      b.setRequirements(Collections.singletonList(requirement));
      KernelControllerContext contextB = install(b);
      try
      {
         assertNoClassLoader(contextB);

         MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
         a.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA = install(a);
         try
         {
            ClassLoader clA = assertClassLoader(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextA);
         }
         assertNoClassLoader(contextA);
      }
      finally
      {
         uninstall(contextB);
      }
      assertNoClassLoader(contextB);
   }
   
   public void testBDependsARedeployA() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));
         KernelControllerContext contextB = install(b);
         try
         {
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
            
            uninstall(contextA);
            try
            {
               assertNoClassLoader(contextA);
               assertNoClassLoader(contextB);
            }
            finally
            {
               contextA = install(a);
            }
            clA = assertClassLoader(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
         assertNoClassLoader(contextB);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
   
   public void testBDependsARedeployB() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));
         KernelControllerContext contextB = install(b);
         try
         {
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
            
            uninstall(contextB);
            try
            {
               assertLoadClass(A.class, clA);
               assertLoadClassFail(B.class, clA);
               assertNoClassLoader(contextB);
            }
            finally
            {
               contextB = install(b);
            }
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
         assertNoClassLoader(contextB);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }

   /**
    * This checks if Module::removeIDependOn cleans up properly.
    * If/when the cleanup algorithm changes, this test could be changed as well.
    *
    * @throws Exception for any error
    */
   public void testDependencyRemoval() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(a);
      try
      {
         DependencyInfo infoA = contextA.getDependencyInfo();

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));

         KernelControllerContext contextB = install(b);
         try
         {
            Set<DependencyItem> items = infoA.getDependsOnMe(null);
            assertEquals(1, items.size());
         }
         finally
         {
            uninstall(contextB);
            Set<DependencyItem> items = infoA.getDependsOnMe(null);
            assertEquals(0, items.size());
         }
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
   
   public void testBDependsALazyShutdownA() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      a.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));
         KernelControllerContext contextB = install(b);
         try
         {
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
            
            uninstall(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            assertModule(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
         assertNoClassLoader(contextB);
      }
      finally
      {
      }
      assertNoClassLoader(contextA);
   }
   
   public void testBDependsALazyShutdownAConfiguredOnDomain() throws Exception
   {
      ClassLoaderDomain domain = system.createAndRegisterDomain("LazyShutdown", ParentPolicy.BEFORE_BUT_JAVA_ONLY);
      domain.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      a.setDomain(domain.getName());
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         b.setDomain(domain.getName());
         ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
         Requirement requirement = factory.createRequirePackage(A.class.getPackage().getName());
         b.setRequirements(Collections.singletonList(requirement));
         KernelControllerContext contextB = install(b);
         try
         {
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
            
            uninstall(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            assertModule(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertLoadClass(A.class, clA);
         assertLoadClassFail(B.class, clA);
         assertNoClassLoader(contextB);
      }
      finally
      {
      }
      assertNoClassLoader(contextA);
   }
   
   public void testCircularModule() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      a.getRequirements().addRequirement(factory.createRequireModule("b"));
      KernelControllerContext contextA = install(a);
      try
      {
         assertNoClassLoader(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         b.getRequirements().addRequirement(factory.createRequireModule("a"));
         KernelControllerContext contextB = install(b);
         try
         {
            ClassLoader clA = assertClassLoader(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertNoClassLoader(contextA);
         assertNoClassLoader(contextB);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }

   public void testCircularPackage() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      a.getRequirements().addRequirement(factory.createRequirePackage(B.class.getPackage().getName()));
      KernelControllerContext contextA = install(a);
      try
      {
         assertNoClassLoader(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         b.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
         KernelControllerContext contextB = install(b);
         try
         {
            ClassLoader clA = assertClassLoader(contextA);
            assertLoadClass(A.class, clA);
            assertLoadClassFail(B.class, clA);
            ClassLoader clB = assertClassLoader(contextB);
            assertLoadClass(B.class, clB);
            assertLoadClass(A.class, clB, clA);
         }
         finally
         {
            uninstall(contextB);
         }
         assertNoClassLoader(contextA);
         assertNoClassLoader(contextB);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }

   public void testTransitiveCircularPackage() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      a.getRequirements().addRequirement(factory.createRequirePackage(B.class.getPackage().getName()));
      KernelControllerContext contextA = install(a);
      try
      {
         assertNoClassLoader(contextA);

         MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
         b.setPathsAndPackageNames(B.class);
         b.getRequirements().addRequirement(factory.createRequirePackage(C.class.getPackage().getName()));
         KernelControllerContext contextB = install(b);
         try
         {
            assertNoClassLoader(contextB);

            MockClassLoadingMetaData c = new MockClassLoadingMetaData("c");
            c.setPathsAndPackageNames(C.class);
            c.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
            KernelControllerContext contextC = install(c);
            try
            {
               ClassLoader clA = assertClassLoader(contextA);
               assertLoadClass(A.class, clA);
               assertLoadClassFail(B.class, clA);
               ClassLoader clB = assertClassLoader(contextB);
               assertLoadClass(B.class, clB);
               assertLoadClass(B.class, clA, clB);
               ClassLoader clC = assertClassLoader(contextC);
               assertLoadClass(C.class, clB, clC);
               assertLoadClass(C.class, clC);
               assertLoadClass(A.class, clC, clA);
            }
            finally
            {
               uninstall(contextC);
            }
            assertNoClassLoader(contextA);
            assertNoClassLoader(contextB);
            assertNoClassLoader(contextC);
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
