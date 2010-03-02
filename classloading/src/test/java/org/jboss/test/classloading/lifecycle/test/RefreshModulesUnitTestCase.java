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
package org.jboss.test.classloading.lifecycle.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.dependency.Domain;
import org.jboss.classloading.spi.dependency.ExportPackage;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoaderPolicyModule;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.dependency.support.a.A;
import org.jboss.test.classloading.dependency.support.b.B;
import org.jboss.test.classloading.lifecycle.support.a.MockLifeCycle;

/**
 * ClassLoadingAdmin unit tests
 *
 * @author adrian@jboss.org
 */
public class RefreshModulesUnitTestCase extends AbstractMockLifeCycleUnitTest
{
   static String PACKAGEA = ClassLoaderUtils.getClassPackageName(A.class.getName());
   static String PACKAGEB = ClassLoaderUtils.getClassPackageName(B.class.getName());
   
   boolean lazyShutdown;
   
   public static Test suite()
   {
      return suite(RefreshModulesUnitTestCase.class);
   }

   public RefreshModulesUnitTestCase(String name)
   {
      super(name);
   }

   public RefreshModulesUnitTestCase(String name, boolean lazyShutdown)
   {
      super(name);
      this.lazyShutdown = lazyShutdown;
   }

   ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
   
   MockClassLoaderPolicyModule moduleA;
   MockLifeCycle lifecycleA;
   Class<?> classA;
   Domain domainA;
   KernelControllerContext contextA;
   
   MockClassLoaderPolicyModule moduleA2;
   MockLifeCycle lifecycleA2;
   Class<?> classA2;
   Domain domainA2;
   KernelControllerContext contextA2;

   MockClassLoaderPolicyModule moduleB;
   MockLifeCycle lifecycleB;
   Class<?> classB;
   Domain domainB;
   KernelControllerContext contextB;

   MockClassLoaderPolicyModule moduleC;
   MockLifeCycle lifecycleC;
   KernelControllerContext contextC;

   MockClassLoaderPolicyModule moduleD;
   MockLifeCycle lifecycleD;
   KernelControllerContext contextD;
   
   MockClassLoaderPolicyModule moduleE;
   MockLifeCycle lifecycleE;
   KernelControllerContext contextE;

   @Override
   protected KernelControllerContext install(MockClassLoadingMetaData metaData) throws Exception
   {
      if (lazyShutdown)
         metaData.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      return super.install(metaData);
   }
   
   protected void setUpA() throws Exception
   {
      MockClassLoadingMetaData a = new MockClassLoadingMetaData("a", "1.0.0");
      a.getCapabilities().addCapability(factory.createModule("ModuleA", "1.0.0"));
      a.getCapabilities().addCapability(factory.createPackage(PACKAGEA, "1.0.0"));
      a.setPathsAndPackageNames(A.class);
      a.setDomain("main");
      contextA = install(a);
      moduleA = assertMockClassPolicyModule(contextA);
      lifecycleA = assertLifeCycle(contextA);
      resolve(contextA);
      ClassLoader clA = assertClassLoader(contextA);
      classA = clA.loadClass(A.class.getName());
      domainA = moduleA.checkDomain();
   }
   
   protected void setUpB() throws Exception
   {
      MockClassLoadingMetaData b = new MockClassLoadingMetaData("b", "2.0.0");
      b.setDomain("other");
      b.getCapabilities().addCapability(factory.createModule("ModuleBAlias", "3.0.0"));
      b.getCapabilities().addCapability(factory.createModule("ModuleB", "2.0.0"));
      b.getCapabilities().addCapability(factory.createPackage(PACKAGEB, "2.0.0"));
      b.setPathsAndPackageNames(B.class);
      contextB = install(b);
      moduleB = assertMockClassPolicyModule(contextB);
      lifecycleB = assertLifeCycle(contextB);
      resolve(contextB);
      ClassLoader clB = assertClassLoader(contextB);
      classB = clB.loadClass(B.class.getName());
      domainB = moduleB.checkDomain();
   }
   
   protected void setUpAB() throws Exception
   {
      setUpA();

      MockClassLoadingMetaData a2 = new MockClassLoadingMetaData("a", "2.0.0");
      a2.getCapabilities().addCapability(factory.createModule("ModuleA", "2.0.0"));
      a2.getCapabilities().addCapability(factory.createPackage(PACKAGEA, "2.0.0"));
      a2.getCapabilities().addCapability(factory.createPackage(PACKAGEB, "2.0.0"));
      a2.setPathsAndPackageNames(A.class);
      a2.setDomain("main");
      contextA2 = install(a2);
      moduleA2 = assertMockClassPolicyModule(contextA2);
      lifecycleA2 = assertLifeCycle(contextA2);
      resolve(contextA2);
      ClassLoader clA2 = assertClassLoader(contextA2);
      classA2 = clA2.loadClass(A.class.getName());
      domainA2 = moduleA2.checkDomain();

      setUpB();
   }

