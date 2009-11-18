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

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.dependency.spi.ControllerMode;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.AbstractBootstrap;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.AbstractClassLoadingTest;
import org.jboss.test.classloading.lifecycle.support.a.MockLifeCycle;
import org.jboss.test.classloading.lifecycle.support.a.MockLifeCycleClassLoaderPolicyModule;

/**
 * AbstractMockLifeCycleUnitTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractMockLifeCycleUnitTest extends AbstractClassLoadingTest
{
   private KernelController controller;

   protected ClassLoaderSystem system;

   public AbstractMockLifeCycleUnitTest(String name)
   {
      super(name);
   }

   protected void assertNoClassLoader(KernelControllerContext context) throws Exception
   {
      MockLifeCycleClassLoaderPolicyModule module = assertMockClassPolicyModule(context);
      ClassLoader cl = module.getClassLoader();
      assertNull("" + cl, cl);
   }

   protected ClassLoader assertClassLoader(KernelControllerContext context) throws Exception
   {
      MockLifeCycleClassLoaderPolicyModule module = assertMockClassPolicyModule(context);
      ClassLoader cl = module.getClassLoader();
      assertNotNull("Should be a classloader for " + module, cl);
      return cl;
   }
   
   protected MockLifeCycle assertNotResolved(KernelControllerContext context) throws Exception
   {
      assertNoClassLoader(context);
      MockLifeCycle lifeCycle = assertLifeCycle(context);
      assertFalse(context.getName() + " should be unresolved", lifeCycle.isResolved());
      assertFalse(context.getName() + " should be no resolved notification", lifeCycle.gotResolved);
      assertFalse(context.getName() + " should not be started", lifeCycle.isStarted());
      return lifeCycle;
   }
   
   protected MockLifeCycle assertUnresolved(KernelControllerContext context) throws Exception
   {
      assertNoClassLoader(context);
      MockLifeCycle lifeCycle = assertLifeCycle(context);
      assertFalse(context.getName() + " should be unresolved", lifeCycle.isResolved());
      assertTrue(context.getName() + " should be an unresolved notification", lifeCycle.gotUnresolved);
      assertFalse(context.getName() + " should not be started", lifeCycle.isStarted());
      return lifeCycle;
   }
   
   protected ClassLoader assertResolved(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifeCycle = assertLifeCycle(context);
      assertTrue(context.getName() + " should be resolved: " + context.getDependencyInfo().getUnresolvedDependencies(null), lifeCycle.isResolved());
      assertTrue(context.getName() + " should be a resolved notification", lifeCycle.gotResolved);
      assertFalse(context.getName() + " should not be started", lifeCycle.isStarted());
      return assertClassLoader(context);
   }
   
   protected ClassLoader assertStarted(KernelControllerContext context) throws Exception
   {
      MockLifeCycle lifeCycle = assertLifeCycle(context);
      assertTrue(context.getName() + " should be resolved: " + context.getDependencyInfo().getUnresolvedDependencies(null), lifeCycle.isResolved());
      assertTrue(context.getName() + " should be started", lifeCycle.isStarted());
      assertEquals(ControllerState.INSTALLED, context.getState());
      return assertClassLoader(context);
   }

   protected MockLifeCycle assertLifeCycle(KernelControllerContext context) throws Exception
   {
      MockLifeCycleClassLoaderPolicyModule module = assertMockClassPolicyModule(context);
      MockLifeCycle result = (MockLifeCycle) module.getLifeCycle();
      assertNotNull(result);
      return result;
   }

   protected MockLifeCycleClassLoaderPolicyModule assertMockClassPolicyModule(KernelControllerContext context) throws Exception
   {
      MockLifeCycleClassLoaderPolicyModule module = (MockLifeCycleClassLoaderPolicyModule) context.getTarget();
      assertNotNull("Should be a module for " + context, module);
      return module;
   }

   protected KernelControllerContext install(MockClassLoadingMetaData metaData) throws Exception
   {
      // Determine some properties
      String contextName = metaData.getName() + ":" + metaData.getVersion().toString(); 
      
      // Create the module
      MockLifeCycleClassLoaderPolicyModule mockModule = new MockLifeCycleClassLoaderPolicyModule(metaData, contextName);
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(contextName, mockModule.getClass().getName());
      builder.setConstructorValue(mockModule);
      builder.setMode(ControllerMode.MANUAL);
      builder.addConstructorParameter(MockClassLoadingMetaData.class.getName(), metaData);
      builder.addConstructorParameter(String.class.getName(), contextName);
      builder.setNoClassLoader();
      builder.setCreate("registerClassLoaderPolicy");
      builder.addCreateParameter(ClassLoaderSystem.class.getName(), builder.createValue(system));
      builder.setDestroy("removeClassLoader");
      BeanMetaData module = builder.getBeanMetaData();
      KernelControllerContext result = install(module);
      change(result, ControllerState.CONFIGURED);
      return result;
   }
   
   protected KernelControllerContext install(BeanMetaData beanMetaData) throws Exception
   {
      try
      {
         return controller.install(beanMetaData);
      }
      catch (Exception e)
      {
         throw e;
      }
      catch (Throwable t)
      {
         throw new RuntimeException("Error during install: " + beanMetaData, t);
      }
   }
   
   protected void change(KernelControllerContext controllerContext, ControllerState state) throws Exception
   {
      try
      {
         controller.change(controllerContext, state);
      }
      catch (Exception e)
      {
         throw e;
      }
      catch (Throwable t)
      {
         throw new RuntimeException("Error during change: " + controllerContext, t);
      }
   }
   
   protected void install(KernelControllerContext controllerContext) throws Exception
   {
      change(controllerContext, ControllerState.CONFIGURED);
   }
   
   protected void resolve(KernelControllerContext controllerContext) throws Exception
   {
      change(controllerContext, ControllerState.CREATE);
   }
   
   protected void unresolve(KernelControllerContext controllerContext) throws Exception
   {
      change(controllerContext, ControllerState.CONFIGURED);
   }
   
   protected void start(KernelControllerContext controllerContext) throws Exception
   {
      change(controllerContext, ControllerState.INSTALLED);
   }
   
   protected void stop(KernelControllerContext controllerContext) throws Exception
   {
      change(controllerContext, ControllerState.CREATE);
   }

   protected void uninstall(KernelControllerContext context)
   {
      controller.uninstall(context.getName());
   }
   
   protected void setUp() throws Exception
   {
      super.setUp();
      
      // Bootstrap the kernel
      AbstractBootstrap bootstrap = new BasicBootstrap();
      bootstrap.run();
      Kernel kernel = bootstrap.getKernel();
      controller = kernel.getController();

      system = new DefaultClassLoaderSystem();
      ClassLoaderDomain defaultDomain = system.getDefaultDomain();
      defaultDomain.setParentPolicy(ParentPolicy.BEFORE_BUT_JAVA_ONLY);

      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("ClassLoading", ClassLoading.class.getName());
      builder.addMethodInstallCallback("addModule", null, null, ControllerState.CONFIGURED, null);
      builder.addMethodUninstallCallback("removeModule", null, null, ControllerState.CONFIGURED, null);

      install(builder.getBeanMetaData());
   }

   protected void tearDown() throws Exception
   {
      controller.shutdown();
      super.tearDown();
   }
}
