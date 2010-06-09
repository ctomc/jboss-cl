/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Inc., and individual contributors
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

import org.jboss.classloader.spi.ImportType;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.metadata.helpers.AbstractRequirement;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;

import junit.framework.Test;

/**
 * WildcardPackageUnitTestCase.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class WildcardPackageUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(WildcardPackageUnitTestCase.class);
   }

   public WildcardPackageUnitTestCase(String name)
   {
      super(name);
   }

   public void testWildcardPackage() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();

      MockClassLoadingMetaData a1 = new MockClassLoadingMetaData("a1");
      a1.getRequirements().addRequirement(factory.createWildcardPackage(A.class.getPackage().getName()));
      KernelControllerContext contextA1 = install(a1);
      try
      {
         ClassLoader clA1 = assertClassLoader(contextA1);
         assertLoadClassFail(A.class, clA1);

         MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a2");
         a2.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
         a2.setPathsAndPackageNames(A.class);
         KernelControllerContext contextA2 = install(a2);
         try
         {
            ClassLoader clA2 = assertClassLoader(contextA2);
            assertLoadClass(A.class, clA2);
            assertLoadClass(A.class, clA1, clA2);
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

   // deploy B first, then A
   public void testWildcardImportAexportAandB2() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();

      MockClassLoadingMetaData b = new MockClassLoadingMetaData("b");
      b.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
      b.setPathsAndPackageNames(A.class, B.class);
      KernelControllerContext contextB = install(b);
      try
      {
         assertClassLoader(contextB); // force CL install

         MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");

         Requirement aPackage = factory.createWildcardPackage(A.class.getPackage().getName());
         ((AbstractRequirement)aPackage).setImportType(ImportType.AFTER);

         a.getRequirements().addRequirement(aPackage);
         a.setPaths(A.class);
         KernelControllerContext contextA = install(a);
         try
         {
            ClassLoader clA = assertClassLoader(contextA);
            assertLoadClassFail(B.class, clA);
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
}