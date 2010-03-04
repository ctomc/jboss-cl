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
package org.jboss.classloading.spi.dependency;

import org.jboss.classloader.spi.ClassFoundEvent;
import org.jboss.classloader.spi.ClassFoundHandler;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.ClassFilterUtils;
import org.jboss.classloading.spi.dependency.policy.ClassLoaderPolicyModule;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.ControllerStateModel;
import org.jboss.logging.Logger;

/**
 * Lifecycle.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class LifeCycle
{
   /** The log */
   private static final Logger log = Logger.getLogger(LifeCycle.class);
   
   /** The module associated with this lifecycle */
   private Module module;
   
   /** Whether to lazy start */
   private boolean lazyStart = false;
   
   /** Whether to lazy resolve */
   private boolean lazyResolve = false;
   
   /** Any lazy start handler */
   private LazyStartHandler lazyStartHandler;
   
   /** The lazy start filter */
   private ClassFilter lazyStartFilter = ClassFilterUtils.EVERYTHING;
   
   /**
    * Create a new LifeCycle.
    * 
    * @param module the module associated with the lifecycle
    */
   public LifeCycle(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");
      this.module = module;
   }
   
   /**
    * Get the module.
    * 
    * @return the module.
    */
   public Module getModule()
   {
      return module;
   }
   
   /**
    * Whether the context associated with the classloader is lazy start,
    * i.e. the start method will be invoked on first class load
    * 
    * @return true if it is lazy start
    */
   public boolean isLazyStart()
   {
      return lazyStart;
   }

   /**
    * Set the lazyStart 
    *
    * @param lazyStart the lazyStart to set
    */
   public void setLazyStart(boolean lazyStart)
   {
      this.lazyStart = lazyStart;
      if (lazyStart)
         setUpLazyStart();
   }

   /**
    * Get the lazyStartFilter
    *
    * @return the lazyStartFilter
    */
   public ClassFilter getLazyStartFilter()
   {
      return lazyStartFilter;
   }

   /**
    * Set the lazyStartFilter 
    *
    * @param lazyStartFilter the lazyStartFilter to set
    */
   public void setLazyStartFilter(ClassFilter lazyStartFilter)
   {
      if (lazyStartFilter == null)
         lazyStartFilter = ClassFilterUtils.EVERYTHING;
      this.lazyStartFilter = lazyStartFilter;
   }

   /**
    * Whether the module is resolved
    * 
    * @return true when resolved
    */
   public boolean isResolved()
   {
      return module.getClassLoader() != null;
   }
   
   /**
    * Whether the context associated with the classloader is lazy resolve,
    * i.e. the resolve method will be invoked the context is needed
    * 
    * @return true if it is lazy resolve
    */
   public boolean isLazyResolve()
   {
      return lazyResolve;
   }
   
   /**
    * Set the lazyResolve 
    *
    * @param lazyResolve the lazyResolve to set
    */
   public void setLazyResolve(boolean lazyResolve)
   {
      this.lazyResolve = lazyResolve;
   }

   /**
    * Resolve the classloader
    * 
    * @return true if it is actually resolved
    * @throws Exception for any error
    */
   public boolean resolve() throws Exception
   {
      return true;
   }
   
   /**
    * Resolve lots of lifecycles
    * 
    * @param lifecycles the lifecycles to resolve
    * @return true if they are all resolved
    * @throws Exception for any error
    */
   public boolean resolve(LifeCycle... lifecycles) throws Exception
   {
      if (lifecycles == null || lifecycles.length == 0)
         return true;
      boolean result = true;
      for (LifeCycle lifecycle : lifecycles)
      {
         if (lifecycle.isResolved() == false)
         {
            if (lifecycle.resolve() == false)
               result = false;
         }
      }
      return result;
   }
   
   /**
    * Unresolve the classloader
    * 
    * @throws Exception for any error
    */
   public void unresolve() throws Exception
   {
   }
   
   /**
    * Unresolve lots of lifecycles
    * 
    * @param lifecycles the lifecycles to unresolve
    * @throws Exception for any error
    */
   public void unresolve(LifeCycle... lifecycles) throws Exception
   {
      if (lifecycles == null || lifecycles.length == 0)
         return;
      for (LifeCycle lifecycle : lifecycles)
      {
         if (lifecycle.isResolved())
            lifecycle.unresolve();
      }
   }
   
   /**
    * Fired when the classloader is resolved
    */
   public void resolved()
   {
   }
   
   /**
    * Fired when the classloader is unresolved
    */
   public void unresolved()
   {
   }
   
   /**
    * Bounce the classloader
    * 
    * @throws Exception for any error
    */
   public void bounce() throws Exception
   {
   }
   
   /**
    * Bounce lots of lifecycles
    * 
    * @param lifecycles the lifecycles to bounce
    * @throws Exception for any error
    */
   public void bounce(LifeCycle... lifecycles) throws Exception
   {
      if (lifecycles == null || lifecycles.length == 0)
         return;
      for (LifeCycle lifecycle : lifecycles)
         lifecycle.bounce();
   }
   
   /**
    * Whether the module is started
    * 
    * @return true when started
    */
   public boolean isStarted()
   {
      ControllerContext context = module.getControllerContext();
      if (context == null)
         return false;

      Controller controller = context.getController();
      ControllerStateModel model = controller.getStates();
      return model.isBeforeState(context.getState(), ControllerState.INSTALLED) == false;
   }
   
   /**
    * Start the context associated with the classloader
    * 
    * @throws Exception for any error
    */
   public void start() throws Exception
   {
   }
   
   /**
    * Start lots of lifecycles
    * 
    * @param lifecycles the lifecycles to start
    * @throws Exception for any error
    */
   public void start(LifeCycle... lifecycles) throws Exception
   {
      if (lifecycles == null || lifecycles.length == 0)
         return;
      for (LifeCycle lifecycle : lifecycles)
      {
         if (lifecycle.isStarted() == false)
            lifecycle.start();
      }
   }
   
   /**
    * Stop the context associated with the classloader
    * 
    * @throws Exception for any error
    */
   public void stop() throws Exception
   {
   }
   
   /**
    * Stop lots of lifecycles
    * 
    * @param lifecycles the lifecycles to stop
    * @throws Exception for any error
    */
   public void stop(LifeCycle... lifecycles) throws Exception
   {
      if (lifecycles == null || lifecycles.length == 0)
         return;
      for (LifeCycle lifecycle : lifecycles)
      {
         if (lifecycle.isResolved())
            lifecycle.stop();
      }
   }

   /**
    * Setup lazy start
    */
   protected void setUpLazyStart()
   {
      if (isResolved() == false || isStarted())
         return;
      if (lazyStartHandler != null)
         return;
      if (module instanceof ClassLoaderPolicyModule)
      {
         ClassLoaderPolicy policy = ((ClassLoaderPolicyModule) module).getPolicy();
         lazyStartHandler = new LazyStartHandler(policy);
      }
      else
      {
         throw new IllegalStateException("Cannot do lazy start for " + module);
      }
   }

   /**
    * Remove lazy start
    */
   protected void removeLazyStart()
   {
      if (lazyStartHandler == null)
         return;

      lazyStartHandler.cleanup();
      lazyStartHandler = null;
   }
   
   void fireResolved()
   {
      if (isLazyStart())
         setUpLazyStart();
      resolved();
   }
   
   void fireUnresolved()
   {
      removeLazyStart();
      unresolved();
   }

   @Override
   public String toString()
   {
      return getClass().getName() + "{" + getModule() + "}";
   }
   
   /**
    * LazyStartHandler.
    */
   private class LazyStartHandler implements ClassFoundHandler
   {
      ClassLoaderPolicy policy;
      
      public LazyStartHandler(ClassLoaderPolicy policy)
      {
         this.policy = policy;
         policy.addClassFoundHandler(this);
      }
      
      public void classFound(ClassFoundEvent event)
      {
         if (getLazyStartFilter().matchesClassName(event.getClassName()) == false)
            return;
         
         removeLazyStart();
         if (isStarted() == false)
         {
            try
            {
               start();
            }
            catch (Throwable t)
            {
               log.warn("Error in lazy start for " + this, t);
            }
         }
      }
      
      public void cleanup()
      {
         policy.removeClassFoundHandler(this);
      }
   }
}
