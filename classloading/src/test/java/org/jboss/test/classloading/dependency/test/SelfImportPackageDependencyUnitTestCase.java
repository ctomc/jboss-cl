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

/**
 * Test a module that imports and exports the same package.
 * 
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class SelfImportPackageDependencyUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   public static Test suite()
   {
      return suite(SelfImportPackageDependencyUnitTestCase.class);
   }

   public SelfImportPackageDependencyUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testImportPackageNoVersionCheck() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a");
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      a.getCapabilities().addCapability(factory.createModule("ModuleA"));
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      a.getRequirements().addRequirement(factory.createRequirePackage(A.class.getPackage().getName()));
      a.setPathsAndPackageNames(A.class);
      
      KernelControllerContext contextA = install(a);
      try
      {
         ClassLoader clA = assertClassLoader(contextA);
         assertLoadClass(A.class, clA);
      }
      finally
      {
         uninstall(contextA);
      }
      assertNoClassLoader(contextA);
   }
}