   protected void setUpABCDE() throws Exception
   {
      setUpAB();
      
      MockClassLoadingMetaData c = new MockClassLoadingMetaData("c", "2.0.0");
      c.getRequirements().addRequirement(factory.createRequireModule("ModuleA", VersionRange.valueOf("1.0.0")));
      c.getRequirements().addRequirement(factory.createRequirePackage(PACKAGEA, VersionRange.valueOf("1.0.0")));
      c.setDomain("main");
      contextC = install(c);
      moduleC = assertMockClassPolicyModule(contextC);
      lifecycleC = assertLifeCycle(contextC);
      resolve(contextC);
      ClassLoader clC = assertClassLoader(contextC);
      clC.loadClass(A.class.getName());

      MockClassLoadingMetaData d = new MockClassLoadingMetaData("d", "5.0.0");
      d.getRequirements().addRequirement(factory.createRequireModule("ModuleB", new VersionRange("2.0.0")));
      d.getRequirements().addRequirement(factory.createRequirePackage(PACKAGEB, new VersionRange("2.0.0")));
      d.setDomain("other");
      contextD = install(d);
      moduleD = assertMockClassPolicyModule(contextD);
      lifecycleD = assertLifeCycle(contextD);
      resolve(contextD);
      ClassLoader clD = assertClassLoader(contextD);
      clD.loadClass(B.class.getName());

      MockClassLoadingMetaData e = new MockClassLoadingMetaData("e", "5.0.0");
      e.getRequirements().addRequirement(factory.createRequireModule("ModuleB", new VersionRange("2.0.0")));
      e.setDomain("other");
      contextE = install(e);
      moduleE = assertMockClassPolicyModule(contextB);
      lifecycleE = assertLifeCycle(contextE);
      resolve(contextE);
      moduleE = assertMockClassPolicyModule(contextE);
      ClassLoader clE = assertClassLoader(contextE);
      clE.loadClass(B.class.getName());
   }
   
   public void testRefreshModulesSimple() throws Exception
   {
      setUpAB();
      
      resetFlags();
      classLoading.refreshModules(moduleA);
      assertBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
   }
   
   public void testRefreshModulesMultiple() throws Exception
   {
      setUpAB();
      
      resetFlags();
      classLoading.refreshModules(moduleA, moduleB);
      assertBounce(contextA);
      assertNoBounce(contextA2);
      assertBounce(contextB);
   }
   
   public void testRefreshModulesNothing() throws Exception
   {
      setUpAB();
      
      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
   }
   
