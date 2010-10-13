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
package org.jboss.test.classloading.metadata.xml.test;

import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.metadata.ClassLoadingDomainMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingDomainMetaData10;
import org.jboss.classloading.spi.metadata.FilterMetaData;
import org.jboss.classloading.spi.metadata.LoaderMetaData;
import org.jboss.classloading.spi.metadata.ParentPolicyMetaData;
import org.jboss.javabean.plugins.jaxb.JavaBean20;
import org.jboss.test.classloading.metadata.xml.AbstractJBossXBTest;

import junit.framework.Test;

/**
 * ClassLoadingDomainMetaDataXmlUnitTestCase.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class ClassLoadingDomainMetaDataXmlUnitTestCase extends AbstractJBossXBTest
{
   public static Test suite()
   {
      return suite(ClassLoadingDomainMetaDataXmlUnitTestCase.class);
   }

   public ClassLoadingDomainMetaDataXmlUnitTestCase(String name)
   {
      super(name);
   }

   public void testDomainName() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal();
      assertEquals("testDomain", result.getName());
   }

   public void testDomainParentName() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal();
      assertEquals("testParentDomain", result.getParentDomain());
   }

   public void testDomainParentLoader() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal(JavaBean20.class);
      LoaderMetaData lmd = result.getParent();
      assertNotNull("Null parent loader metadata", lmd);
      assertNotNull("Null parent loader", lmd.getValue());      
   }

   public void testDomainParentPolicyWithName() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal();
      ParentPolicyMetaData ppmd = result.getParentPolicy();
      assertNotNull(ppmd);
      assertEquals("BEFORE", ppmd.getName());
      // actual PP and CF instantiation
      assertNotNull(ppmd.createParentPolicy());
   }

   public void testDomainParentPolicyWithFilters() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal();
      ParentPolicyMetaData ppmd = result.getParentPolicy();
      assertNotNull(ppmd);
      assertNull(ppmd.getName());
      FilterMetaData before = ppmd.getBeforeFilter();
      assertNotNull(before);
      assertEqualStrings(new String[]{"org.jboss.acme", "com.redhat.acme"}, before.getValue());
      FilterMetaData after = ppmd.getAfterFilter();
      assertNotNull(after);
      assertEqualStrings(new String[]{"org.jboss.foobar", "com.redhat.foobar"}, after.getValue());
      assertEquals("Qwert", ppmd.getDescription());
      // actual PP and CF instantiation
      assertNotNull(ppmd.createParentPolicy());
   }

   public void testDomainParentPolicyWithJavaBean() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal(JavaBean20.class);
      ParentPolicyMetaData ppmd = result.getParentPolicy();
      assertNotNull(ppmd);
      assertNull(ppmd.getName());
      FilterMetaData before = ppmd.getBeforeFilter();
      assertNotNull(before);
      // actual PP and CF instantiation
      assertNotNull(ppmd.createParentPolicy());
   }

   public void testDomainShutdown() throws Exception
   {
      ClassLoadingDomainMetaData result = unmarshal();
      assertEquals(ShutdownPolicy.GARBAGE_COLLECTION, result.getShutdownPolicy());
   }

   public void assertEqualStrings(String[] expected, Object result)
   {
      assertNotNull(expected);
      assertNotNull(result);
      assertTrue(result instanceof String[]);
      String[] strings = (String[]) result;
      assertEquals(expected.length, strings.length);
      for (int i = 0; i < expected.length; i++)
         assertEquals(expected[i], strings[i]);
   }

   protected ClassLoadingDomainMetaData unmarshal(Class<?>... extra) throws Exception
   {
      return unmarshalObject(ClassLoadingDomainMetaData10.class, ClassLoadingDomainMetaData10.class, extra);
   }
}
