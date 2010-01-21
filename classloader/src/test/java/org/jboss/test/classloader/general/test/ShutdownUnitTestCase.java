/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloader.general.test;

import java.util.Collections;

import junit.framework.Test;

import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.test.classloader.AbstractClassLoaderTest;
import org.jboss.test.classloader.general.support.A;

/**
 * ShutdownUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ShutdownUnitTestCase extends AbstractClassLoaderTest
{
   public static Test suite()
   {
      return suite(ShutdownUnitTestCase.class);
   }

   public ShutdownUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testSimpleShutdown() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);
      
      system.unregisterClassLoader(clA);
      // This is actually correct, we can load from ourselves after shutdown
      assertLoadClass(A.class, clA); 
      assertGetResource(A.class, clA);
   }
   
   public void testShutdownImportAll() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);

      MockClassLoaderPolicy policyB = createMockClassLoaderPolicy("b");
      policyB.setImportAll(true);
      ClassLoader clB = system.registerClassLoaderPolicy(policyB);
      assertLoadClass(A.class, clB, clA);
      assertGetResource(A.class, clB);
      
      system.unregisterClassLoader(clA);
      assertLoadClassFail(A.class, clB);
      assertGetResourceFail(A.class, clB);
   }
   
   public void testShutdownImport() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);

      MockClassLoaderPolicy policyB = createMockClassLoaderPolicy("b");
      policyB.setDelegates(Collections.singletonList(new DelegateLoader(policyA)));
      ClassLoader clB = system.registerClassLoaderPolicy(policyB);
      assertLoadClass(A.class, clB, clA);
      assertGetResource(A.class, clB);
      
      system.unregisterClassLoader(clA);
      assertLoadClassFail(A.class, clB);
      assertGetResourceFail(A.class, clB);
   }
   
   public void testLazyShutdown() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      policyA.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);
      
      system.unregisterClassLoader(clA);
      assertLoadClass(A.class, clA); 
      assertGetResource(A.class, clA);
   }
   
   public void testLazyImportAll() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      policyA.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);

      MockClassLoaderPolicy policyB = createMockClassLoaderPolicy("b");
      policyB.setImportAll(true);
      ClassLoader clB = system.registerClassLoaderPolicy(policyB);
      assertLoadClass(A.class, clB, clA);
      assertGetResource(A.class, clB);
      
      system.unregisterClassLoader(clA);
      assertLoadClassFail(A.class, clB);
      assertGetResourceFail(A.class, clB);
   }
   
   public void testLazyImport() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      MockClassLoaderPolicy policyA = createMockClassLoaderPolicy("a");
      policyA.setPathsAndPackageNames(A.class);
      policyA.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      ClassLoader clA = system.registerClassLoaderPolicy(policyA);
      assertLoadClass(A.class, clA);
      assertGetResource(A.class, clA);

      MockClassLoaderPolicy policyB = createMockClassLoaderPolicy("b");
      policyB.setDelegates(Collections.singletonList(new DelegateLoader(policyA)));
      ClassLoader clB = system.registerClassLoaderPolicy(policyB);
      assertLoadClass(A.class, clB, clA);
      assertGetResource(A.class, clB);
      
      system.unregisterClassLoader(clA);
      assertLoadClass(A.class, clB, clA);
      assertGetResource(A.class, clB);
   }
}
