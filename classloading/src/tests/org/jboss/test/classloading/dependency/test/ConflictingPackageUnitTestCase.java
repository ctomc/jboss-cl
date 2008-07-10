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
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;

/**
 * ConflictingPackageUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ConflictingPackageUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(ConflictingPackageUnitTestCase.class);
   }

   public ConflictingPackageUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testConflictingPackage() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2", "2.0.0");
         a2.getRequirements().addRequirement(factory.createRequireModule("a1"));
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            assertNoClassLoader(contextA2);
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
   
   public void testConflictingPackageNoImport() throws Exception
   {
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
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
   
   public void testConflictingPackageNoExport() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1", "1.0.0");
      a1.setPaths(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2", "2.0.0");
         a2.getRequirements().addRequirement(factory.createRequireModule("a1"));
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
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
   
   public void testConflictingPackageUses() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1", "1.0.0");
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClass(A.class, clA1);
         assertLoadClassFail(B.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2", "2.0.0");
         a2.getRequirements().addRequirement(factory.createUsesPackage(A.class.getPackage().getName()));
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2, clA1);
            assertLoadClassFail(B.class, clA2);
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
   
   public void testConflictingPackageRedeploy() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1", "1.0.0");
      a1.getRequirements().addRequirement(factory.createRequireModule("a2"));
      a1.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA1 = install(a1);
      try
      {
         assertNoClassLoader(contextA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2", "2.0.0");
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClassFail(B.class, clA2);
            
            uninstall(contextA1);

            a1 = new MockClassLoadingMetaData("a1", "1.0.0");
            a1.getRequirements().addRequirement(factory.createRequireModule("a2"));
            a1.setPathsAndPackageNames(B.class);
            contextA1 = install(a1);

            ClassLoader clA1 = assertClassLoader(contextA1);
            assertLoadClass(A.class, clA2);
            assertLoadClass(B.class, clA1);
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
