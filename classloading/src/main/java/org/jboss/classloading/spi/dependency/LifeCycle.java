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
import org.jboss.classloading.spi.dependency.policy.ClassLoaderPolicyModule;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
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
   
   /** Any lazy start handler */
   private LazyStartHandler lazyStart;
   
   /** Whether we are already in the lifecycle */
   // TODO FIX THIS IN THE MC?
   private boolean lifeCycle = false;
   
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
      return false;
   }
   
   /**
    * Resolve the classloader
    */
   void doResolve()
   {
      if (lifeCycle == false)
      {
         lifeCycle = true;
         try
         {
            resolve();
         }
         catch (Throwable t)
         {
            log.warn("Error in resolve: " + this, t);
         }
         finally
         {
            lifeCycle = false;
         }
      }
   }
   
   /**
    * Resolve the classloader
    */
   public void resolve()
   {
   }
   
   /**
    * Unresolve the classloader
    */
   public void unresolve()
   {
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
    * Whether the module is started
    * 
    * @return true when started
    */
   public boolean isStarted()
   {
      ControllerContext context = module.getControllerContext();
      if (context == null)
         return false;
      return ControllerState.INSTALLED.equals(context.getState());
   }
   
   /**
    * Start the context associated with the classloader
    */
   void doStart()
   {
      if (lifeCycle == false)
      {
         lifeCycle = true;
         try
         {
            start();
         }
         catch (Throwable t)
         {
            log.warn("Error in start: " + this, t);
         }
         finally
         {
            lifeCycle = false;
         }
      }
   }
   
   /**
    * Start the context associated with the classloader
    */
   public void start()
   {
   }
   
   /**
    * Stop the context associated with the classloader
    */
   void doStop()
   {
      if (lifeCycle == false)
      {
         lifeCycle = true;
         try
         {
            start();
         }
         catch (Throwable t)
         {
            log.warn("Error in stop: " + this, t);
         }
         finally
         {
            lifeCycle = false;
         }
      }
   }
   
   /**
    * Stop the context associated with the classloader
    */
   public void stop()
   {
   }
   
   /**
    * Whether the context associated with the classloader is lazy start,
    * i.e. the start method will be invoked on first class load
    * 
    * @return true if it is lazy start
    */
   public boolean isLazyStart()
   {
      return false;
   }

   /**
    * Setup lazy start
    */
   protected void setUpLazyStart()
   {
      if (isStarted())
         return;
      if (module instanceof ClassLoaderPolicyModule)
      {
         ClassLoaderPolicy policy = ((ClassLoaderPolicyModule) module).getPolicy();
         lazyStart = new LazyStartHandler(policy);
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
      if (lazyStart == null)
         return;

      lazyStart.cleanup();
      lazyStart = null;
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
         removeLazyStart();
         if (isStarted() == false)
            start();
      }
      
      public void cleanup()
      {
         policy.removeClassFoundHandler(this);
      }
   }
}
