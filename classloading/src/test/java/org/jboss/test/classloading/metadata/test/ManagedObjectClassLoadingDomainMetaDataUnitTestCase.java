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
package org.jboss.test.classloading.metadata.test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.metadata.ClassLoadingDomainMetaData;
import org.jboss.classloading.spi.metadata.ParentPolicyMetaData;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.MetaTypeFactory;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.MetaValueFactory;
import org.jboss.test.BaseTestCase;

import junit.framework.Test;

/**
 * ManagedObjectClassLoadingDomainMetaDataUnitTestCase.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class ManagedObjectClassLoadingDomainMetaDataUnitTestCase extends BaseTestCase
{
   private ManagedObjectFactory moFactory = ManagedObjectFactory.getInstance();
   private MetaTypeFactory mtFactory = MetaTypeFactory.getInstance();
   private MetaValueFactory mvFactory = MetaValueFactory.getInstance();

   public static Test suite()
   {
      return suite(ManagedObjectClassLoadingDomainMetaDataUnitTestCase.class);
   }

   public ManagedObjectClassLoadingDomainMetaDataUnitTestCase(String name)
   {
      super(name);
   }

   protected ManagedObject assertManagedObject(ClassLoadingDomainMetaData test)
   {
      ManagedObject result = moFactory.initManagedObject(test, null, null);
      assertNotNull(result);
      List<String> expectedProperties = Arrays.asList("name", "parentDomain", "parentPolicy", "shutdown", "useLoadClassForParent");
      Set<String> actualProperties = result.getPropertyNames();
      for (String expected : expectedProperties)
      {
         if (actualProperties.contains(expected) == false)
            fail("Expected property: " + expected);
      }
      for (String actual : actualProperties)
      {
         if (expectedProperties.contains(actual) == false)
            fail("Did not expect property: " + actual);
      }
      return result;
   }

   protected ManagedProperty assertManagedProperty(ManagedObject mo, String name, MetaType metaType, MetaValue metaValue)
   {
      ManagedProperty property = mo.getProperty(name);
      assertNotNull("No property " + name, property);
      assertEquals(metaType, property.getMetaType());
      assertEquals(metaValue, property.getValue());
      return property;
   }

   protected <T> ManagedProperty assertManagedProperty(ManagedObject mo, String name, Class<T> type, T value)
   {
      MetaType metaType = mtFactory.resolve(type);

      MetaValue metaValue = null;
      if (value != null)
         metaValue = mvFactory.create(value);
      return assertManagedProperty(mo, name, metaType, metaValue);
   }

   public void testConstructor() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setParentPolicy(new ParentPolicyMetaData());
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "name", String.class, "<unknown>");
      assertManagedProperty(mo, "parentDomain", String.class, null);
      assertManagedProperty(mo, "parentPolicy", ParentPolicyMetaData.class, new ParentPolicyMetaData());
      assertManagedProperty(mo, "shutdown", ShutdownPolicy.class, null);
      assertManagedProperty(mo, "useLoadClassForParent", Boolean.class, null);
   }

   public void testSetName() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setName("test");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "name", String.class, "test");
   }

   public void testSetParentDomain() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setParentDomain("parentDomain");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "parentDomain", String.class, "parentDomain");
   }

   public void testParentPolicy() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setParentPolicy(new ParentPolicyMetaData());
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "parentPolicy", ParentPolicyMetaData.class, test.getParentPolicy());
   }

   public void testSetShutdownPolicy() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "shutdown", ShutdownPolicy.class, ShutdownPolicy.GARBAGE_COLLECTION);
   }

   public void testSetUseLoadClassForParent() throws Exception
   {
      ClassLoadingDomainMetaData test = new ClassLoadingDomainMetaData();
      test.setUseLoadClassForParent(true);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "useLoadClassForParent", Boolean.class, true);
   }
}
