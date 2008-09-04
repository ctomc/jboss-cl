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

import junit.framework.Test;

import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

/**
 * ConflictingPackageUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ConflictingRequirementUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(ConflictingRequirementUnitTestCase.class);
   }

   public ConflictingRequirementUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testConflictingRequirement() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);
         assertLoadClassFail(C.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
            assertLoadClassFail(C.class, clA2);

            MockClassLoadingMetaData b1 = new MockClassLoadingMetaData("b", "1.0.0");
            b1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("2.0.0", "3.0.0")));
            b1.setPathsAndPackageNames(B.class);
            KernelControllerContext contextB1 = install(b1);
            try
            {
               ClassLoader clB1 = assertClassLoader(contextB1);
               assertLoadClass(B.class, clB1);
               assertLoadClass(A.class, clB1, clA2);
               assertLoadClassFail(C.class, clB1);

               MockClassLoadingMetaData c1 = new MockClassLoadingMetaData("c", "1.0.0");
               c1.getRequirements().addRequirement(factory.createRequireModule("b"));
               c1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
               c1.setPathsAndPackageNames(C.class);
               KernelControllerContext contextC1 = install(c1);
               try
               {
                  assertNoClassLoader(contextC1);
               }
               finally
               {
                  uninstall(contextC1);
               }
               assertNoClassLoader(contextC1);
            }
            finally
            {
               uninstall(contextB1);
            }
            assertNoClassLoader(contextB1);
         }
         finally
         {
            uninstall(contextA2);
         }
         assertNoClassLoader(contextA2);
      }
      finally
      {
         uninstall(contextA1);
      }
      assertNoClassLoader(contextA1);
   }
   
   public void testConflictingRequirementNoJoin() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);
         assertLoadClassFail(C.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
            assertLoadClassFail(C.class, clA2);

            MockClassLoadingMetaData b1 = new MockClassLoadingMetaData("b", "1.0.0");
            b1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("2.0.0", "3.0.0")));
            b1.setPathsAndPackageNames(B.class);
            KernelControllerContext contextB1 = install(b1);
            try
            {
               ClassLoader clB1 = assertClassLoader(contextB1);
               assertLoadClass(B.class, clB1);
               assertLoadClass(A.class, clB1, clA2);
               assertLoadClassFail(C.class, clB1);

               MockClassLoadingMetaData c1 = new MockClassLoadingMetaData("c", "1.0.0");
               c1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
               c1.setPathsAndPackageNames(C.class);
               KernelControllerContext contextC1 = install(c1);
               try
               {
                  ClassLoader clC1 = assertClassLoader(contextC1);
                  assertLoadClass(C.class, clC1);
                  assertLoadClass(A.class, clC1, clA1);
                  assertLoadClassFail(B.class, clC1);
               }
               finally
               {
                  uninstall(contextC1);
               }
               assertNoClassLoader(contextC1);
            }
            finally
            {
               uninstall(contextB1);
            }
            assertNoClassLoader(contextB1);
         }
         finally
         {
            uninstall(contextA2);
         }
         assertNoClassLoader(contextA2);
      }
      finally
      {
         uninstall(contextA1);
      }
      assertNoClassLoader(contextA1);
   }
   
   public void testRequirementIsConsistent() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);
         assertLoadClassFail(C.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
            assertLoadClassFail(C.class, clA2);

            MockClassLoadingMetaData b1 = new MockClassLoadingMetaData("b", "1.0.0");
            b1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
            b1.setPathsAndPackageNames(B.class);
            KernelControllerContext contextB1 = install(b1);
            try
            {
               ClassLoader clB1 = assertClassLoader(contextB1);
               assertLoadClass(B.class, clB1);
               assertLoadClass(A.class, clB1, clA1);
               assertLoadClassFail(C.class, clB1);

               MockClassLoadingMetaData c1 = new MockClassLoadingMetaData("c", "1.0.0");
               c1.getRequirements().addRequirement(factory.createRequireModule("b"));
               c1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
               c1.setPathsAndPackageNames(C.class);
               KernelControllerContext contextC1 = install(c1);
               try
               {
                  ClassLoader clC1 = assertClassLoader(contextC1);
                  assertLoadClass(C.class, clC1);
                  assertLoadClass(A.class, clC1, clA1);
                  assertLoadClass(B.class, clC1, clB1);
               }
               finally
               {
                  uninstall(contextC1);
               }
               assertNoClassLoader(contextC1);
            }
            finally
            {
               uninstall(contextB1);
            }
            assertNoClassLoader(contextB1);
         }
         finally
         {
            uninstall(contextA2);
         }
         assertNoClassLoader(contextA2);
      }
      finally
      {
         uninstall(contextA1);
      }
      assertNoClassLoader(contextA1);
   }
   
   public void testRequirementRedeploy() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);
         assertLoadClassFail(C.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
            assertLoadClassFail(C.class, clA2);

            MockClassLoadingMetaData b1 = new MockClassLoadingMetaData("b", "1.0.0");
            b1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
            b1.setPathsAndPackageNames(B.class);
            KernelControllerContext contextB1 = install(b1);
            try
            {
               ClassLoader clB1 = assertClassLoader(contextB1);
               assertLoadClass(B.class, clB1);
               assertLoadClass(A.class, clB1, clA1);
               assertLoadClassFail(C.class, clB1);

               MockClassLoadingMetaData c1 = new MockClassLoadingMetaData("c", "1.0.0");
               c1.getRequirements().addRequirement(factory.createRequireModule("b"));
               c1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("2.0.0", "3.0.0")));
               c1.setPathsAndPackageNames(C.class);
               KernelControllerContext contextC1 = install(c1);
               try
               {
                  assertNoClassLoader(contextC1);
               }
               finally
               {
                  uninstall(contextC1);
               }
               assertNoClassLoader(contextC1);

               c1 = new MockClassLoadingMetaData("c", "1.0.0");
               c1.getRequirements().addRequirement(factory.createRequireModule("b"));
               c1.getRequirements().addRequirement(factory.createRequireModule("a", new VersionRange("1.0.0", "2.0.0")));
               c1.setPathsAndPackageNames(C.class);
               contextC1 = install(c1);
               try
               {
                  ClassLoader clC1 = assertClassLoader(contextC1);
                  assertLoadClass(C.class, clC1);
                  assertLoadClass(A.class, clC1, clA1);
                  assertLoadClass(B.class, clC1, clB1);
               }
               finally
               {
                  uninstall(contextC1);
               }
               assertNoClassLoader(contextC1);
            }
            finally
            {
               uninstall(contextB1);
            }
            assertNoClassLoader(contextB1);
         }
         finally
         {
            uninstall(contextA2);
         }
         assertNoClassLoader(contextA2);
      }
      finally
      {
         uninstall(contextA1);
      }
      assertNoClassLoader(contextA1);
   }
}
