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

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.plugins.metadata.PackageCapability.SplitPackagePolicy;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

/**
 * Test the split package policies.
 * 
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class SplitPackageDependencyUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(SplitPackageDependencyUnitTestCase.class);
   }

   public SplitPackageDependencyUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testSplitPackageDefault() throws Exception
   {
      MockClassLoadingMetaData ab = getModuleAB(null);
      MockClassLoadingMetaData ac = getModuleAC(null);
      
      KernelControllerContext contextAB = install(ab);
      try
      {
         ClassLoader clAB = assertClassLoader(contextAB);
         assertLoadClass(A.class, clAB);
         assertLoadClass(B.class, clAB);
         
         KernelControllerContext contextAC = install(ac);
         assertEquals("package conflict on A expected", ControllerState.ERROR, contextAC.getState());
         assertNotNull("package conflict on A expected", contextAC.getError());
         assertNoClassLoader(contextAC);
      }
      finally
      {
         uninstall(contextAB);
      }
      assertNoClassLoader(contextAB);
   }

   public void testSplitPackageError() throws Exception
   {
      MockClassLoadingMetaData ab = getModuleAB(null);
      MockClassLoadingMetaData ac = getModuleAC(SplitPackagePolicy.Error);
      
      KernelControllerContext contextAB = install(ab);
      try
      {
         ClassLoader clAB = assertClassLoader(contextAB);
         assertLoadClass(A.class, clAB);
         assertLoadClass(B.class, clAB);
         
         KernelControllerContext contextAC = install(ac);
         assertEquals("package conflict on A expected", ControllerState.ERROR, contextAC.getState());
         assertNotNull("package conflict on A expected", contextAC.getError());
         assertNoClassLoader(contextAC);
      }
      finally
      {
         uninstall(contextAB);
      }
      assertNoClassLoader(contextAB);
   }

   public void testSplitPackageFirst() throws Exception
   {
      MockClassLoadingMetaData ab = getModuleAB(null);
      MockClassLoadingMetaData ac = getModuleAC(SplitPackagePolicy.First);
      
      KernelControllerContext contextAB = install(ab);
      try
      {
         ClassLoader clAB = assertClassLoader(contextAB);
         assertLoadClass(A.class, clAB);
         assertLoadClass(B.class, clAB);
         
         KernelControllerContext contextAC = install(ac);
         ClassLoader clAC = assertClassLoader(contextAC);
         assertLoadClass(A.class, clAB);
         assertLoadClass(B.class, clAB);
         assertLoadClass(C.class, clAC);
      }
      finally
      {
         uninstall(contextAB);
      }
      assertNoClassLoader(contextAB);
   }

   public void testSplitPackageLast() throws Exception
   {
      MockClassLoadingMetaData ab = getModuleAB(null);
      MockClassLoadingMetaData ac = getModuleAC(SplitPackagePolicy.Last);
      
      KernelControllerContext contextAB = install(ab);
      try
      {
         ClassLoader clAB = assertClassLoader(contextAB);
         assertLoadClass(A.class, clAB);
         assertLoadClass(B.class, clAB);
         
         KernelControllerContext contextAC = install(ac);
         ClassLoader clAC = assertClassLoader(contextAC);
         System.out.println("FIXME: SplitPackagePolicy.Last");
         // assertLoadClass(A.class, clAC);
         assertLoadClass(B.class, clAB);
         assertLoadClass(C.class, clAC);
      }
      finally
      {
         uninstall(contextAB);
      }
      assertNoClassLoader(contextAB);
   }

   private MockClassLoadingMetaData getModuleAB(SplitPackagePolicy policy)
   {
      MockClassLoadingMetaData ab = new MockClassLoadingMetaData("ab");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      ab.getCapabilities().addCapability(factory.createModule("ModuleAB"));
      ab.getCapabilities().addCapability(createPackageCapability(A.class.getPackage().getName(), policy));
      ab.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
      ab.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
      ab.setPathsAndPackageNames(A.class, B.class);
      return ab;
   }

   private MockClassLoadingMetaData getModuleAC(SplitPackagePolicy policy)
   {
      MockClassLoadingMetaData ac = new MockClassLoadingMetaData("ac");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      ac.getCapabilities().addCapability(factory.createModule("ModuleAC"));
      ac.getCapabilities().addCapability(createPackageCapability(A.class.getPackage().getName(), policy));
      ac.getCapabilities().addCapability(factory.createPackage(C.class.getPackage().getName()));
      ac.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
      ac.setPathsAndPackageNames(A.class, C.class);
      return ac;
   }
   
   private PackageCapability createPackageCapability(String className, SplitPackagePolicy policy)
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      PackageCapability capability = (PackageCapability)factory.createPackage(className);
      if (policy != null)
         capability.setSplitPackagePolicy(policy);
      
      return capability;
   }
}
