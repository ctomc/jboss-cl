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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoaderPolicyModule;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.MockResourceVisitor;
import org.jboss.test.classloading.dependency.support.MockFilteredResourceVisitor;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.dependency.support.c.C;

/**
 * MockResourceVisitorUnitTestCase.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class MockResourceVisitorUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   private static String[] paths = new String[]
   {
      ClassLoaderUtils.packageNameToPath(A.class.getName()),
      ClassLoaderUtils.packageNameToPath(B.class.getName()),
      ClassLoaderUtils.packageNameToPath(C.class.getName()),
   };

   private static String[] classes = new String[]
   {
      ClassLoaderUtils.classNameToPath(A.class),
      ClassLoaderUtils.classNameToPath(B.class),
      ClassLoaderUtils.classNameToPath(C.class),
   };

   public static Test suite()
   {
      return suite(MockResourceVisitorUnitTestCase.class);
   }

   public MockResourceVisitorUnitTestCase(String name)
   {
      super(name);
   }

   protected MockClassLoadingMetaData createClassLoadingMetaData(String name)
   {
      MockClassLoadingMetaData clmd = new MockClassLoadingMetaData(name);
      clmd.setPaths(paths);
      return clmd;
   }

   public void testIncluded() throws Exception
   {
      MockClassLoadingMetaData a = createClassLoadingMetaData("a");
      a.setIncluded(new PackageClassFilter(new String[]{A.class.getPackage().getName(), B.class.getPackage().getName()}));
      testMockClassLoadingMetaData(a);
   }

   public void testExcluded() throws Exception
   {
      MockClassLoadingMetaData a = createClassLoadingMetaData("a");
      a.setExcluded(new PackageClassFilter(new String[]{C.class.getPackage().getName()}));
      testMockClassLoadingMetaData(a);
   }

   public void testFiltered() throws Exception
   {
      MockClassLoadingMetaData a = createClassLoadingMetaData("a");
      testMockClassLoadingMetaData(a, new MockFilteredResourceVisitor());
   }

   protected void testMockClassLoadingMetaData(MockClassLoadingMetaData a) throws Exception
   {
      testMockClassLoadingMetaData(a, new MockResourceVisitor());
   }

   protected void testMockClassLoadingMetaData(MockClassLoadingMetaData a, MockResourceVisitor visitor) throws Exception
   {
      KernelControllerContext contextA = install(a);
      try
      {
         MockClassLoaderPolicyModule module = assertModule(contextA);
         module.registerClassLoaderPolicy(system);

         module.visit(visitor);

         Set<String> resources = new HashSet<String>(Arrays.asList(classes));
         resources.remove(ClassLoaderUtils.classNameToPath(C.class));
         assertEquals(resources, visitor.getResources());
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoModule(contextA);
   }
}