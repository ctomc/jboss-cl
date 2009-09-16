/*
* JBoss, Home of Professional Open Source
* Copyright 2007, JBoss Inc., and individual contributors as indicated
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
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import junit.framework.Test;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.test.BaseTestCase;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Unit test JBCL-67
 * VFSClassLoaderPolicy and certificates
 *
 * @author <a href="anil.saldhana@jboss.org">Anil Saldhana</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class VFSCLPolicySignedCertsUnitTestCase extends BaseTestCase
{
   public VFSCLPolicySignedCertsUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(VFSCLPolicySignedCertsUnitTestCase.class);
   }

   /**
    * Ensure that wstx.jar that is loaded by the base class loader
    * is able to inject the certs into the protection domain held
    * by the codesource of the class
    *
    * @throws Exception for any error
    */
   public void testCertificates() throws Exception
   {
      URL signedJarURL = getResource("/classloader/signedjar");
      VirtualFile signedJarRoot = VFS.getRoot(signedJarURL);
      VirtualFile signedJar = signedJarRoot.getChild("wstx.jar");
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(signedJar);
      policy.setExportAll(ExportAll.ALL);

      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);

      Class<?> clazz = classLoader.loadClass("org.codehaus.stax2.validation.XMLValidator");
      assertNotNull(clazz);
      ProtectionDomain pd = clazz.getProtectionDomain();
      assertNotNull("Protection Domain is null: " + clazz , pd);
      Certificate[] certs = pd.getCodeSource().getCertificates();
      assertNotNull("Certs are null: " + pd, certs);
      assertTrue("Certs are empty.", certs.length > 0);
      //RH, thawte, thawte root CA
      assertEquals("Should be 3 certs.", 3, certs.length);
   }
}