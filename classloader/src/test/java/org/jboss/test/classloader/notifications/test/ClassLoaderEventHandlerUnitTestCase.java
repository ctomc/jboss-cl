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

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderEvent;
import org.jboss.classloader.spi.ClassLoaderEventHandler;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.test.classloader.AbstractClassLoaderTestWithSecurity;

/**
 * ClassFoundHnadlerUnitTestCase
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoaderEventHandlerUnitTestCase extends AbstractClassLoaderTestWithSecurity implements ClassLoaderEventHandler
{
   public static Test suite()
   {
      return suite(ClassLoaderEventHandlerUnitTestCase.class);
   }

   public ClassLoaderEventHandlerUnitTestCase(String name)
   {
      super(name);
   }
   
   List<ClassLoaderEvent> registered = new CopyOnWriteArrayList<ClassLoaderEvent>();
   List<ClassLoaderEvent> unregistered = new CopyOnWriteArrayList<ClassLoaderEvent>();
   
   public void fireRegisterClassLoader(ClassLoaderEvent event)
   {
      registered.add(event);
   }
   
   public void fireUnregisterClassLoader(ClassLoaderEvent event)
   {
      unregistered.add(event);
   }

   public void testClassLoaderEventDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertRegistered(domain, cl);
      
      system.unregisterClassLoader(cl);
      assertUnregistered(domain, cl);
   }

   public void testClassLoaderEventSystem() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      system.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertRegistered(domain, cl);
      
      system.unregisterClassLoader(cl);
      assertUnregistered(domain, cl);
   }

   public void testClassLoaderEventWrongDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain1 = system.createAndRegisterDomain("Domain1");
      ClassLoaderDomain domain2 = system.createAndRegisterDomain("Domain2");
      domain1.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain2, policy);

      assertNoRegistered();
      
      system.unregisterClassLoader(cl);
      assertNoUnregistered();
   }

   public void testClassLoaderEventCorrectDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      system.createAndRegisterDomain("Domain1");
      ClassLoaderDomain domain2 = system.createAndRegisterDomain("Domain2");
      domain2.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(domain2, policy);

      assertRegistered(domain2, cl);
      
      system.unregisterClassLoader(cl);
      assertUnregistered(domain2, cl);
   }

   public void testClassLoaderEventShutdownDomain() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      domain.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertRegistered(domain, cl);
      
      system.shutdown();
      assertUnregistered(domain, cl);
   }

   public void testClassLoaderEventShutdownSystem() throws Exception
   {
      ClassLoaderSystem system = createClassLoaderSystemWithModifiedBootstrap();
      ClassLoaderDomain domain = system.getDefaultDomain();
      system.addClassLoaderEventHandler(this);

      MockClassLoaderPolicy policy = createMockClassLoaderPolicy("test");
      ClassLoader cl = system.registerClassLoaderPolicy(policy);

      assertRegistered(domain, cl);
      
      system.shutdown();
      assertUnregistered(domain, cl);
   }

   protected void assertRegistered(ClassLoaderDomain domain, ClassLoader expected)
   {
      assertTrue("Expected a registered", registered.isEmpty() == false);
      ClassLoaderEvent event = registered.remove(0);
      assertEquals(domain, event.getClassLoaderDomain());
      assertEquals(domain, event.getSource());
      assertEquals(expected, event.getClassLoader());
   }

   protected void assertNoRegistered()
   {
      assertTrue("Expected no registered: " + registered, registered.isEmpty());
   }

   protected void assertUnregistered(ClassLoaderDomain domain, ClassLoader expected)
   {
      assertTrue("Expected an unregistered", unregistered.isEmpty() == false);
      ClassLoaderEvent event = unregistered.remove(0);
      assertEquals(domain, event.getClassLoaderDomain());
      assertEquals(domain, event.getSource());
      assertEquals(expected, event.getClassLoader());
   }

   protected void assertNoUnregistered()
   {
      assertTrue("Expected no unregistered: " + unregistered, unregistered.isEmpty());
   }
}
