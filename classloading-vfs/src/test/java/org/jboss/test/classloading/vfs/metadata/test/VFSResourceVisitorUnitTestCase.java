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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import junit.framework.Test;
import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.plugins.visitor.FederatedResourceVisitor;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.vfs.metadata.VFSClassLoaderFactory;
import org.jboss.classloading.spi.visitor.ClassVisitor;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.test.classloading.vfs.metadata.VFSClassLoadingMicrocontainerTest;
import org.jboss.test.classloading.vfs.metadata.support.a.A;
import org.jboss.test.classloading.vfs.metadata.support.b.B;
import org.jboss.test.classloading.vfs.metadata.support.c.C;

/**
 * VFSResourceVisitorUnitTestCase.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class VFSResourceVisitorUnitTestCase extends VFSClassLoadingMicrocontainerTest
{
   private static Map<String, Class<?>> aliases;

   static
   {
      aliases = new HashMap<String, Class<?>>();
      aliases.put("a/A.class", A.class);
      aliases.put("b/B.class", B.class);
      aliases.put("c/C.class", C.class);
   }

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
      KernelDeployment deployment = install(factory);
      try
      {
         visitModule();
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testBytes() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(System.getProperty("test.dir") + "/support/"));
      KernelDeployment deployment = install(factory);
      try
      {
         final Map<String, byte[]> bytes = new HashMap<String,byte[]>();
         ResourceVisitor visitor = new ClassVisitor()
         {
            public void visit(ResourceContext resource)
            {
               try
               {
                  bytes.put(resource.getResourceName(), resource.getBytes());
               }
               catch (IOException e)
               {
                  throw new Error(e);
               }
            }
         };
         Module module = assertModule("test:0.0.0");
         module.visit(visitor);

         assertFalse(bytes.isEmpty());
         for (Map.Entry<String, byte[]> entry : bytes.entrySet())
         {
            Class<?> clazz = aliases.get(entry.getKey());
            assertNotNull(clazz);
            URL url = new URL(getRoot(clazz) + ClassLoaderUtils.classNameToPath(clazz));
            InputStream in = url.openStream();
            try
            {
               byte[] classBytes = ClassLoaderUtils.loadBytes(in);
               byte[] value = entry.getValue();
               assertTrue(Arrays.equals(classBytes, value));
            }
            finally
            {
               in.close();
            }
         }
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testClassloading() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(getRoot(getClass())));
      KernelDeployment deployment = install(factory);
      try
      {
         ResourceVisitor visitor = new ResourceVisitor()
         {
            public ResourceFilter getFilter()
            {
               return new ResourceFilter()
               {
                  public boolean accepts(ResourceContext resource)
                  {
                     return resource.isClass() && resource.getResourceName().contains("C.class");
                  }
               };
            }

            public void visit(ResourceContext resource)
            {
               Class<?> clazz = resource.loadClass();
               assertEquals(C.class.getName(), clazz.getName());
            }
         };
         Module module = assertModule("test:0.0.0");
         module.visit(visitor);
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testRecurseFilter() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(System.getProperty("test.dir") + "/support/"));
      KernelDeployment deployment = install(factory);
      try
      {
         final Set<String> classes = new HashSet<String>();
         ResourceVisitor visitor = new ClassVisitor()
         {
            public void visit(ResourceContext resource)
            {
               classes.add(resource.getResourceName());
            }
         };
         ResourceFilter recurseFilter = new ResourceFilter()
         {
            public boolean accepts(ResourceContext resource)
            {
               return "a".equals(resource.getResourceName());
            }
         };

         Module module = assertModule("test:0.0.0");
         module.visit(visitor, visitor.getFilter(), recurseFilter);

         assertEquals(1, classes.size());
         assertEquals(aliases.get(classes.iterator().next()), A.class);
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testRecurseFilterFromTop() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(getRoot(getClass())));
      KernelDeployment deployment = install(factory);
      try
      {
         final Set<String> classes = new HashSet<String>();
         ResourceVisitor visitor = new ResourceVisitor()
         {
            public ResourceFilter getFilter()
            {
               return new ResourceFilter()
               {
                  public boolean accepts(ResourceContext resource)
                  {
                     return resource.getResourceName().contains("support");
                  }
               };
            }

            public void visit(ResourceContext resource)
            {
               classes.add(resource.getClassName());
            }
         };
         final String pathA = ClassLoaderUtils.packageNameToPath(A.class.getName());
         final int pathAlength = pathA.length();
         ResourceFilter recurseFilter = new ResourceFilter()
         {
            public boolean accepts(ResourceContext resource)
            {
               String resourceName = resource.getResourceName();
               int min = Math.min(resourceName.length(), pathAlength);
               return pathA.substring(0, min).equals(resourceName.substring(0, min));
            }
         };

         Module module = assertModule("test:0.0.0");
         module.visit(visitor, visitor.getFilter(), recurseFilter);

         assertEquals(1, classes.size());
         assertEquals(classes.iterator().next(), A.class.getName());
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testUrlsParameter() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(
            Arrays.asList(
                  System.getProperty("test.dir") + "/support/a",
                  System.getProperty("test.dir") + "/support/b",
                  System.getProperty("test.dir") + "/support/c"
            )
      );
      KernelDeployment deployment = install(factory);
      try
      {
         final Set<String> classes = new HashSet<String>();
         ResourceVisitor visitor = new ClassVisitor()
         {
            public void visit(ResourceContext resource)
            {
               classes.add(resource.getResourceName());
            }
         };

         URL aURL = new URL("vfs" + System.getProperty("test.dir") + "/support/a/");
         Module module = assertModule("test:0.0.0");
         module.visit(visitor, visitor.getFilter(), null, aURL);

         assertEquals(1, classes.size());
         assertEquals(classes.iterator().next(), A.class.getSimpleName() + ".class");
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testUrlsParameters() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(
            Arrays.asList(
                  System.getProperty("test.dir") + "/support/a",
                  System.getProperty("test.dir") + "/support/b",
                  System.getProperty("test.dir") + "/support/c"
            )
      );
      KernelDeployment deployment = install(factory);
      try
      {
         final Set<String> classes = new TreeSet<String>();
         ResourceVisitor visitor = new ClassVisitor()
         {
            public void visit(ResourceContext resource)
            {
               classes.add(resource.getResourceName());
            }
         };

         URL aURL = new URL("vfs" + System.getProperty("test.dir") + "/support/a/");
         URL bURL = new URL("vfs" + System.getProperty("test.dir") + "/support/b/");
         Module module = assertModule("test:0.0.0");
         module.visit(visitor, visitor.getFilter(), null, aURL, bURL);

         assertEquals(2, classes.size());
         Iterator<String> iterator = classes.iterator();
         assertEquals(iterator.next(), A.class.getSimpleName() + ".class");
         assertEquals(iterator.next(), B.class.getSimpleName() + ".class");
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testFederated() throws Exception
   {
      VFSClassLoaderFactory factory = new VFSClassLoaderFactory("test");
      factory.setRoots(Arrays.asList(System.getProperty("test.dir") + "/support/"));
      KernelDeployment deployment = install(factory);
      try
      {
         final List<String> classes = new ArrayList<String>();
         ResourceVisitor visitor = new ClassVisitor()
         {
            public void visit(ResourceContext resource)
            {
               classes.add(resource.getResourceName());
            }
         };
         ResourceFilter rfA = new ResourceFilter()
         {
            public boolean accepts(ResourceContext resource)
            {
               return "a".equals(resource.getResourceName());
            }
         };
         ResourceFilter rfB = new ResourceFilter()
         {
            public boolean accepts(ResourceContext resource)
            {
               return "b".equals(resource.getResourceName());
            }
         };
         FederatedResourceVisitor fedRV = new FederatedResourceVisitor(
               new ResourceVisitor[]{visitor, visitor},
               null,
               new ResourceFilter[]{rfA, rfB}
         );

         Module module = assertModule("test:0.0.0");
         module.visit(fedRV, fedRV.getFilter(), fedRV.getRecurseFilter());

         assertEquals(2, classes.size());
         assertTrue(classes.contains("a/A.class"));
         assertTrue(classes.contains("b/B.class"));
      }
      finally
      {
         undeploy(deployment);
      }
   }

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
      assertEquals(aliases.keySet(), set);
   }
}