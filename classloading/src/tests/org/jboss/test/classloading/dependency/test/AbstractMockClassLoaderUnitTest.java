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
package org.jboss.test.classloading.dependency.test;

import junit.framework.Test;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoaderPolicyModule;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.ControllerStateModel;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.AbstractBootstrap;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.classloading.AbstractClassLoadingTest;

/**
 * AbstractMockClassLoaderUnitTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractMockClassLoaderUnitTest extends AbstractClassLoadingTest
{
   private KernelController controller;

   protected ClassLoaderSystem system;
   
   public static Test suite()
   {
      return suite(AbstractMockClassLoaderUnitTest.class);
   }

   public AbstractMockClassLoaderUnitTest(String name)
   {
      super(name);
   }
   
   protected ClassLoader assertClassLoader(KernelControllerContext context) throws Exception
   {
      MockClassLoaderPolicyModule module = assertModule(context);
      return module.registerClassLoaderPolicy(system);
   }
   
   protected MockClassLoaderPolicyModule assertModule(KernelControllerContext context) throws Exception
   {
      ControllerStateModel states = context.getController().getStates();
      ControllerState state = context.getState();
      ControllerState next = states.getNextState(state);
      assertEquals(context.getName() + " should be installed: " + state + " unresolved=" + context.getDependencyInfo().getUnresolvedDependencies(next), ControllerState.INSTALLED, context.getState());
      Object target = context.getTarget();
      assertNotNull(target);
      return assertInstanceOf(target, MockClassLoaderPolicyModule.class);
   }

   protected void assertNoClassLoader(KernelControllerContext context) throws Exception
   {
      assertNoModule(context);
   }

   protected void assertNoModule(KernelControllerContext context) throws Exception
   {
      boolean test = ControllerState.INSTALLED.equals(context.getState());
      if (test)
      {
         MockClassLoaderPolicyModule module = assertModule(context);
         Object target = context.getTarget();
         assertNotNull(target);
         fail("Should not be able to create classloader: " + module.registerClassLoaderPolicy(system));
      }
   }
   
   protected KernelControllerContext install(MockClassLoadingMetaData metaData) throws Exception
   {
      return install(metaData, MockClassLoaderPolicyModule.class);
   }

   protected KernelControllerContext install(MockClassLoadingMetaData metaData, Class<? extends MockClassLoaderPolicyModule> clazz) throws Exception
   {
      // Determine some properties
      String contextName = metaData.getName() + ":" + metaData.getVersion().toString(); 
      
      // Create the module
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(contextName, clazz.getName());
      builder.addConstructorParameter(MockClassLoadingMetaData.class.getName(), metaData);
      builder.addConstructorParameter(String.class.getName(), contextName);
      builder.setNoClassLoader();
      builder.addUninstall("removeClassLoader");
      BeanMetaData module = builder.getBeanMetaData();
      return install(module);
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
