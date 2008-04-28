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

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.vfs.metadata.VFSClassLoaderFactory;
import org.jboss.classloading.spi.visitor.ClassVisitor;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.test.classloading.vfs.metadata.VFSClassLoadingMicrocontainerTest;

/**
 * VFSResourceVisitorUnitTestCase.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class VFSResourceVisitorUnitTestCase extends VFSClassLoadingMicrocontainerTest
{
   private static String[] paths = new String[] {"a/A.class", "b/B.class", "c/C.class"};

   public static Test suite()
   {
      return suite(VFSResourceVisitorUnitTestCase.class);
   }

   public VFSResourceVisitorUnitTestCase(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {
      super.setUp();

      URL url = getClass().getResource("VFSResourceVisitorUnitTestCase.class");
      String urlString = url.toExternalForm();
      String end = "/test/VFSResourceVisitorUnitTestCase.class";
      int length = urlString.length() - end.length();
      urlString = urlString.substring(0, length);
      System.setProperty("test.dir", urlString);
   }

   public void testFromXml() throws Exception
   {
      KernelDeployment deployment = deploy("VFSResourceVisitorTest.xml");
      try
      {
         validate();
         visitModule();
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testFromAPI() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(System.getProperty("test.dir") + "/support/"));
      install(factory);
      try
      {
         visitModule();
      }
      finally
      {
         shutdown();
      }
   }

   // TODO - test inputstream / bytes

   protected void visitModule()
   {
      Module module = assertModule("test:0.0.0");
      final Set<String> set = new HashSet<String>();
      ResourceVisitor visitor = new ClassVisitor()
      {
         public void visit(ResourceContext resource)
         {
            set.add(resource.getResourceName());
         }
      };
      module.visit(visitor);
      assertEquals(new HashSet<String>(Arrays.asList(paths)), set);
   }
}