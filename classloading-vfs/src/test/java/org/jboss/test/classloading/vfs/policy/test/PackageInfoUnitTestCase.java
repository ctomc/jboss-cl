/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.test.classloading.vfs.policy.test;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.classloader.spi.PackageInformation;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.test.BaseTestCase;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Package related tests of VFSClassLoaderPolicy
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class PackageInfoUnitTestCase extends BaseTestCase
{

   public PackageInfoUnitTestCase(String name)
   {
      super(name);
   }

   public void testCorrectPackage()
      throws Exception
   {
      URL testear1xURL = getResource("/classloader/testear1x.ear");
      VirtualFile testear1x = VFS.getRoot(testear1xURL);
      VirtualFile jar1 = testear1x.getChild("lib/jar1.jar");
      assertNotNull(jar1);
      VirtualFile jar2 = testear1x.getChild("lib/jar2.jar");
      assertNotNull(jar2);
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy("testCorrectPackage", testear1x, jar2, jar1);
      policy.setExportAll(ExportAll.NON_EMPTY);
      policy.setImportAll(true);

      PackageInformation utilInfo = policy.getClassPackageInformation("util.Shared", "util");
      /*
      Specification-Title: testear1x.ear/lib/jar1.jar
      Specification-Version: 1.0.1.GA
      Specification-Vendor: JBoss
      Implementation-Title: JBoss [division of RedHat]
      Implementation-URL: http://www.jboss.org/
      Implementation-Version: 1.0.1.GA 
      Implementation-Vendor: JBoss.org
      Implementation-Vendor-Id: http://www.jboss.org/
       */
      assertEquals("testear1x.ear/lib/jar1.jar", utilInfo.specTitle);
      assertEquals("1.0.1.GA", utilInfo.specVersion);
   }

   public static Test suite()
   {
      return new TestSuite(PackageInfoUnitTestCase.class);
   }
}
