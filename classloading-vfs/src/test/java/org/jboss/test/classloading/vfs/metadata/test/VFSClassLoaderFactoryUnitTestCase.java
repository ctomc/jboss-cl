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
package org.jboss.test.classloading.vfs.metadata.test;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.test.classloading.vfs.metadata.VFSClassLoadingMicrocontainerTest;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;

import junit.framework.Test;

/**
 * VFSClassLoaderFactoryUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VFSClassLoaderFactoryUnitTestCase extends VFSClassLoadingMicrocontainerTest
{
   public static Test suite()
   {
      return suite(VFSClassLoaderFactoryUnitTestCase.class);
   }

   public VFSClassLoaderFactoryUnitTestCase(String name)
   {
      super(name);
   }

   public void testSmoke() throws Exception
   {
      KernelDeployment deployment = deploy("SmokeTest.xml");
      try
      {
         validate();
         assertClassLoader("test", "0.0.0");
      }
      finally
      {
         undeploy(deployment);
      }
      validate();
      assertNoClassLoader("test", "0.0.0");
   }
   
   public void testADependsUponBCorrectWay() throws Exception
   {
      KernelDeployment a = deploy("A1.0.0.xml");
      try
      {
         validate();
         assertClassLoader("a", "1.0.0");
         
         KernelDeployment b = deploy("B1.0.0.xml");
         try
         {
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
         }
         finally
         {
            undeploy(b);
         }
         validate();
         assertNoClassLoader("b", "1.0.0");
         assertClassLoader("a", "1.0.0");
      }
      finally
      {
         undeploy(a);
      }
      validate();
      assertNoClassLoader("a", "1.0.0");
   }
   
   public void testADependsUponBWrongWay() throws Exception
   {
      KernelDeployment b = deploy("B1.0.0.xml");
      try
      {
         assertNoClassLoader("b", "1.0.0");
         
         KernelDeployment a = deploy("A1.0.0.xml");
         try
         {
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
         }
         finally
         {
            undeploy(a);
         }
         assertNoClassLoader("a", "1.0.0");
         assertNoClassLoader("b", "1.0.0");
      }
      finally
      {
         undeploy(b);
      }
      validate();
   }
   
   public void testADependsUponBRedeployA() throws Exception
   {
      KernelDeployment a = deploy("A1.0.0.xml");
      try
      {
         validate();
         assertClassLoader("a", "1.0.0");
         
         KernelDeployment b = deploy("B1.0.0.xml");
         try
         {
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
            
            undeploy(a);
            try
            {
               assertNoClassLoader("a", "1.0.0");
               assertNoClassLoader("b", "1.0.0");
            }
            finally
            {
              a = deploy("A1.0.0.xml");
            }
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
         }
         finally
         {
            undeploy(b);
         }
         validate();
         assertNoClassLoader("b", "1.0.0");
         assertClassLoader("a", "1.0.0");
      }
      finally
      {
         undeploy(a);
      }
      validate();
      assertNoClassLoader("a", "1.0.0");
   }
   
   public void testADependsUponBRedeployB() throws Exception
   {
      KernelDeployment a = deploy("A1.0.0.xml");
      try
      {
         validate();
         assertClassLoader("a", "1.0.0");
         
         KernelDeployment b = deploy("B1.0.0.xml");
         try
         {
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
            
            undeploy(b);
            try
            {
               assertClassLoader("a", "1.0.0");
               assertNoClassLoader("b", "1.0.0");
            }
            finally
            {
              b = deploy("B1.0.0.xml");
            }
            validate();
            assertClassLoader("a", "1.0.0");
            assertClassLoader("b", "1.0.0");
         }
         finally
         {
            undeploy(b);
         }
         validate();
         assertNoClassLoader("b", "1.0.0");
         assertClassLoader("a", "1.0.0");
      }
      finally
      {
         undeploy(a);
      }
      validate();
      assertNoClassLoader("a", "1.0.0");
   }

   public void testWildcard() throws Exception
   {
      String testDir = "test";
      List<Closeable> closeables = new ArrayList<Closeable>();
      try
      {
         VirtualFile root = VFS.getRootVirtualFile();
         VirtualFile test = root.getChild(testDir);
         closeables.add(VFS.mountAssembly(new VirtualFileAssembly(), test));
         VirtualFile jar1 = test.getChild("j1.jar");
         closeables.add(VFS.mountAssembly(new VirtualFileAssembly(), jar1));
         VirtualFile jar2 = test.getChild("j2.jar");
         closeables.add(VFS.mountAssembly(new VirtualFileAssembly(), jar2));

         System.setProperty("test.dir", testDir);
         try
         {
            KernelDeployment wc = deploy("Wildcard.xml");
            try
            {
               validate();
               ClassLoader cl = assertClassLoader("wc", "0.0.0");
               assertLoadClassFail("org.acme.FooBar", cl);
            }
            finally
            {
               undeploy(wc);
            }
         }
         finally
         {
            System.clearProperty("test.dir");
         }
      }
      finally
      {
         VFSUtils.safeClose(closeables);
      }
   }
}
