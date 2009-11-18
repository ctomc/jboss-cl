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

import org.jboss.classloader.plugins.loader.ClassLoaderToLoaderAdapter;
import org.jboss.classloader.spi.ClassFoundEvent;
import org.jboss.classloader.spi.ClassFoundHandler;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.test.classloader.AbstractClassLoaderTestWithSecurity;
import org.jboss.test.classloader.notifications.support.a.A;

/**
 * ClassFoundHnadlerUnitTestCase
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassFoundHandlerUnitTestCase extends AbstractClassLoaderTestWithSecurity implements ClassFoundHandler
{
   public static Test suite()
   {
      return suite(ClassFoundHandlerUnitTestCase.class);
   }

   public ClassFoundHandlerUnitTestCase(String name)
   {
      super(name);
   }
   
   List<ClassFoundEvent> events = new CopyOnWriteArrayList<ClassFoundEvent>();
   
   public void classFound(ClassFoundEvent event)
   {
      events.add(event);
   }

   public void testClassFoundHandlerPolicy() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      policy.addClassFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClass(A.class, cl);
      assertLoadClassNoEvent(A.class, cl);
   }

   public void testClassFoundHandlerDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClass(A.class, cl);
      assertLoadClassNoEvent(A.class, cl);
   }

   public void testClassFoundHandlerParentDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      defaultDomain.addClassFoundHandler(this);

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setPathsAndPackageNames(A.class);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClass(A.class, cl, parentCl);
      assertLoadClassNoEvent(A.class, cl, parentCl);
   }

   public void testClassFoundHandlerParentClassLoader() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      defaultDomain.addClassFoundHandler(this);

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setPathsAndPackageNames(A.class);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClass(A.class, cl, parentCl);
      assertLoadClassNoEvent(A.class, cl, parentCl);
   }

   public void testClassFoundHandlerSystem() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClass(A.class, cl);
      assertLoadClassNoEvent(A.class, cl);
   }

   public void testClassFoundHandlerPolicyNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      policy.addClassFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClassFail("does.not.exist.Class", cl);
   }

   public void testClassFoundHandlerDomainNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.Class", cl);
   }

   public void testClassFoundHandlerParentDomainNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, defaultDomain);
      defaultDomain.addClassFoundHandler(this);

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setPathsAndPackageNames(A.class);
      system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.Class", cl);
   }

   public void testClassFoundHandlerParentClassLoaderNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      defaultDomain.addClassFoundHandler(this);

      MockClassLoaderPolicy parentPolicy = createMockClassLoaderPolicy("parent");
      parentPolicy.setPathsAndPackageNames(A.class);
      ClassLoader parentCl = system.registerClassLoaderPolicy(defaultDomain, parentPolicy);

      ClassLoaderDomain domain = system.createAndRegisterDomain("TestDomain", ParentPolicy.BEFORE, new ClassLoaderToLoaderAdapter(parentCl));
      domain.setUseLoadClassForParent(false);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain, policy);

      assertLoadClassFail("does.not.exist.Class", cl);
   }

   public void testClassFoundHandlerSystemNoEvent() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.addClassFoundHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setPathsAndPackageNames(A.class);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertLoadClassFail("does.not.exist.Class", cl);
   }

   public void testClassFoundHandlerWrongPolicy() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      policy.addClassFoundHandler(this);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      ClassLoader clA = system.registerClassLoaderPolicy(a);

      assertLoadClassNoEvent(A.class, cl, clA);
   }

   public void testClassFoundHandlerDifferentPolicy() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      policy.setImportAll(true);
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      MockClassLoaderPolicy a = createMockClassLoaderPolicy("a");
      a.setPathsAndPackageNames(A.class);
      a.addClassFoundHandler(this);
      ClassLoader clA = system.registerClassLoaderPolicy(a);

      assertLoadClass(A.class, cl, clA);
      assertLoadClassNoEvent(A.class, cl, clA);
   }

   protected Class<?> assertLoadClassNoEvent(Class<?> reference, ClassLoader start)
   {
      return assertLoadClass(reference, start, start);
   }

   protected Class<?> assertLoadClassNoEvent(Class<?> reference, ClassLoader start, ClassLoader expected)
   {
      Class<?> result = super.assertLoadClass(reference, start, expected);
      assertNoEvent();
      return result;
   }

   protected Class<?> assertLoadClass(Class<?> reference, ClassLoader start, ClassLoader expected)
   {
      Class<?> result = super.assertLoadClass(reference, start, expected);
      assertEvent(reference.getName(), expected);
      return result;
   }

   protected void assertLoadClassFail(String name, ClassLoader start)
   {
      super.assertLoadClassFail(name, start);
      assertNoEvent();
   }

   protected void assertEvent(String name, ClassLoader expected)
   {
      assertTrue("Expected an event", events.isEmpty() == false);
      ClassFoundEvent event = events.remove(0);
      assertEquals(name, event.getClassName());
      assertEquals(expected, event.getClassLoader());
      assertEquals(expected, event.getSource());
   }

   protected void assertNoEvent()
   {
      assertTrue("Expected no events: " + events, events.isEmpty());
   }
}
