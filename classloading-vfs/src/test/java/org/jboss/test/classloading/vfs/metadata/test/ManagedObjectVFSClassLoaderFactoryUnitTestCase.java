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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.metadata.CapabilitiesMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.metadata.FilterMetaData;
import org.jboss.classloading.spi.metadata.ParentPolicyMetaData;
import org.jboss.classloading.spi.metadata.RequirementsMetaData;
import org.jboss.classloading.spi.version.Version;
import org.jboss.classloading.spi.vfs.metadata.VFSClassLoaderFactory;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.MetaTypeFactory;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.MetaValueFactory;
import org.jboss.test.BaseTestCase;

/**
 * ManagedObjectVFSClassLoaderFactoryUnitTestCase.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class ManagedObjectVFSClassLoaderFactoryUnitTestCase extends BaseTestCase
{
   private ManagedObjectFactory moFactory = ManagedObjectFactory.getInstance();
   private MetaTypeFactory mtFactory = MetaTypeFactory.getInstance();
   private MetaValueFactory mvFactory = MetaValueFactory.getInstance();

   public static Test suite()
   {
      return suite(ManagedObjectVFSClassLoaderFactoryUnitTestCase.class);
   }

   public ManagedObjectVFSClassLoaderFactoryUnitTestCase(String name)
   {
      super(name);
   }

   protected ManagedObject assertManagedObject(VFSClassLoaderFactory test)
   {
      ManagedObject result = moFactory.initManagedObject(test, null, null);
      assertNotNull(result);
      List<String> expectedProperties = Arrays.asList("name", "version", "context", "domain", "parentDomain", "topLevelClassLoader", "exportAll", "shutdown", "included", "includedMetaData", "excluded", "excludedMetaData", "excludedExport", "excludedExportMetaData", "importAll", "parentFirst", "cache", "blackList", "system", "roots", "capabilities", "requirements", "parentPolicy");
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

   protected ManagedProperty assertManagedProperty(ManagedObject mo, String name, Type type, Object value)
   {
      MetaType metaType = mtFactory.resolve(type);

      MetaValue metaValue = null;
      if (value != null)
         metaValue = mvFactory.create(value, type);
      return assertManagedProperty(mo, name, metaType, metaValue);
   }

   protected <T> ManagedProperty assertManagedProperty(ManagedObject mo, String name, Class<T> type, T value)
   {
      return assertManagedProperty(mo, name, (Type) type, value);
   }

   public void testConstructor() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setParentPolicy(new ParentPolicyMetaData());
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "name", String.class, "<unknown>");
      assertManagedProperty(mo, "version", Version.class, Version.DEFAULT_VERSION);
      assertManagedProperty(mo, "domain", String.class, null);
      assertManagedProperty(mo, "parentDomain", String.class, null);
      assertManagedProperty(mo, "topLevelClassLoader", boolean.class, false);
      assertManagedProperty(mo, "exportAll", ExportAll.class, null);
      assertManagedProperty(mo, "shutdown", ShutdownPolicy.class, null);
      assertManagedProperty(mo, "included", String.class, null);
      assertManagedProperty(mo, "includedMetaData", FilterMetaData.class, null);
      assertManagedProperty(mo, "excluded", String.class, null);
      assertManagedProperty(mo, "excludedMetaData", FilterMetaData.class, null);
      assertManagedProperty(mo, "excludedExport", String.class, null);
      assertManagedProperty(mo, "excludedExportMetaData", FilterMetaData.class, null);
      assertManagedProperty(mo, "importAll", boolean.class, false);
      assertManagedProperty(mo, "parentFirst", boolean.class, true);
      assertManagedProperty(mo, "capabilities", CapabilitiesMetaData.class, new CapabilitiesMetaData());
      assertManagedProperty(mo, "requirements", RequirementsMetaData.class, new RequirementsMetaData());
      assertManagedProperty(mo, "parentPolicy", ParentPolicyMetaData.class, new ParentPolicyMetaData());
   }

   public void testSetName() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setName("test");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "name", String.class, "test");
   }

   public void testSetVersion() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setVersion("1.0.0");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "version", Version.class, Version.parseVersion("1.0.0"));
   }

   public void testContext() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setContextName("context");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "context", String.class, "context");
   }

   public void testSetDomain() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setDomain("domain");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "domain", String.class, "domain");
   }

   public void testSetParentDomain() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setParentDomain("parentDomain");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "parentDomain", String.class, "parentDomain");
   }

   public void testSetTopLevelClassLoader() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setTopLevelClassLoader(true);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "topLevelClassLoader", boolean.class, true);
   }

   public void testSetExportAll() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setExportAll(ExportAll.ALL);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "exportAll", ExportAll.class, ExportAll.ALL);
   }

   public void testSetShutdownPolicy() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "shutdown", ShutdownPolicy.class, ShutdownPolicy.GARBAGE_COLLECTION);
   }

   public void testSetIncludedPackages() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setIncludedPackages("Included");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "included", String.class, "Included");
   }

   public void testSetIncludedMetaData() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      FilterMetaData fmd = new FilterMetaData();
      test.setIncludedMetaData(fmd);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "includedMetaData", FilterMetaData.class, fmd);
   }

   public void testSetExcludedPackages() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setExcludedPackages("Excluded");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "excluded", String.class, "Excluded");
   }

   public void testSetExcludedMetaData() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      FilterMetaData fmd = new FilterMetaData();
      test.setExcludedMetaData(fmd);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "excludedMetaData", FilterMetaData.class, fmd);
   }

   public void testSetExcludedExportPackages() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setExcludedExportPackages("ExcludedExport");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "excludedExport", String.class, "ExcludedExport");
   }

   public void testSetExcludedExportMetaData() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      FilterMetaData fmd = new FilterMetaData();
      test.setExcludedExportMetaData(fmd);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "excludedExportMetaData", FilterMetaData.class, fmd);
   }

   public void testSetImportAll() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setImportAll(true);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "importAll", boolean.class, true);
   }

   public void testJ2seClassLoadingComplaince() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setJ2seClassLoadingCompliance(false);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "parentFirst", boolean.class, false);
   }

   public void testCacheable() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setCacheable(false);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "cache", boolean.class, false);
   }

   public void testBlackList() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setBlackListable(false);
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "blackList", boolean.class, false);
   }

   public void testSetSystem() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setClassLoaderSystemName("test");
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "system", String.class, "test");
   }

   public void testSetRoots() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      List<String> roots = Arrays.asList("test1", "test2");
      test.setRoots(roots);
      ManagedObject mo = assertManagedObject(test);
      Field field = getClass().getField("rootsSignature");
      assertManagedProperty(mo, "roots", field.getGenericType(), roots);
   }

   public static List<String> rootsSignature;

   public void testCapabilities() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.getCapabilities().addCapability(factory.createModule("module"));
      test.getCapabilities().addCapability(factory.createPackage("package"));
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "requirements", RequirementsMetaData.class, test.getRequirements());
   }

   public void testRequirements() throws Exception
   {
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.getRequirements().addRequirement(factory.createRequireModule("module"));
      test.getRequirements().addRequirement(factory.createRequirePackage("package"));
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "requirements", RequirementsMetaData.class, test.getRequirements());
   }

   public void testParentPolicy() throws Exception
   {
      VFSClassLoaderFactory test = new VFSClassLoaderFactory();
      test.setParentPolicy(new ParentPolicyMetaData());
      ManagedObject mo = assertManagedObject(test);
      assertManagedProperty(mo, "parentPolicy", ParentPolicyMetaData.class, test.getParentPolicy());
   }
}
