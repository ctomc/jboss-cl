/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloader.notifications.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.plugins.loader.ClassLoaderToLoaderAdapter;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ClassNotFoundEvent;
import org.jboss.classloader.spi.ClassNotFoundHandler;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.test.classloader.AbstractClassLoaderTestWithSecurity;
import org.jboss.test.classloader.notifications.support.a.A;

/**
 * ClassNotFoundHnadlerUnitTestCase
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassNotFoundHandlerUnitTestCase extends AbstractClassLoaderTestWithSecurity implements ClassNotFoundHandler
{
   public static Test suite()
   {
      return suite(ClassNotFoundHandlerUnitTestCase.class);
   }

   public ClassNotFoundHandlerUnitTestCase(String name)
   {
      super(name);
   }
   
   List<ClassNotFoundEvent> events = new CopyOnWriteArrayList<ClassNotFoundEvent>();
   
   RegisterClassLoader runnable = null;
   
   public boolean classNotFound(ClassNotFoundEvent event)
   {
      events.add(event);

      if (runnable != null)
      {
         runnable.run();
         return true;
      }
      return false;
   }

   public void testClassNotFoundHandlerPolicy() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.addClassNotFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerSystem() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerParentDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerParentClassLoader() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.addClassNotFoundHandler(this);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerPolicyNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      policy.addClassNotFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader expected = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, expected);
   }

   public void testClassNotFoundHandlerDomainNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader expected = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, expected);
   }

   public void testClassNotFoundHandlerParentDomainNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader expected = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, expected);
   }

   public void testClassNotFoundHandlerParentClassLoaderNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setImportAll(true);
      parentPolicy.addClassNotFoundHandler(this);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader expected = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, expected);
   }

   public void testClassNotFoundHandlerSystemNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader expected = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, expected);
   }

   public void testClassNotFoundHandlerPolicyResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      policy.addClassNotFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClass(A.class, cl, runnable);
      assertLoadClassNoEvent(A.class, cl, runnable.getClassLoader());
   }

   public void testClassNotFoundHandlerDomainResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, domain, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
      assertLoadClassNoEvent(A.class, cl, runnable.getClassLoader());
   }

   public void testClassNotFoundHandlerParentDomainResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClass(A.class, cl, runnable);
      assertLoadClassNoEvent(A.class, cl, runnable.getClassLoader());
   }

   public void testClassNotFoundHandlerParentClassLoaderResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setImportAll(true);
      parentPolicy.addClassNotFoundHandler(this);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClass(A.class, cl, runnable);
      assertLoadClassNoEvent(A.class, cl, runnable.getClassLoader());
   }

   public void testClassNotFoundHandlerSystemResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClass(A.class, cl, runnable);
      assertLoadClassNoEvent(A.class, cl, runnable.getClassLoader());
   }

   public void testClassNotFoundHandlerPolicyNotResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      policy.addClassNotFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerDomainNotResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, domain, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerParentDomainNotResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      domain.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerParentClassLoaderNotResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setImportAll(true);
      parentPolicy.addClassNotFoundHandler(this);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   public void testClassNotFoundHandlerSystemNotResolved() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassNotFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy resolved = createMockClassLoaderPolicy("a");
      resolved.setPathsAndPackageNames(A.class);
      runnable = new RegisterClassLoader(system, null, resolved);
      assertLoadClassFail("does.not.exist.ClassName", cl);
   }

   protected Class<?> assertLoadClassNoEvent(Class<?> reference, ClassLoader start, ClassLoader expected)
   {
      Class<?> result = assertLoadClass(reference, start, expected);
      assertNoEvent();
      return result;
   }

   protected Class<?> assertLoadClass(Class<?> reference, ClassLoader start, RegisterClassLoader runnable)
   {
      return assertLoadClass(reference, start, runnable, false);
   }

   protected Class<?> assertLoadClass(Class<?> reference, ClassLoader start, RegisterClassLoader runnable, boolean isReference)
   {
      String name = reference.getName();
      Class<?> result = null;
      try
      {
         result = start.loadClass(name);
         getLog().debug("Got class: " + ClassLoaderUtils.classToString(result) + " for " + name + " from " + start);
      }
      catch (ClassNotFoundException e)
      {
         failure("Did not expect CNFE for " + name + " from " + start, e);
      }
      assertClassLoader(result, runnable.getClassLoader());
      if (isReference)
         assertClassEquality(reference, result);
      else
         assertNoClassEquality(reference, result);
      assertEvent(name, start);
      return result;
   }

   protected void assertLoadClassFail(String name, ClassLoader start)
   {
      super.assertLoadClassFail(name, start);
      assertEvent(name, start);
   }

   protected void assertEvent(String name, ClassLoader start)
   {
      assertTrue("Expected an event", events.isEmpty() == false);
      ClassNotFoundEvent event = events.remove(0);
      assertEquals(name, event.getClassName());
      assertEquals(start, event.getClassLoader());
      assertEquals(start, event.getSource());
   }

   protected void assertNoEvent()
   {
      assertTrue("Expected no events: " + events, events.isEmpty());
   }
   
   class RegisterClassLoader implements Runnable
   {
      private ClassLoaderSystem system;
      private ClassLoaderDomain domain;
      private ClassLoaderPolicy policy;
      private ClassLoader classLoader;
      
      public RegisterClassLoader(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy)
      {
         this.system = system;
         if (domain == null)
            domain = system.getDefaultDomain();
         this.domain = domain;
         this.policy = policy;
      }
      
      public void run()
      {
         classLoader = system.registerClassLoaderPolicy(domain, policy);
      }
      
      public ClassLoader getClassLoader()
      {
         if (classLoader == null)
            throw new Error("No classloader registered");
         return classLoader;
      }
   }
}
