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

import org.jboss.classloading.spi.dependency.LifeCycle;
import org.jboss.classloading.spi.dependency.Module;

/**
 * MockLifeCycle.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockLifeCycle extends LifeCycle
{
   public boolean gotResolved = false;
   public boolean gotUnresolved = false;
   public boolean gotResolve = false;
   public boolean gotUnresolve = false;
   public boolean gotStart= false;
   public boolean gotStop = false;

   public boolean lazyResolve = false;
   public boolean lazyStart = false;
   
   public MockLifeCycle(Module module)
   {
      super(module);
   }

   public MockLifeCycleClassLoaderPolicyModule getModule()
   {
      return (MockLifeCycleClassLoaderPolicyModule) super.getModule();
   }

   public void resetFlags()
   {
      gotResolved = false;
      gotUnresolved = false;
      gotResolve = false;
      gotUnresolve = false;
      gotStart = false;
      gotStop = false;
   }
   
   @Override
   public void resolve()
   {
      gotResolve = true;
      getModule().resolveIt();
   }

   @Override
   public void resolved()
   {
      gotResolved = true;
   }

   @Override
   public void start()
   {
      gotStart = true;
   }

   @Override
   public void stop()
   {
      gotStop = true;
   }

   @Override
   public void unresolve()
   {
      gotUnresolve = true;
   }

   @Override
   public void unresolved()
   {
      gotUnresolved = true;
   }

   @Override
   public boolean isLazyResolve()
   {
      return lazyResolve;
   }

   @Override
   public boolean isLazyStart()
   {
      return lazyStart;
   }
}