   public void testRefreshModulesCascaded() throws Exception
   {
      setUpABCDE();
      
      resetFlags();
      classLoading.refreshModules(moduleA);
      assertBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertReResolved(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();
      
      resetFlags();
      classLoading.refreshModules(moduleA);
      assertBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesNotResolvedCascaded() throws Exception
   {
      setUpABCDE();
      
      resetFlags();
      unresolve(contextA);
      assertNotResolved(contextA);
      assertNotResolved(contextC);

      resetFlags();
      classLoading.refreshModules(moduleA);
      assertNotResolved(contextA);
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNotResolved(contextC);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesNotResolvedNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();
      
      resetFlags();
      unresolve(contextA);
      assertNotResolved(contextA);
      assertIsResolved(contextC);

      resetFlags();
      classLoading.refreshModules(moduleA);
      assertNotResolved(contextA);
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertFailedBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesUninstalledCascaded() throws Exception
   {
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      assertNotResolved(contextC);

      resetFlags();
      classLoading.refreshModules(moduleA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNoBounce(contextC);
      assertNotResolved(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesUninstalledNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      assertIsResolved(contextC);

      resetFlags();
      classLoading.refreshModules(moduleA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertFailedBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesAllCascaded() throws Exception
   {
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      assertNotResolved(contextC);

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesAllNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      assertIsResolved(contextC);

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertFailedBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);

      // Shouldn't do it twice
      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesAllMultipleCascaded() throws Exception
   {
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      uninstall(contextB);
      assertIsResolved(contextA2);
      assertNotResolved(contextC);
      assertNotResolved(contextD);
      assertNotResolved(contextE);

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesAllMultipleNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();
      
      resetFlags();
      uninstall(contextA);
      uninstall(contextB);
      assertIsResolved(contextA2);
      assertIsResolved(contextC);
      assertIsResolved(contextD);
      assertIsResolved(contextE);

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertFailedBounce(contextC);
      assertFailedBounce(contextD);
      assertFailedBounce(contextE);

      // Shouldn't do it twice
      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA2);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesReinstalledCascaded() throws Exception
   {
      setUpABCDE();

      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      
      resetFlags();
      uninstall(contextA);
      assertNotResolved(contextC);

      setUpA();
      assertResolved(contextA);
      assertResolved(contextC);

      ExportPackage result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertEquals(moduleA, result.getModule());

      resetFlags();
      classLoading.refreshModules(moduleA);
      assertBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertReResolved(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);

      result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertEquals(moduleA, result.getModule());
   }
   
   public void testRefreshModulesReinstalledNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();

      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      
      resetFlags();
      Module oldModuleA = moduleA;
      uninstall(contextA);
      assertIsResolved(contextC);

      setUpA();
      assertResolved(contextA);
      assertNoBounce(contextC);

      ExportPackage result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"));
      assertEquals(moduleA, result.getModule());
      
      resetFlags();
      classLoading.refreshModules(oldModuleA);
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
   }
   
   public void testRefreshModulesAllReinstalledCascaded() throws Exception
   {
      setUpABCDE();

      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      
      resetFlags();
      uninstall(contextA);
      assertNotResolved(contextC);

      setUpA();
      assertResolved(contextA);
      assertResolved(contextC);

      ExportPackage result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertEquals(moduleA, result.getModule());

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);

      result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertEquals(moduleA, result.getModule());
   }
   
   public void testRefreshModulesAllReinstalledNonCascaded() throws Exception
   {
      lazyShutdown = true;
      setUpABCDE();

      assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      
      resetFlags();
      uninstall(contextA);
      assertIsResolved(contextC);

      setUpA();
      assertResolved(contextA);
      assertNoBounce(contextC);

      ExportPackage result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"));
      assertEquals(moduleA, result.getModule());

      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);
      
      // Should not do it twice
      resetFlags();
      classLoading.refreshModules();
      assertNoBounce(contextA);
      assertNoBounce(contextA2);
      assertNoBounce(contextB);
      assertNoBounce(contextC);
      assertNoBounce(contextD);
      assertNoBounce(contextE);

      result = assertExportedPackageImporting(PACKAGEA, VersionRange.valueOf("1.0.0"), moduleC);
      assertEquals(moduleA, result.getModule());
   }

   protected void assertBounce(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifecycle = assertLifeCycle(context);
      assertTrue(context.getName() + " should have bounced", lifecycle.gotBounce);
      assertTrue(context.getName() + " should have unresolved", lifecycle.gotUnresolved);
      assertResolved(context);
   }

   protected void assertReResolved(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifecycle = assertLifeCycle(context);
      assertTrue(context.getName() + " should have unresolved", lifecycle.gotUnresolved);
      assertResolved(context);
   }

   protected void assertFailedBounce(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifecycle = assertLifeCycle(context);
      assertTrue(context.getName() + " should have bounced", lifecycle.gotBounce);
      assertTrue(context.getName() + " should have unresolved", lifecycle.gotUnresolved);
      assertNotResolved(context);
   }

   protected void assertNoBounce(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifecycle = assertLifeCycle(context);
      assertFalse(context.getName() + " should not have bounced", lifecycle.gotBounce);
      assertFalse(context.getName() + " should not have unresolved", lifecycle.gotUnresolved);
      assertFalse(context.getName() + " should not have resolved", lifecycle.gotResolved);
   }
      
   protected ExportPackage assertExportedPackageImporting(String name, VersionRange range, Module... expected) throws Exception
   {
      Collection<ExportPackage> exportPackages = classLoading.getExportedPackages(name, range);
      assertTrue(exportPackages.toString(), exportPackages.size() == 1);
      ExportPackage exportPackage = exportPackages.iterator().next();
      assertModules(exportPackage.getImportingModules(), expected);
      return exportPackage;
   }
   
   protected void assertModules(Collection<Module> actual, Module... expected) throws Exception
   {
      Set<Module> expect = new HashSet<Module>();
      for (Module module : expected)
         expect.add(module);
      
      Set<Module> act = new HashSet<Module>(actual);
      
      assertEquals(expect, act);
   }

   protected void resetFlags()
   {
      lifecycleA.resetFlags();
      lifecycleA2.resetFlags();
      lifecycleB.resetFlags();
      if (lifecycleC != null)
         lifecycleC.resetFlags();
      if (lifecycleD != null)
         lifecycleD.resetFlags();
      if (lifecycleE != null)
         lifecycleE.resetFlags();
   }
}
