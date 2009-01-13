/*
* JBoss, Home of Professional Open Source
* Copyright 2007, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.classloading.spi.dependency.policy;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.classloader.plugins.loader.ClassLoaderToLoaderAdapter;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.Loader;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.filter.LazyFilteredDelegateLoader;
import org.jboss.classloading.spi.dependency.Domain;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.RequirementDependencyItem;
import org.jboss.classloading.spi.dependency.helpers.ClassLoadingMetaDataModule;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;

/**
 * ClassLoaderPolicyModule.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class ClassLoaderPolicyModule extends ClassLoadingMetaDataModule
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -3357427104777457717L;
   
   /** Our cached policy */
   private ClassLoaderPolicy policy;
   
   /** The classloader system we are registered with */
   private ClassLoaderSystem system;
   
   /** The classloader */
   private ClassLoader classLoader;

   /**
    * Create a new ClassLoaderPolicyModule.
    * 
    * @param classLoadingMetaData the classloading metadata
    * @param contextName the context name
    */
   public ClassLoaderPolicyModule(ClassLoadingMetaData classLoadingMetaData, String contextName)
   {
      super(classLoadingMetaData, contextName);
   }

   @Override
   protected ClassLoader getClassLoaderForClass(final String className) throws ClassNotFoundException
   {
      if (classLoader == null)
         throw new IllegalStateException("No classloader for module " + this);
      
      if (classLoader instanceof BaseClassLoader == false)
         return super.getClassLoaderForClass(className);
      
      final BaseClassLoader bcl = (BaseClassLoader) classLoader;
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         try
         {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>()
            {
               public ClassLoader run() throws Exception
               {
                  return bcl.findClassLoader(className); 
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            Throwable t = e.getCause();
            if (t instanceof ClassNotFoundException)
               throw (ClassNotFoundException) t;
            if (t instanceof Error)
               throw (Error) t;
            if (t instanceof RuntimeException)
               throw (RuntimeException) t;
            throw new RuntimeException("Error during findClassLoader for " + className, e);
         }
      }
      return bcl.findClassLoader(className); 
   }

   /**
    * Register the classloader policy with a classloader system
    *
    * @param system the classloader system
    * @return the classloader
    */
   public ClassLoader registerClassLoaderPolicy(ClassLoaderSystem system)
   {
      if (system == null)
         throw new IllegalArgumentException("Null classloader system");

      if (isValid() == false)
         throw new IllegalStateException("Module " + this + " is not registered, see previous error messages");
      
      String domainName = getDeterminedDomainName();
      ParentPolicy parentPolicy = getDeterminedParentPolicy();
      String parentName = getDeterminedParentDomainName();
      ClassLoader result = system.registerClassLoaderPolicy(domainName, parentPolicy, parentName, getPolicy());
      this.system = system;
      this.classLoader = result;
      registerModuleClassLoader(this, result);
      return result;
   }
   
   /**
    * Register the classloader policy with a classloader system
    *
    * @param system the classloader system
    * @param parent the parent classloader
    * @return the classloader
    */
   public ClassLoader registerClassLoaderPolicy(ClassLoaderSystem system, ClassLoader parent)
   {
      if (system == null)
         throw new IllegalArgumentException("Null classloader system");
      if (parent == null)
         throw new IllegalArgumentException("Null parent");

      if (isValid() == false)
         throw new IllegalStateException("Module " + this + " is not registered, see previous error messages");

      Loader loader = new ClassLoaderToLoaderAdapter(parent);
      ClassLoader result = registerClassLoaderPolicy(system, loader); 
      this.classLoader = result;
      registerModuleClassLoader(this, result);
      return result;
   }
   
   /**
    * Register the classloader policy with a classloader system
    *
    * @param system the classloader system
    * @param loader the parent loader
    * @return the classloader
    */
   public ClassLoader registerClassLoaderPolicy(ClassLoaderSystem system, Loader loader)
   {
      if (system == null)
         throw new IllegalArgumentException("Null classloader system");

      if (isValid() == false)
         throw new IllegalStateException("Module " + this + " is not registered, see previous error messages");
      
      String domainName = getDeterminedDomainName();
      ParentPolicy parentPolicy = getDeterminedParentPolicy();
      ClassLoader result = system.registerClassLoaderPolicy(domainName, parentPolicy, loader, getPolicy());
      this.system = system;
      this.classLoader = result;
      registerModuleClassLoader(this, result);
      return result;
   }

   /**
    * Get the policy
    * 
    * @return the policy
    */
   public ClassLoaderPolicy getPolicy()
   {
      if (policy != null)
         return policy;
      policy = determinePolicy();
      return policy;
   }

   public void removeClassLoader()
   {
      if (system != null && policy != null)
         system.unregisterClassLoaderPolicy(policy);
      if (classLoader != null)
         unregisterModuleClassLoader(this, classLoader);
      classLoader = null;
      system = null;
      policy = null;
   }
   
   /**
    * Determine the classloader policy
    * 
    * @return the policy
    */
   protected abstract ClassLoaderPolicy determinePolicy();

   @Override
   protected ClassLoader getClassLoader()
   {
      return classLoader;
   }
   
   @Override
   public DelegateLoader createLazyDelegateLoader(Domain domain, RequirementDependencyItem item)
   {
      ControllerContext context = getControllerContext();
      if (context == null)
         throw new IllegalStateException("No controller context");
      Controller controller = context.getController();
         
      DynamicClassLoaderPolicyFactory factory = new DynamicClassLoaderPolicyFactory(controller, domain, item);
      return new LazyFilteredDelegateLoader(factory);
   }

   @Override
   public DelegateLoader getDelegateLoader(Module requiringModule, Requirement requirement)
   {
      return getPolicy().getExported();
   }

   @Override
   public void reset()
   {
      super.reset();
      system = null;
      policy = null;
   }
}
