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
package org.jboss.test.classloading.dependency.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.dependency.Domain;
import org.jboss.classloading.spi.dependency.ExportPackage;
import org.jboss.classloading.spi.dependency.ImportModule;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoaderPolicyModule;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;

/**
 * ClassLoadingAdmin unit tests
 *
 * @author adrian@jboss.org
 */
public class ClassLoadingAdminUnitTestCase extends AbstractMockClassLoaderUnitTest
{
   static String PACKAGEA = ClassLoaderUtils.getClassPackageName(A.class.getName());
   static String PACKAGEB = ClassLoaderUtils.getClassPackageName(B.class.getName());
   
   boolean lazyShutdown;
   
   public static Test suite()
   {
      return suite(ClassLoadingAdminUnitTestCase.class);
   }

   public ClassLoadingAdminUnitTestCase(String name)
   {
      super(name);
   }

   public ClassLoadingAdminUnitTestCase(String name, boolean lazyShutdown)
   {
      super(name);
      this.lazyShutdown = lazyShutdown;
   }

   ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
   
   MockClassLoaderPolicyModule moduleA;
   Class<?> classA;
   Domain domainA;
   KernelControllerContext contextA;
   
   MockClassLoaderPolicyModule moduleA2;
   Class<?> classA2;
   Domain domainA2;
   KernelControllerContext contextA2;

   MockClassLoaderPolicyModule moduleB;
   Class<?> classB;
   Domain domainB;
   KernelControllerContext contextB;

   MockClassLoaderPolicyModule moduleC;
   KernelControllerContext contextC;

   MockClassLoaderPolicyModule moduleD;
   MockClassLoaderPolicyModule moduleE;

   @Override
   protected KernelControllerContext install(MockClassLoadingMetaData metaData) throws Exception
   {
      if (lazyShutdown)
         metaData.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      return super.install(metaData);
   }

   protected void setUpAB() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a", "1.0.0");
      a.getCapabilities().addCapability(factory.createModule("ModuleA", "1.0.0"));
      a.getCapabilities().addCapability(factory.createPackage(PACKAGEA, "1.0.0"));
      a.setPathsAndPackageNames(A.class);
      a.setDomain("main");
      contextA = install(a);
      moduleA = assertModule(contextA);
      ClassLoader clA = assertClassLoader(contextA);
      classA = clA.loadClass(A.class.getName());
      domainA = moduleA.checkDomain();

      MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
      a2.getCapabilities().addCapability(factory.createModule("ModuleA", "2.0.0"));
      a2.getCapabilities().addCapability(factory.createPackage(PACKAGEA, "2.0.0"));
      a2.getCapabilities().addCapability(factory.createPackage(PACKAGEB, "2.0.0"));
      a2.setPathsAndPackageNames(A.class);
      a2.setDomain("main");
      contextA2 = install(a2);
      moduleA2 = assertModule(contextA2);
      ClassLoader clA2 = assertClassLoader(contextA2);
      classA2 = clA2.loadClass(A.class.getName());
      domainA2 = moduleA2.checkDomain();

