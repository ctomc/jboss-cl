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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.metadata.VFSClassLoaderFactory;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.test.classloading.vfs.metadata.VFSClassLoadingMicrocontainerTest;
import org.jboss.test.classloading.vfs.metadata.support.a.A;
import org.jboss.test.classloading.vfs.metadata.support.b.B;
import org.jboss.util.id.GUID;
import org.jboss.virtual.MemoryFileFactory;
import org.jboss.virtual.VFS;
import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;

/**
 * DomainUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GeneratedClassesUnitTestCase extends VFSClassLoadingMicrocontainerTest
{
   final static GeneratedClassInfo NEW_PACKAGE  = new GeneratedClassInfo("newpackage.GeneratedClass");
   final static GeneratedClassInfo OTHER_PACKAGE  = new GeneratedClassInfo("otherpackage.GeneratedClass");
   final static GeneratedClassInfo EXISTING_PACKAGE  = new GeneratedClassInfo("org.jboss.test.classloading.vfs.metadata.support.a.GeneratedClass");
   static
   {
      VFS.init();
   }
   
   public static Test suite()
   {
      return suite(GeneratedClassesUnitTestCase.class);
   }

   public GeneratedClassesUnitTestCase(String name)
   {
      super(name);
   }

   public void testImportAllGenerateClassInExistingPackage() throws Exception
   {
      runImportAllGenerateClass(EXISTING_PACKAGE, true);
   }
   
   public void testImportAllGenerateClassInGlobalIncludedPackage() throws Exception
   {
      runImportAllGenerateClass(NEW_PACKAGE, true);
   }
   
   public void testImportAllGenerateClassInOtherPackage() throws Exception
   {
      runImportAllGenerateClass(OTHER_PACKAGE, false);
   }
   
   private void runImportAllGenerateClass(GeneratedClassInfo info, boolean expectSuccess) throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      String dynamicClassRoot = getDynamicClassRoot();
      VFSClassLoaderFactory a = new VFSClassLoaderFactory("a");
      a.setImportAll(true);
      a.getRoots().add(getRoot(A.class));
      a.getRoots().add(dynamicClassRoot);
      a.getCapabilities().addCapability(factory.createPackage(A.class.getPackage().getName()));
      KernelDeployment depA = install(a);

      VFSClassLoaderFactory b = new VFSClassLoaderFactory("b");
      b.setExportAll(ExportAll.NON_EMPTY);
      b.setImportAll(true);
      b.getRoots().add(getRoot(B.class));
      b.getCapabilities().addCapability(factory.createPackage(B.class.getPackage().getName()));
      KernelDeployment depB = install(b);
      try
      {
         ClassLoader clA = assertClassLoader(a);
         ClassLoader clB = assertClassLoader(b);
         assertLoadClass(A.class, clA);
         assertLoadClass(B.class, clB);
         assertLoadClass(A.class, clB, clA);
         assertLoadClass(B.class, clA, clB);
         
         Class<?> clazz = generateClass(clA, dynamicClassRoot, info);
         Class<?> clazzA = assertLoadClass(info.getClassname(), clA);
         assertSame(clazz, clazzA);
         
         try
         {
            Class<?> clazzB = assertLoadClass(info.getClassname(), clB, clA);
            
            if (!expectSuccess)
            {
               fail("Should not have been able to load " + info.getClassname());
            }
            assertSame(clazz, clazzB);
         }
         catch(Throwable t)
         {
            if (expectSuccess)
            {
               fail("Should have been able to load class" + info.getClassname() + " " + t);
            }
         }
      }
      finally
      {
         undeploy(depB);
         undeploy(depA);
      }
   }

   private Class<?> generateClass(ClassLoader loader, String dynamicClassRoot, GeneratedClassInfo info) throws Exception
   {
      URL outputURL = new URL(dynamicClassRoot + "/" + info.getResourceName());
      MemoryContextFactory factory = MemoryContextFactory.getInstance();
      factory.putFile(outputURL, info.getClassBytes());
      
      if (loader instanceof RealClassLoader)
      {
         ((RealClassLoader)loader).clearBlackList(info.getResourceName());
      }
      return loader.loadClass(info.getClassname());
   }
   
   private String getDynamicClassRoot() throws Exception
   {
      URL dynamicClassRoot = new URL("vfsmemory", GUID.asString(), "");
      MemoryFileFactory.createRoot(dynamicClassRoot).getRoot();
      return dynamicClassRoot.toString();
   }

   private static class GeneratedClassInfo
   {
      String classname;
      String resourceName;
      byte[] classBytes;
      
      public GeneratedClassInfo(String classname)
      {
         this.classname = classname;
         resourceName = ClassLoaderUtils.classNameToPath(classname);
         loadClassBytes();
      }
      
      public String getClassname()
      {
         return classname;
      }
      
      public String getPackageName()
      {
         return ClassLoaderUtils.getClassPackageName(classname);
      }

      public String getResourceName()
      {
         return resourceName;
      }

      public byte[] getClassBytes()
      {
         return classBytes;
      }

      private void loadClassBytes()
      {
         InputStream in = this.getClass().getClassLoader().getResourceAsStream("classes/" + resourceName);
         
         assertNotNull("Could not find inputstream for " + resourceName, in);
         try
         {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int i = in.read();
            while(i != -1)
            {
               out.write((byte)i);
               i = in.read();
            }
            classBytes = out.toByteArray();
         }
         catch(Exception e)
         {
            throw new RuntimeException(e);
         }
         finally
         {
            if (in != null)
            {
               try
               {
                  in.close();
               }
               catch (IOException e)
               {
                  // AutoGenerated
                  throw new RuntimeException(e);
               }
            }
         }
      }
   }
}
