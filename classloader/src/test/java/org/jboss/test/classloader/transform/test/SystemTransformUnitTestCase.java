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
package org.jboss.test.classloader.transform.test;

import java.lang.reflect.Method;

import junit.framework.Test;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.test.classloader.transform.support.LocaleTranslator;

/**
 * Per system transformation tests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SystemTransformUnitTestCase extends TransformTest
{
   public SystemTransformUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(SystemTransformUnitTestCase.class);
   }

   protected void prepareTransform(ClassLoaderSystem system, ClassLoaderPolicy policy)
   {
      system.addTranslator(new LocaleTranslator());
   }

   protected String getClassName()
   {
      return "org.jboss.test.classloader.transform.support.SystemTester";
   }

   protected void testInstance(Object instance) throws Exception
   {
      try
      {
         instance.getClass().getDeclaredMethod("author");
         fail("Should not be here.");
      }
      catch (Exception ignored)
      {
      }

      Method locale = instance.getClass().getDeclaredMethod("locale");
      assertNotNull(locale.invoke(instance));
   }
}