/*
* JBoss, Home of Professional Open Source
* Copyright 2010, JBoss Inc., and individual contributors as indicated
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
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.test.BaseTestCase;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.automount.Automounter;

/**
 * ShutdownUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ShutdownUnitTestCase extends BaseTestCase
{
   
   private VirtualFile signedJar;
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      URL signedJarURL = getResource("/classloader/signedjar");
      VirtualFile signedJarRoot = VFS.getChild(signedJarURL);
      signedJar = signedJarRoot.getChild("wstx.jar");
      Automounter.mount(signedJar);
   }

   @Override
   protected void tearDown() throws Exception
   {
      Automounter.cleanup(signedJar);
      super.tearDown();
   }

   public void testShutdownUnregisterDefault() throws Exception
   {
      VFSClassLoaderPolicy policy = getClassLoaderPolicy();;
      testShutdown(policy, true);
   }

   public void testShutdownUnregister() throws Exception
   {
      VFSClassLoaderPolicy policy = getClassLoaderPolicy();;
      policy.setShutdownPolicy(ShutdownPolicy.UNREGISTER);
      testShutdown(policy, true);
   }

   public void testShutdownGC() throws Exception
   {
      VFSClassLoaderPolicy policy = getClassLoaderPolicy();;
      policy.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      testShutdown(policy, false);
   }

   protected VFSClassLoaderPolicy getClassLoaderPolicy() throws Exception
   {
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(signedJar);
      return policy;
   }
   
   protected void testShutdown(VFSClassLoaderPolicy policy, boolean shutdownAtUnregister) throws Exception
   {
      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      
      Class<?> clazz = classLoader.loadClass("org.codehaus.stax2.validation.XMLValidator");
      assertEquals(classLoader, clazz.getClassLoader());
      
      MockClassLoaderPolicy mock = new MockClassLoaderPolicy();
      mock.setDelegates(Collections.singletonList(new DelegateLoader(policy)));
      ClassLoader mockCl = system.registerClassLoaderPolicy(mock);
      
      clazz = mockCl.loadClass("org.codehaus.stax2.validation.XMLValidator");
      assertEquals(classLoader, clazz.getClassLoader());
      
      system.unregisterClassLoader(classLoader);
      try
      {
         clazz = mockCl.loadClass("org.codehaus.stax2.validation.XMLValidator");
         if (shutdownAtUnregister)
            fail("Should not be here: " + ClassLoaderUtils.classToString(clazz));
      }
      catch (ClassNotFoundException e)
      {
         assertTrue("Didn't expect: " + e, shutdownAtUnregister);
      }
   }

   public static Test suite()
   {
      return new TestSuite(ShutdownUnitTestCase.class);
   }

   public ShutdownUnitTestCase(String name) throws Throwable
   {
      super(name);
   }
}
