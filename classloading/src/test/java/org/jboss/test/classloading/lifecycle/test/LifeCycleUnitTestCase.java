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
package org.jboss.test.classloading.lifecycle.test;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.lifecycle.support.a.A;
import org.jboss.test.classloading.lifecycle.support.a.MockLifeCycle;

/**
 * LifeCycleUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class LifeCycleUnitTestCase extends AbstractMockLifeCycleUnitTest
{
   public static Test suite()
   {
      return suite(LifeCycleUnitTestCase.class);
   }

   public LifeCycleUnitTestCase(String name)
   {
      super(name);
   }

   static String packageA = ClassLoaderUtils.getClassPackageName(A.class.getName());
   
   public void testInstall() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testResolve() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         
         resolve(context);
         assertResolved(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testUnresolve() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         
         resolve(context);
         assertResolved(context);
         
         unresolve(context);
         assertUnresolved(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testStart() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         
         start(context);
         assertStarted(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testStop() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         
         start(context);
         assertStarted(context);
         
         stop(context);
         assertResolved(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testNoLazyResolve() throws Exception
   {
      MockClassLoadingMetaData metaDataA = new MockClassLoadingMetaData("a");
      metaDataA.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(metaDataA);

      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.getRequirements().addRequirement(factory.createRequirePackage(packageA));
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         assertNotResolved(contextA);
         
         resolve(context);
         assertNotResolved(context);
         assertNotResolved(contextA);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testLazyResolve() throws Exception
   {
      MockClassLoadingMetaData metaDataA = new MockClassLoadingMetaData("a");
      metaDataA.setPathsAndPackageNames(A.class);
      KernelControllerContext contextA = install(metaDataA);

      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.getRequirements().addRequirement(new PackageRequirement(ClassLoaderUtils.getClassPackageName(A.class.getName())));
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         MockLifeCycle lifeCycleA = assertNotResolved(contextA);
         lifeCycleA.lazyResolve = true;
         
         resolve(context);
         ClassLoader cl = assertResolved(context);

         ClassLoader clA = assertResolved(contextA);
         
         assertLoadClass(A.class, cl, clA);
         assertTrue("Should get resolve invocation", lifeCycleA.gotResolve);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testNotLazyStart() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.setPathsAndPackageNames(A.class);
      KernelControllerContext context = install(metaData);
      try
      {
         assertNotResolved(context);
         
         resolve(context);
         ClassLoader cl = assertResolved(context);
         assertLoadClass(A.class, cl);
         assertResolved(context);
      }
      finally
      {
         uninstall(context);
      }
   }
   
   public void testLazyStart() throws Exception
   {
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.setPathsAndPackageNames(A.class);
      KernelControllerContext context = install(metaData);
      try
      {
         MockLifeCycle lifeCycle = assertNotResolved(context);
         lifeCycle.lazyStart = true;
         
         resolve(context);
         ClassLoader cl = assertResolved(context);
         assertLoadClass(A.class, cl);
         assertTrue("Should get start invocation", lifeCycle.gotStart);
      }
      finally
      {
         uninstall(context);
      }
   }
}