      MockClassLoadingMetaData b = new MockClassLoadingMetaData("b", "2.0.0");
      b.setDomain("other");
      b.getCapabilities().addCapability(factory.createModule("ModuleBAlias", "3.0.0"));
      b.getCapabilities().addCapability(factory.createModule("ModuleB", "2.0.0"));
      b.getCapabilities().addCapability(factory.createPackage(PACKAGEB, "2.0.0"));
      b.setPathsAndPackageNames(B.class);
      contextB = install(b);
      moduleB = assertModule(contextB);
      ClassLoader clB = assertClassLoader(contextB);
      classB = clB.loadClass(B.class.getName());
      domainB = moduleB.checkDomain();
   }

   protected void setUpABCDE() throws Exception
   {
      setUpAB();
      
      MockClassLoadingMetaData c = new MockClassLoadingMetaData("c", "2.0.0");
      c.getRequirements().addRequirement(factory.createRequireModule("ModuleA", VersionRange.valueOf("1.0.0")));
      c.getRequirements().addRequirement(factory.createRequirePackage(PACKAGEA, VersionRange.valueOf("1.0.0")));
      c.setDomain("main");
      contextC = install(c);
      moduleC = assertModule(contextC);
      ClassLoader clC = assertClassLoader(contextC);
      clC.loadClass(A.class.getName());

      MockClassLoadingMetaData d = new MockClassLoadingMetaData("d", "5.0.0");
      d.getRequirements().addRequirement(factory.createRequireModule("ModuleB", new VersionRange("2.0.0")));
      d.getRequirements().addRequirement(factory.createRequirePackage(PACKAGEB, new VersionRange("2.0.0")));
      d.setDomain("other");
      KernelControllerContext contextD = install(d);
      moduleD = assertModule(contextD);
      ClassLoader clD = assertClassLoader(contextD);
      clD.loadClass(B.class.getName());

      MockClassLoadingMetaData e = new MockClassLoadingMetaData("e", "5.0.0");
      e.getRequirements().addRequirement(factory.createRequireModule("ModuleB", new VersionRange("2.0.0")));
      e.setDomain("other");
      KernelControllerContext contextE = install(e);
      moduleE = assertModule(contextE);
      ClassLoader clE = assertClassLoader(contextE);
      clE.loadClass(B.class.getName());
   }
   
   public void testGetModuleForClass() throws Exception
   {
      setUpAB();
      
      assertEquals(moduleA, classLoading.getModuleForClass(classA));
      assertEquals(moduleA, domainA.getModuleForClass(classA));
      
      assertEquals(moduleB, classLoading.getModuleForClass(classB));
      assertEquals(moduleB, domainB.getModuleForClass(classB));
      
      assertNull(classLoading.getModuleForClass(Object.class));
      assertNull(domainA.getModuleForClass(Object.class));
      
      uninstall(contextB);
      
      assertNull(classLoading.getModuleForClass(classB));
      assertNull(domainB.getModuleForClass(classB));
   }
   
   public void testGetModules() throws Exception
   {
      setUpAB();
      
      assertGetModules("ModuleA", null, moduleA, moduleA2);
      assertGetModules(domainA, "ModuleA", null, moduleA, moduleA2);
      assertGetModules(domainB, "ModuleA", null);
      
      assertGetModules("ModuleB", null, moduleB);
      assertGetModules(domainA, "ModuleB", null);
      assertGetModules(domainB, "ModuleB", null, moduleB);
      
      assertGetModules("ModuleA", "[1.0.0,1.0.0]", moduleA);
      assertGetModules(domainA, "ModuleA", "[1.0.0,1.0.0]", moduleA);
      assertGetModules(domainB, "ModuleA", "[1.0.0,1.0.0]");
      
      assertGetModules("ModuleA", "[1.0.0,2.0.0]", moduleA, moduleA2);
      assertGetModules(domainA, "ModuleA", "[1.0.0,2.0.0]", moduleA, moduleA2);
      assertGetModules(domainB, "ModuleA", "[1.0.0,2.0.0]");
      
      assertGetModules("ModuleA", "[0.0.0,0.0.0]");
      assertGetModules(domainA, "ModuleA", "[0.0.0,0.0.0]");
      assertGetModules(domainB, "ModuleA", "[0.0.0,0.0.0]");
      
      assertGetModules("ModuleBAlias", null, moduleB);
      assertGetModules(domainA, "ModuleBAlias", null);
      assertGetModules(domainB, "ModuleBAlias", null, moduleB);
      
      assertGetModules("ModuleBAlias", "[3.0.0,4.0.0]", moduleB);
      assertGetModules(domainA, "ModuleBAlias", "[3.0.0,4.0.0]");
      assertGetModules(domainB, "ModuleBAlias", "[3.0.0,4.0.0]", moduleB);
      
      uninstall(contextA2);
      
      assertGetModules("ModuleA", null, moduleA);
      assertGetModules(domainA, "ModuleA", null, moduleA);
      assertGetModules(domainB, "ModuleA", null);
      
      assertGetModules("ModuleA", "[1.0.0,2.0.0]", moduleA);
      assertGetModules(domainA, "ModuleA", "[1.0.0,2.0.0]", moduleA);
      assertGetModules(domainB, "ModuleA", "[1.0.0,2.0.0]");
   }
   
   public void testModuleExportingPackages() throws Exception
   {
      setUpAB();

      assertModuleExportingPackages(moduleA, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));
      assertModuleExportingPackages(domainA, moduleA, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));

      assertModuleExportingPackages(moduleA2, new ExportingPackage(moduleA2, PACKAGEA, "2.0.0"), new ExportingPackage(moduleA2, PACKAGEB, "2.0.0"));
      assertModuleExportingPackages(domainA2, moduleA2, new ExportingPackage(moduleA2, PACKAGEA, "2.0.0"), new ExportingPackage(moduleA2, PACKAGEB, "2.0.0"));

      uninstall(contextA2);

      assertModuleExportingPackages(moduleA2); 
      assertModuleExportingPackages(domainA2, moduleA2);
   }
   
   public void testExportingPackages() throws Exception
   {
      setUpABCDE();

      assertExportingPackages(PACKAGEA, null, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"), new ExportingPackage(moduleA2, PACKAGEA, "2.0.0"));
      assertExportingPackages(domainA, PACKAGEA, null, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"), new ExportingPackage(moduleA2, PACKAGEA, "2.0.0"));
      assertExportingPackages(domainB, PACKAGEA, null);

      assertExportingPackages(PACKAGEA, "[0.0.0,1.0.0]", new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));
      assertExportingPackages(domainA, PACKAGEA, "[0.0.0,1.0.0]", new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));
      assertExportingPackages(domainB, PACKAGEA, "[0.0.0,1.0.0]");

      assertExportingPackages(PACKAGEB, null, new ExportingPackage(moduleB, PACKAGEB, "2.0.0"), new ExportingPackage(moduleA2, PACKAGEB, "2.0.0"));
      assertExportingPackages(domainA, PACKAGEB, null, new ExportingPackage(moduleA2, PACKAGEB, "2.0.0"));
      assertExportingPackages(domainB, PACKAGEB, null, new ExportingPackage(moduleB, PACKAGEB, "2.0.0"));

      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("2.0.0"));
      assertExportedPackageImporting(domainA, PACKAGEB, VersionRange.valueOf("2.0.0"));
      assertExportedPackageImporting(domainB, PACKAGEB, VersionRange.valueOf("2.0.0"), moduleD);
      
      uninstall(contextA2);

      assertExportingPackages(PACKAGEA, null, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));
      assertExportingPackages(domainA, PACKAGEA, null, new ExportingPackage(moduleA, PACKAGEA, "1.0.0"));
      assertExportingPackages(domainB, PACKAGEA, null);
      
      uninstall(contextC);
      
      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"));
   }
   
   public void testImportingModules() throws Exception
   {
      setUpABCDE();

      assertImportingModule("ModuleA", null, new ImportingModule(moduleA));
      assertImportingModule(domainA, "ModuleA", null, new ImportingModule(moduleA));
      assertImportingModule(domainB, "ModuleA", null);

      assertImportingModule("ModuleA", "[0.0.0,1.0.0]", new ImportingModule(moduleA));
      assertImportingModule(domainA, "ModuleA", "[0.0.0,1.0.0]", new ImportingModule(moduleA));
      assertImportingModule(domainB, "ModuleA", "[0.0.0,1.0.0]");

      assertImportingModule("ModuleA", "[0.0.0,0.0.0]");
      assertImportingModule(domainA, "ModuleA", "[0.0.0,0.0.0]");
      assertImportingModule(domainB, "ModuleA", "[0.0.0,0.0.0]");

      assertImportingModule(null, null, new ImportingModule(moduleA), new ImportingModule(moduleB));
      assertImportingModule(domainA, null, null, new ImportingModule(moduleA));
      assertImportingModule(domainB, null, null, new ImportingModule(moduleB));

      assertImportedModulesImporting("ModuleA", moduleC);
      assertImportedModulesImporting("ModuleB", moduleD, moduleE);
      
      uninstall(contextC);

      assertImportingModule("ModuleA", null);
      assertImportingModule(domainA, "ModuleA", null);
      assertImportingModule(domainB, "ModuleA", null);

      assertNoImportedModules("ModuleA");
   }
   
   protected void assertGetModules(String name, String versionRange, Module... modules) throws Exception
   {
      Collection<Module> expected = new HashSet<Module>();
      for (Module module : modules)
         expected.add(module);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      Collection<Module> actual = classLoading.getModules(name, range);
      
      assertEquals(expected, actual);
   }
   
   protected void assertGetModules(Domain domain, String name, String versionRange, Module... modules) throws Exception
   {
      Collection<Module> expected = new HashSet<Module>();
      for (Module module : modules)
         expected.add(module);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      Collection<Module> actual = domain.getModules(name, range);
      
      assertEquals(expected, actual);
   }

   protected void assertModuleExportingPackages(Module module, ExportingPackage... expect) throws Exception
   {
      Set<ExportingPackage> expected = new HashSet<ExportingPackage>();
      for (ExportingPackage e : expect)
         expected.add(e);
      
      Set<ExportingPackage> actual = new HashSet<ExportingPackage>();
      Collection<ExportPackage> exported = classLoading.getExportedPackages(module);
      for (ExportPackage export : exported)
         actual.add(new ExportingPackage(export.getModule(), export.getName(), export.getVersion()));
      
      assertEquals(expected, actual);
   }

   protected void assertModuleExportingPackages(Domain domain, Module module, ExportingPackage... expect) throws Exception
   {
      Set<ExportingPackage> expected = new HashSet<ExportingPackage>();
      for (ExportingPackage e : expect)
         expected.add(e);
      
      Set<ExportingPackage> actual = new HashSet<ExportingPackage>();
      Collection<ExportPackage> exported = domain.getExportedPackages(module);
      for (ExportPackage export : exported)
         actual.add(new ExportingPackage(export.getModule(), export.getName(), export.getVersion()));
      
      assertEquals(expected, actual);
   }

   protected void assertExportingPackages(String name, String versionRange, ExportingPackage... expect) throws Exception
   {
      Set<ExportingPackage> expected = new HashSet<ExportingPackage>();
      for (ExportingPackage e : expect)
         expected.add(e);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      
      Set<ExportingPackage> actual = new HashSet<ExportingPackage>();
      Collection<ExportPackage> exported = classLoading.getExportedPackages(name, range);
      for (ExportPackage export : exported)
         actual.add(new ExportingPackage(export.getModule(), export.getName(), export.getVersion()));
      
      assertEquals(expected, actual);
   }

   protected void assertExportingPackages(Domain domain, String name, String versionRange, ExportingPackage... expect) throws Exception
   {
      Set<ExportingPackage> expected = new HashSet<ExportingPackage>();
      for (ExportingPackage e : expect)
         expected.add(e);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      
      Set<ExportingPackage> actual = new HashSet<ExportingPackage>();
      Collection<ExportPackage> exported = domain.getExportedPackages(name, range);
      for (ExportPackage export : exported)
         actual.add(new ExportingPackage(export.getModule(), export.getName(), export.getVersion()));
      
      assertEquals(expected, actual);
   }
   
   protected void assertExportedPackageImporting(String name, Module... expected) throws Exception
   {
      assertExportedPackageImporting(name, null, expected);
   }
      
   protected void assertExportedPackageImporting(String name, VersionRange range, Module... expected) throws Exception
   {
      Collection<ExportPackage> exportPackages = classLoading.getExportedPackages(name, range);
      assertTrue(exportPackages.toString(), exportPackages.size() == 1);
      ExportPackage exportPackage = exportPackages.iterator().next();
      assertModules(exportPackage.getImportingModules(), expected);
   }
   
   protected void assertExportedPackageImporting(Domain domain, String name, Module... expected) throws Exception
   {
      assertExportedPackageImporting(domain, name, null, expected);
   }
      
   protected void assertExportedPackageImporting(Domain domain, String name, VersionRange range, Module... expected) throws Exception
   {
      Collection<ExportPackage> exportPackages = domain.getExportedPackages(name, range);
      assertTrue(exportPackages.toString(), exportPackages.size() == 1);
      ExportPackage exportPackage = exportPackages.iterator().next();
      assertModules(exportPackage.getImportingModules(), expected);
   }

   protected void assertImportingModule(String name, String versionRange, ImportingModule... expect) throws Exception
   {
      Set<ImportingModule> expected = new HashSet<ImportingModule>();
      for (ImportingModule e : expect)
         expected.add(e);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      
      Set<ImportingModule> actual = new HashSet<ImportingModule>();
      Collection<ImportModule> imported = classLoading.getImportedModules(name, range);
      for (ImportModule imp : imported)
         actual.add(new ImportingModule(imp.getModule()));
      
      assertEquals(expected, actual);
   }

   protected void assertImportingModule(Domain domain, String name, String versionRange, ImportingModule... expect) throws Exception
   {
      Set<ImportingModule> expected = new HashSet<ImportingModule>();
      for (ImportingModule e : expect)
         expected.add(e);
      
      VersionRange range = null;
      if (versionRange != null)
         range = VersionRange.parseRangeSpec(versionRange);
      
      Set<ImportingModule> actual = new HashSet<ImportingModule>();
      Collection<ImportModule> imported = domain.getImportedModules(name, range);
      for (ImportModule imp : imported)
         actual.add(new ImportingModule(imp.getModule()));
      
      assertEquals(expected, actual);
   }
   
   protected void assertNoImportedModules(String name) throws Exception
   {
      Collection<ImportModule> importedModules = classLoading.getImportedModules(name, null);
      assertTrue(importedModules.toString(), importedModules.isEmpty());
   }
   
   protected void assertImportedModulesImporting(String name, Module... expected) throws Exception
   {
      Collection<ImportModule> importedModules = classLoading.getImportedModules(name, null);
      assertTrue(importedModules.toString(), importedModules.size() == 1);
      ImportModule importModule = importedModules.iterator().next();
      assertModules(importModule.getImportingModules(), expected);
   }
   
   protected void assertModules(Collection<Module> actual, Module... expected) throws Exception
   {
      Set<Module> expect = new HashSet<Module>();
      for (Module module : expected)
         expect.add(module);
      
      Set<Module> act = new HashSet<Module>(actual);
      
      assertEquals(expect, act);
   }
   
   private class ExportingPackage
   {
      Module module;
      String name;
      Object version;
      
      public ExportingPackage(Module module, String name, Object version)
      {
         this.module = module;
         this.name = name;
         this.version = version;
      }
      
      public boolean equals(Object obj)
      {
         if (obj == this)
            return true;
         if (obj == null || obj instanceof ExportingPackage == false)
            return false;

         ExportingPackage other = (ExportingPackage) obj;
         if (notEquals(module, other.module))
            return false;
         if (notEquals(name, other.name))
            return false;
         if (notEquals(version, other.version))
            return false;
         return true;
      }
      
      public int hashCode()
      {
         return name.hashCode();
      }
      
      public String toString()
      {
         if (version != null)
            return module + "/" + name + ":" + version;
         return module + "/" + name;
      }
   }
   
   private class ImportingModule
   {
      Module module;
      
      public ImportingModule(Module module)
      {
         this.module = module;
      }
      
      public boolean equals(Object obj)
      {
         if (obj == this)
            return true;
         if (obj == null || obj instanceof ImportingModule == false)
            return false;

         ImportingModule other = (ImportingModule) obj;
         if (notEquals(module, other.module))
            return false;
         return true;
      }
      
      public int hashCode()
      {
         return module.hashCode();
      }
      
      public String toString()
      {
         return module.toString();
      }
   }
   
   static boolean notEquals(Object one, Object two)
   {
      if (one == null && two == null)
         return false;
      if (one == null && two != null)
         return true;
      return one.equals(two) == false;
   }
}
