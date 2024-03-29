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
package org.jboss.test.classloading;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.jboss.test.classloading.vfs.client.test.ClientClassPathUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.DomainUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.GeneratedClassesUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.ImportAllUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.ManagedObjectVFSClassLoaderFactoryUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.ModuleDependencyUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.PackageDependencyUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.ReExportModuleUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.ReExportPackageUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.UsesPackageUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.VFSClassLoaderFactoryUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.test.VFSResourceVisitorUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.xml.test.VFSClassLoaderFactoryXMLUnitTestCase;
import org.jboss.test.classloading.vfs.metadata.xml.test.NoopClassLoaderFactoryXMLUnitTestCase;
import org.jboss.test.classloading.vfs.policy.test.ExportAllUnitTestCase;
import org.jboss.test.classloading.vfs.policy.test.PackageInfoUnitTestCase;
import org.jboss.test.classloading.vfs.policy.test.ShutdownUnitTestCase;
import org.jboss.test.classloading.vfs.policy.test.VFSCLPolicySignedCertsUnitTestCase;

/**
 * ClassLoading VFS Test Suite.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 37459 $
 */
public class ClassLoadingVFSTestSuite extends TestSuite
{
   /**
    * For running the testsuite from the command line
    * 
    * @param args the command line args
    */
   public static void main(String[] args)
   {
      TestRunner.run(suite());
   }

   /**
    * Create the testsuite
    * 
    * @return the testsuite
    */
   public static Test suite()
   {
      TestSuite suite = new TestSuite("ClassLoading VFS Tests");

      suite.addTest(ExportAllUnitTestCase.suite());
      suite.addTest(VFSClassLoaderFactoryXMLUnitTestCase.suite());
      suite.addTest(NoopClassLoaderFactoryXMLUnitTestCase.suite());
      suite.addTest(DomainUnitTestCase.suite());
      suite.addTest(ImportAllUnitTestCase.suite());
      suite.addTest(ModuleDependencyUnitTestCase.suite());
      suite.addTest(PackageDependencyUnitTestCase.suite());
      suite.addTest(VFSClassLoaderFactoryUnitTestCase.suite());
      suite.addTest(ManagedObjectVFSClassLoaderFactoryUnitTestCase.suite());
      suite.addTest(ReExportModuleUnitTestCase.suite());
      suite.addTest(ReExportPackageUnitTestCase.suite());
      suite.addTest(UsesPackageUnitTestCase.suite());
      suite.addTest(VFSResourceVisitorUnitTestCase.suite());
      suite.addTest(ClientClassPathUnitTestCase.suite());
      suite.addTest(PackageInfoUnitTestCase.suite());
      suite.addTest(GeneratedClassesUnitTestCase.suite());
      suite.addTest(VFSCLPolicySignedCertsUnitTestCase.suite());
      suite.addTest(ShutdownUnitTestCase.suite());

      return suite;
   }
}
