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
package org.jboss.test.classloading.vfs.client.test;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import junit.framework.Test;

import org.jboss.test.classloading.vfs.client.support.launcher.ClientLauncher;
import org.jboss.test.classloading.vfs.metadata.VFSClassLoadingMicrocontainerTest;

/**
 * Tests of vfs class loading that affect a client application type of env.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ClientClassPathUnitTestCase extends VFSClassLoadingMicrocontainerTest
{
   public static Test suite()
   {
      return suite(ClientClassPathUnitTestCase.class);
   }

   public ClientClassPathUnitTestCase(String name)
   {
      super(name);
   }
/*
   public void testClientVFSClassLoaderFactory()
      throws Exception
   {
      URL clientJar = super.getResource("/org/jboss/test/classloading/vfs/client.jar");
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("ClientLauncher");
      ClassLoadingMetaDataFactory cfactory = ClassLoadingMetaDataFactory.getInstance();
      String clientClassName = "org.jboss.test.classloading.vfs.client.support.main.Client";
      //factory.getCapabilities().addCapability(cfactory.createPackage("org.jboss.test.classloading.vfs.client.support.main"));
      factory.getRoots().add(clientJar.toString());
      // This would be handled by the structure deployer
      URL mfURL = super.getResource("/org/jboss/test/classloading/vfs/client.jar/META-INF/MANIFEST.MF");
      InputStream mfIS = mfURL.openStream();
      Manifest mf = new Manifest(mfIS);
      mfIS.close();
      String pathValue = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
      String[] paths = pathValue.split(" ");
      for(String path : paths)
      {
         URL pathURL = new URL(clientJar, path);
         factory.getRoots().add(pathURL.toString());
      }
      getLog().debug("VFSClassLoaderFactory.roots : "+factory.getRoots());

//      factory.setIncludedPackages("org.jboss.test.classloading.vfs.client.support.main");
      KernelDeployment depA = install(factory);
      try
      {
         ClassLoader clA = assertClassLoader(factory);
         assertLoadClass(clientClassName, clA);
         assertLoadClassFail("org.jboss.test.classloading.vfs.client.support.launcher.ClientLauncher", clA);
      }
      finally
      {
         undeploy(depA);
      }
      assertNoClassLoader(factory);
   }
*/
   /**
    * Test an application client launcher mock up that uses the mc, vfs,
    * class loaders to launch the application client environment and call its
    * main method.
    * @throws Throwable
    */
   public void testClientMainClassPath()
      throws Throwable
   {
      URL clientJar = super.getResource("/org/jboss/test/classloading/vfs/client.jar/");
      // This would be handled by the structure deployer
      URL mfURL = new URL(clientJar, "META-INF/MANIFEST.MF");
      InputStream mfIS = mfURL.openStream();
      Manifest mf = new Manifest(mfIS);
      mfIS.close();
      String pathValue = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
      String[] paths = pathValue.split(" ");
      ArrayList<String> pathList = new ArrayList<String>();
      pathList.add(clientJar.toString());
      for(String path : paths)
      {
         URL pathURL = new URL(clientJar, "../" + path);
         pathList.add(pathURL.toString());
      }

      String[] clientCP = new String[pathList.size()];
      pathList.toArray(clientCP);
      String[] args = {clientJar.toString()};
      String clientClassName = "org.jboss.test.classloading.vfs.client.support.main.Client";
      ClientLauncher.launch(clientClassName, "testClientMainClassPath", clientCP, args);
      if(ClientLauncher.getException() != null)
      {
         Exception ex = ClientLauncher.getException();
         getLog().error("ClientLauncher.exception: ", ex);
         fail("ClientLauncher saw an exception, "+ex);
      }
   }
}
