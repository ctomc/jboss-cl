/*
* JBoss, Home of Professional Open Source
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
package org.jboss.test.classloading.lifecycle.support.a;

import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoaderPolicyModule;
import org.jboss.classloading.spi.dependency.policy.mock.MockClassLoadingMetaData;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;

/**
 * MockLifeCycleClassLoaderPolicyModule.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockLifeCycleClassLoaderPolicyModule extends MockClassLoaderPolicyModule
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   public MockLifeCycleClassLoaderPolicyModule(MockClassLoadingMetaData classLoadingMetaData, String contextName)
   {
      super(classLoadingMetaData, contextName);
      MockLifeCycle lifeCycle = new MockLifeCycle(this);
      setLifeCycle(lifeCycle);
   }
   
   @Override
   public ClassLoader getClassLoader()
   {
      return super.getClassLoader();
   }
   
   @Override
   public ControllerState getClassLoaderState()
   {
      return ControllerState.CREATE;
   }
   
   void resolveIt()
   {
      ControllerContext context = getControllerContext();
      Controller controller = context.getController();
      try
      {
         controller.change(context, getClassLoaderState());
      }
      catch (Throwable t)
      {
         throw new Error("Error", t);
      }
   }
   
   void unresolveIt()
   {
      ControllerContext context = getControllerContext();
      Controller controller = context.getController();
      try
      {
         controller.change(context, ControllerState.CONFIGURED);
      }
      catch (Throwable t)
      {
         throw new Error("Error", t);
      }
   }
}
