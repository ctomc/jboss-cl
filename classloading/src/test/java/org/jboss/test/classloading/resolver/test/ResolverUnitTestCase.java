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
package org.jboss.test.classloading.resolver.test;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.ResolutionContext;
import org.jboss.classloading.spi.dependency.Resolver;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.test.AbstractMockClassLoaderUnitTest;
import org.jboss.test.classloading.resolver.support.a.A;

/**
 * ResolverUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ResolverUnitTestCase extends AbstractMockClassLoaderUnitTest implements Resolver
{
   public static Test suite()
   {
      return suite(ResolverUnitTestCase.class);
   }

   static String packageA = ClassLoaderUtils.getClassPackageName(A.class.getName());
   
   boolean doResolve = true;
   KernelControllerContext contextA = null;
   ClassLoader other = null;
   
   public ResolverUnitTestCase(String name)
   {
      super(name);
   }
   
   public boolean resolve(ResolutionContext context)
   {
      if (doResolve == false)
         return false;
      
      Requirement requirement = context.getRequirement();
      if (requirement instanceof PackageRequirement == false)
         return false;
      
      PackageRequirement packageRequirement = (PackageRequirement) requirement;
      if (packageA.equals(packageRequirement.getName()) == false)
         return false;
      
      if (other != null)
         return true;

      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      a.setPathsAndPackageNames(A.class);
      try
      {
         contextA = install(a);
         other = assertClassLoader(contextA);
         return true;
      }
      catch (Exception e)
      {
         getLog().warn("Error:", e);
      }
      return false;
   }

   public void testResolve() throws Exception
   {
      doResolve = true;

      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.getRequirements().addRequirement(new PackageRequirement(ClassLoaderUtils.getClassPackageName(A.class.getName())));
      KernelControllerContext context = install(metaData);
      try
      {
         ClassLoader cl = assertClassLoader(context);
         assertNotNull("Should have resolved the other classloader", other);
         assertLoadClass(A.class, cl, other);
      }
      finally
      {
         uninstall(context);
      }
   }

   public void testNotResolved() throws Exception
   {
      doResolve = false;
      
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      MockClassLoadingMetaData metaData = new MockClassLoadingMetaData("test");
      metaData.getRequirements().addRequirement(factory.createRequirePackage(packageA));
      KernelControllerContext context = install(metaData);
      try
      {
         assertNoClassLoader(context);
         assertNull("Should not have resolved the other classloader", other);
      }
      finally
      {
         uninstall(context);
      }
   }

   protected void setUp() throws Exception
   {
      super.setUp();
      ClassLoading classLoading = getBean("ClassLoading", ClassLoading.class);
      classLoading.addResolver(this);
   }

   protected void tearDown() throws Exception
   {
      if (contextA != null)
         uninstall(contextA);
      super.tearDown();
   }
}
