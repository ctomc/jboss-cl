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
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.test.classloader.transform.support.YearTranslator;

/**
 * Per policy transformation tests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DomainTransformUnitTestCase extends TransformTest
{
   public DomainTransformUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(DomainTransformUnitTestCase.class);
   }

   protected void prepareTransform(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy)
   {
      domain.addTranslator(new YearTranslator());
   }

   protected String getClassName()
   {
      return "org.jboss.test.classloader.transform.support.DomainTester";
   }

   protected void testInstance(Object instance) throws Exception
   {
      Method author = instance.getClass().getDeclaredMethod("year");
      assertNotNull(author.invoke(instance));

      try
      {
         instance.getClass().getDeclaredMethod("locale");
         fail("Should not be here.");
      }
      catch (Exception ignored)
      {
      }
   }
}