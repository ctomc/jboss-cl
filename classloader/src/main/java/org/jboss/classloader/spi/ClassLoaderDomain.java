/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.classloader.spi;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.plugins.loader.ClassLoaderToLoaderAdapter;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.base.BaseClassLoaderDomain;
import org.jboss.classloader.spi.base.BaseClassLoaderSource;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.spi.RealClassLoader;
import org.jboss.logging.Logger;

/**
 * ClassLoaderDomain.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoaderDomain extends BaseClassLoaderDomain implements ClassLoaderDomainMBean, MBeanRegistration, ClassNotFoundHandler, ClassFoundHandler
{
   /** The log */
   private static final Logger log = Logger.getLogger(ClassLoaderDomain.class);
   
   /** The name of the domain */
   private String name;

   /** The parent classloading rules */
   private ParentPolicy parentPolicy = ParentPolicy.BEFORE; 
   
   /** The parent */
   private Loader parent;
   
   /** The shutdown policy */
   private ShutdownPolicy shutdownPolicy;

   /** The MBeanServer */
   private MBeanServer mbeanServer;
   
   /** The object name */
   private ObjectName objectName;
   
   /** Whether to use load class for the parent */
   private boolean useLoadClassForParent = false;

   /** The class not found handlers */
   private List<ClassNotFoundHandler> classNotFoundHandlers;

   /** The class found handlers */
   private List<ClassFoundHandler> classFoundHandlers;

   /** The class loader event handlers */
   private List<ClassLoaderEventHandler> classLoaderEventHandlers;
   
   /**
    * Create a new ClassLoaderDomain with the {@link ParentPolicy#BEFORE} loading rules.
    * 
    * @param name the name of the domain
    * @throws IllegalArgumentException for a null name
    */
   public ClassLoaderDomain(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      this.name = name;
      fixUpParent();
   }
   
   /**
    * Get the name.
    * 
    * @return the name.
    */
   public String getName()
   {
      return name;
   }
   
   /**
    * Get the object name
    * 
    * @return the object name
    */
   public ObjectName getObjectName()
   {
      return objectName;
   }
   
   /**
    * Get the parent policy
    * 
    * @return the parent policy.
    */
   public ParentPolicy getParentPolicy()
   {
      return parentPolicy;
   }
   
   /**
    * Set the parentPolicy.
    * 
    * @param parentPolicy the parentPolicy.
    * @throws IllegalArgumentException for a null parent policy
    */
   public void setParentPolicy(ParentPolicy parentPolicy)
   {
      if (parentPolicy == null)
         throw new IllegalArgumentException("Null parent policy");
      this.parentPolicy = parentPolicy;
   }

   public String getParentPolicyName()
   {
      return parentPolicy.toString();
   }

   /**
    * Get the shutdownPolicy.
    * 
    * @return the shutdownPolicy.
    */
   public ShutdownPolicy getShutdownPolicy()
   {
      return shutdownPolicy;
   }

   /**
    * Set the shutdownPolicy.
    * 
    * @param shutdownPolicy the shutdownPolicy.
    */
   public void setShutdownPolicy(ShutdownPolicy shutdownPolicy)
   {
      this.shutdownPolicy = shutdownPolicy;
   }

   /**
    * Get the parent
    * 
    * @return the parent.
    */
   public Loader getParent()
   {
      return parent;
   }

   /**
    * Set the parent.
    * 
    * @param parent the parent.
    */
   public void setParent(Loader parent)
   {
      this.parent = parent;
      fixUpParent();
   }

   /**
    * Get the useLoadClassForParent.
    * 
    * @return the useLoadClassForParent.
    */
   public boolean isUseLoadClassForParent()
   {
      return useLoadClassForParent;
   }

   /**
    * Set the useLoadClassForParent.
    * 
    * @param useLoadClassForParent the useLoadClassForParent.
    */
   public void setUseLoadClassForParent(boolean useLoadClassForParent)
   {
      this.useLoadClassForParent = useLoadClassForParent;
   }

   public ObjectName getParentDomain()
   {
      if (parent == null || parent instanceof ClassLoaderDomain == false)
         return null;
      ClassLoaderDomain parentDomain = (ClassLoaderDomain) parent;
      return parentDomain.getObjectName();
   }

   public String getParentDomainName()
   {
      if (parent == null || parent instanceof ClassLoaderDomain == false)
         return null;
      ClassLoaderDomain parentDomain = (ClassLoaderDomain) parent;
      return parentDomain.getName();
   }

   public ObjectName getSystem()
   {
      ClassLoaderSystem system = (ClassLoaderSystem) getClassLoaderSystem();
      if (system == null)
         return null;
      return system.getObjectName();
   }

   public List<ObjectName> listClassLoaders()
   {
      List<ObjectName> result = new ArrayList<ObjectName>();
      for (ClassLoader cl : super.getAllClassLoaders())
      {
         if (cl instanceof RealClassLoader)
            result.add(((RealClassLoader) cl).getObjectName());
      }
      return result;
   }

   public Map<String, List<ObjectName>> listExportingClassLoaders()
   {
      HashMap<String, List<ObjectName>> result = new HashMap<String, List<ObjectName>>();
      for (Entry<String, List<ClassLoader>> entry : getClassLoadersByPackage().entrySet())
      {
         List<ObjectName> names = new ArrayList<ObjectName>();
         for (ClassLoader cl : entry.getValue())
         {
            if (cl instanceof RealClassLoader)
               names.add(((RealClassLoader) cl).getObjectName());
            
         }
         result.put(entry.getKey(), names);
      }
      return result;
   }

   public List<ObjectName> listExportingClassLoaders(String packageName)
   {
      if (packageName == null)
         throw new IllegalArgumentException("Null package name");
      
      List<ObjectName> result = new ArrayList<ObjectName>();
      for (ClassLoader cl : getClassLoaders(packageName))
      {
         if (cl instanceof RealClassLoader)
            result.add(((RealClassLoader) cl).getObjectName());
      }
      return result;
   }

   public ObjectName findClassLoaderForClass(String name) throws ClassNotFoundException
   {
      final Class<?> clazz = loadClass(null, name, true);
      if (clazz == null)
         return null;
      
      ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return clazz.getClassLoader(); 
         }
      });
      
      if (cl != null && cl instanceof RealClassLoader)
         return ((RealClassLoader) cl).getObjectName();
      
      return null;
   }

   // FindBugs: The Set doesn't use equals/hashCode
   public Set<URL> loadResources(String name) throws IOException
   {
      TreeSet<URL> result = new TreeSet<URL>(ClassLoaderUtils.URLComparator.INSTANCE);
      getResources(name, result);
      return result;
   }

   /**
    * For subclasses to add information for toLongString()
    * 
    * @param builder the builder
    */
   protected void toLongString(StringBuilder builder)
   {
      builder.append("name=").append(getName());
      builder.append(" parentPolicy=").append(getParentPolicy());
      builder.append(" parent=");
      Loader parent = getParent();
      if (parent != null)
         builder.append(parent);
      else
         builder.append(getParentClassLoader());
   }
   
   /**
    * Add a ClassNotFoundHandler
    * 
    * @param handler the handler
    */
   public void addClassNotFoundHandler(ClassNotFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classNotFoundHandlers == null)
         classNotFoundHandlers = new CopyOnWriteArrayList<ClassNotFoundHandler>();
      
      classNotFoundHandlers.add(handler);
   }
   
   /**
    * Remove a ClassNotFoundHandler
    * 
    * @param handler the handler
    */
   public void removeClassNotFoundHandler(ClassNotFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classNotFoundHandlers == null)
         return;
      classNotFoundHandlers.remove(handler);
   }

   public boolean classNotFound(ClassNotFoundEvent event)
   {
      String className = event.getClassName();

      ClassNotFoundHandler parent = null;
      Loader parentLoader = getParent();
      if (parentLoader instanceof ClassNotFoundHandler)
         parent = (ClassNotFoundHandler) parentLoader;
      else
      {
         ClassLoaderPolicy parentPolicy = getClassLoaderPolicy(parentLoader);
         if (parentPolicy != null)
            parent = parentPolicy;
      }
      
      boolean parentResult = false;
      if (parent != null)
         parentResult = parent.classNotFound(event);

      // Try the parent before
      if (parentResult && getParentPolicy().getBeforeFilter().matchesClassName(className))
         return true;
      
      if (classNotFoundHandlers != null && classNotFoundHandlers.isEmpty() == false)
      {
         for (ClassNotFoundHandler handler : classNotFoundHandlers)
         {
            try
            {
               if (handler.classNotFound(event))
                  return true;
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classNotFoundHandler: " + handler, t);
            }
         }
      }

      // Try the parent after
      if (parentResult && getParentPolicy().getAfterFilter().matchesClassName(className))
         return true;
      
      ClassLoaderSystem system = (ClassLoaderSystem) getClassLoaderSystem();
      return system != null && system.classNotFound(event);
   }
   
   /**
    * Add a ClassFoundHandler
    * 
    * @param handler the handler
    */
   public void addClassFoundHandler(ClassFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classFoundHandlers == null)
         classFoundHandlers = new CopyOnWriteArrayList<ClassFoundHandler>();
      
      classFoundHandlers.add(handler);
   }
   
   /**
    * Remove a ClassFoundHandler
    * 
    * @param handler the handler
    */
   public void removeClassFoundHandler(ClassFoundHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classFoundHandlers == null)
         return;
      classFoundHandlers.remove(handler);
   }

   public void classFound(ClassFoundEvent event)
   {
      ClassFoundHandler parent = null;
      Loader parentLoader = getParent();
      if (parentLoader instanceof ClassFoundHandler)
         parent = (ClassFoundHandler) parentLoader;
      else
      {
         ClassLoaderPolicy parentPolicy = getClassLoaderPolicy(parentLoader);
         if (parentPolicy != null)
            parent = parentPolicy;
      }
      
      if (parent != null)
         parent.classFound(event);
      
      if (classFoundHandlers != null && classFoundHandlers.isEmpty() == false)
      {
         for (ClassFoundHandler handler : classFoundHandlers)
         {
            try
            {
               handler.classFound(event);
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classFoundHandler: " + handler, t);
            }
         }
      }
      
      ClassLoaderSystem system = (ClassLoaderSystem) getClassLoaderSystem();
      if (system != null)
         system.classFound(event);
   }
   
   /**
    * Add a ClassLoaderEventHandler
    * 
    * @param handler the handler
    */
   public void addClassLoaderEventHandler(ClassLoaderEventHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classLoaderEventHandlers == null)
         classLoaderEventHandlers = new CopyOnWriteArrayList<ClassLoaderEventHandler>();
      
      classLoaderEventHandlers.add(handler);
   }
   
   /**
    * Remove a ClassLoaderEventHandler
    * 
    * @param handler the handler
    */
   public void removeClassLoaderEventHandler(ClassLoaderEventHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      
      if (classLoaderEventHandlers == null)
         return;
      classLoaderEventHandlers.remove(handler);
   }

   private void fireRegisterClassLoader(ClassLoaderEvent event)
   {
      if (classLoaderEventHandlers != null && classLoaderEventHandlers.isEmpty() == false)
      {
         for (ClassLoaderEventHandler handler : classLoaderEventHandlers)
         {
            try
            {
               handler.fireRegisterClassLoader(event);
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classLoaderEventHandler: " + handler, t);
            }
         }
      }
      
      ClassLoaderSystem system = (ClassLoaderSystem) getClassLoaderSystem();
      if (system == null)
         return;
      system.fireRegisterClassLoader(event);
   }

   private void fireUnregisterClassLoader(ClassLoaderEvent event)
   {
      if (classLoaderEventHandlers != null && classLoaderEventHandlers.isEmpty() == false)
      {
         for (ClassLoaderEventHandler handler : classLoaderEventHandlers)
         {
            try
            {
               handler.fireUnregisterClassLoader(event);
            }
            catch (Throwable t)
            {
               log.warn("Error invoking classLoaderEventHandler: " + handler, t);
            }
         }
      }
      
      ClassLoaderSystem system = (ClassLoaderSystem) getClassLoaderSystem();
      if (system == null)
         return;
      system.fireUnregisterClassLoader(event);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@").append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{").append(name).append('}');
      return builder.toString();
   }
   
   @Override
   protected Class<?> loadClassBefore(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesClassName(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent beforeFilter=" + filter);
         return loadClassFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent beforeFilter=" + filter);
      return null;
   }

   @Override
   protected Class<?> loadClassAfter(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesClassName(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent afterFilter=" + filter);
         return loadClassFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent afterFilter=" + filter);
      return null;
   }

   /**
    * Try to find a load a from the parent
    * 
    * @param name the name
    * @return the class if found
    */
   protected Class<?> loadClassFromParent(String name)
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not loading from non-existant parent");
         return null;
      }

      if (trace)
         log.trace(this + " load class from parent " + name + " parent=" + parent);

      // Recurse into parent domains
      return parentLoader.loadClass(name);
   }
   
   @Override
   protected Loader findBeforeLoader(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent beforeFilter=" + filter);
         return findLoaderFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent beforeFilter=" + filter);
      return null;
   }

   @Override
   protected Loader findAfterLoader(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent afterFilter=" + filter);
         return findLoaderFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent afterFilter=" + filter);
      return null;
   }

   /**
    * Try to find a loader from the parent
    * 
    * @param name the name
    * @return the loader if found
    */
   protected Loader findLoaderFromParent(String name)
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not loading from non-existant parent");
         return null;
      }

      if (trace)
         log.trace(this + " load from parent " + name + " parent=" + parent);

      // Recurse into parent domains
      if (parentLoader instanceof ClassLoaderDomain)
      {
         ClassLoaderDomain parentDomain = (ClassLoaderDomain) parentLoader;
         return parentDomain.findLoader(name);
      }
      
      // A normal loader
      if (parentLoader.getResource(name) != null)
         return parentLoader;
      
      return null;
   }
   
   @Override
   protected URL beforeGetResource(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent beforeFilter=" + filter);
         return getResourceFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent beforeFilter=" + filter);
      return null;
   }

   @Override
   protected URL afterGetResource(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent afterFilter=" + filter);
         return getResourceFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent afterFilter=" + filter);
      return null;
   }

   /**
    * Try to get a resource from the parent
    * 
    * @param name the name
    * @return the url if found
    */
   protected URL getResourceFromParent(String name)
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not getting resource from non-existant parent");
         return null;
      }

      if (trace)
         log.trace(this + " get resource from parent " + name + " parent=" + parentLoader);
      
      URL result = parentLoader.getResource(name);

      if (trace)
      {
         if (result != null)
            log.trace(this + " got resource from parent " + name + " parent=" + parentLoader + " " + result);
         else
            log.trace(this + " resource not found in parent " + name + " parent=" + parentLoader);
      }
      
      return result;
   }
   
   @Override
   // FindBugs: The Set doesn't use equals/hashCode
   protected void beforeGetResources(String name, Set<URL> urls) throws IOException
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent beforeFilter=" + filter);
         getResourcesFromParent(name, urls);
      }
      else if (trace)
         log.trace(this + " " + name + " does NOT match parent beforeFilter=" + filter);
   }

   @Override
   // FindBugs: The Set doesn't use equals/hashCode
   protected void afterGetResources(String name, Set<URL> urls) throws IOException
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesResourcePath(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent afterFilter=" + filter);
         getResourcesFromParent(name, urls);
      }
      else if (trace)
         log.trace(this + " " + name + " does NOT match parent afterFilter=" + filter);
   }

   /**
    * Try to get resources from the parent
    * 
    * @param name the name
    * @param urls the urls to add to
    * @throws IOException for any error
    */
   // FindBugs: The Set doesn't use equals/hashCode
   protected void getResourcesFromParent(String name, Set<URL> urls) throws IOException
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not getting resources from non-existant parent");
         return;
      }

      if (trace)
         log.trace(this + " get resources from parent " + name + " parent=" + parentLoader);
      
      parentLoader.getResources(name, urls);
   }
   
   @Override
   protected Package beforeGetPackage(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesPackageName(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent beforeFilter=" + filter);
         return getPackageFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent beforeFilter=" + filter);
      return null;
   }

   @Override
   protected Package afterGetPackage(String name)
   {
      boolean trace = log.isTraceEnabled();
      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesPackageName(name))
      {
         if (trace)
            log.trace(this + " " + name + " matches parent afterFilter=" + filter);
         return getPackageFromParent(name);
      }
      if (trace)
         log.trace(this + " " + name + " does NOT match parent afterFilter=" + filter);
      return null;
   }

   /**
    * Try to get a package from the parent
    * 
    * @param name the name
    * @return the package if found
    */
   protected Package getPackageFromParent(String name)
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not getting package from non-existant parent");
         return null;
      }

      if (trace)
         log.trace(this + " get package from parent " + name + " parent=" + parentLoader);
      
      Package result = parentLoader.getPackage(name);

      if (trace)
      {
         if (result != null)
            log.trace(this + " got package from parent " + name + " parent=" + parentLoader + " " + result);
         else
            log.trace(this + " package not found in parent " + name + " parent=" + parentLoader);
      }
      
      return result;
   }
   
   @Override
   protected void beforeGetPackages(Set<Package> packages)
   {
      ClassFilter filter = getParentPolicy().getBeforeFilter();
      getPackagesFromParent(packages, filter);
   }

   @Override
   protected void afterGetPackages(Set<Package> packages)
   {
      ClassFilter filter = getParentPolicy().getAfterFilter();
      getPackagesFromParent(packages, filter);
   }

   /**
    * Try to get packages from the parent
    * 
    * @param packages the packages to add to
    * @param filter the filter
    */
   protected void getPackagesFromParent(Set<Package> packages, ClassFilter filter)
   {
      Loader parentLoader = getParent();

      boolean trace = log.isTraceEnabled();
      if (parentLoader == null)
      {
         if (trace)
            log.trace(this + " not getting packages from non-existant parent");
         return;
      }

      if (trace)
         log.trace(this + " get packages from parent=" + parentLoader + " filter=" + filter);
      
      Set<Package> parentPackages = new HashSet<Package>();
      parentLoader.getPackages(parentPackages);
      for (Package parentPackage : parentPackages)
      {
         if (filter.matchesPackageName(parentPackage.getName()))
         {
            if (trace)
               log.trace(this + " parentPackage=" + parentPackage + " matches filter=" + filter);
            packages.add(parentPackage);
         }
         else if (trace)
            log.trace(this + " parentPackage=" + parentPackage + " does NOT match filter=" + filter);
      }
   }

   /**
    * Fixup the parent to the our classloader as parent if we don't have an explicit one
    */
   private void fixUpParent()
   {
      try
      {
         if (parent == null)
         {
            final ClassLoader classLoader = getParentClassLoader();
            if (classLoader != null)
            {
               parent = AccessController.doPrivileged(new PrivilegedAction<Loader>()
               {
                  public Loader run()
                  {
                     return new ClassLoaderToLoaderAdapter(classLoader);
                  }
               });
            }
         }
      }
      finally
      {
         if ((parent instanceof ClassLoaderDomain == false) && (parent instanceof BaseClassLoaderSource == false))
            setUseLoadClassForParent(true);
      }
   }

   public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
   {
      this.mbeanServer = server;
      this.objectName = name;
      return name;
   }
   
   public void postRegister(Boolean registrationDone)
   {
      if (registrationDone)
      {
         // Register any classloaders that were added before we were registered in the MBeanServer
         for (ClassLoader cl : getAllClassLoaders())
            registerClassLoaderMBean(cl);
      }
      else
      {
         postDeregister();
      }
   }

   public void preDeregister() throws Exception
   {
      // Unregister any remaining classloaders from the MBeanServer
      for (ClassLoader cl : getAllClassLoaders())
         unregisterClassLoaderMBean(cl);
   }
   
   public void postDeregister()
   {
      this.mbeanServer = null;
      this.objectName = null;
   }

   @Override
   protected void afterRegisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      registerClassLoaderMBean(classLoader);
      fireRegisterClassLoader(new ClassLoaderEvent(this, classLoader));
   }

   @Override
   protected void beforeUnregisterClassLoader(ClassLoader classLoader, ClassLoaderPolicy policy)
   {
      fireUnregisterClassLoader(new ClassLoaderEvent(this, classLoader));
      unregisterClassLoaderMBean(classLoader);
   }

   /**
    * Register a classloader with the MBeanServer
    * 
    * @param cl the classloader
    */
   protected void registerClassLoaderMBean(ClassLoader cl)
   {
      if (mbeanServer == null)
         return;
      
      if (cl instanceof RealClassLoader)
      {
         ObjectName name = ((RealClassLoader) cl).getObjectName();
         try
         {
            mbeanServer.registerMBean(cl, name);
         }
         catch (Exception e)
         {
            log.warn("Error registering classloader: " + cl, e);
         }
      }
   }

   /**
    * Unregister a classloader from the MBeanServer
    * 
    * @param cl the classloader
    */
   protected void unregisterClassLoaderMBean(ClassLoader cl)
   {
      if (mbeanServer == null)
         return;
      
      if (cl instanceof RealClassLoader)
      {
         ObjectName name = ((RealClassLoader) cl).getObjectName();
         try
         {
            mbeanServer.unregisterMBean(name);
         }
         catch (Exception e)
         {
            log.warn("Error unregistering classloader: " + cl, e);
         }
      }
   }

   @Override
   protected Class<?> checkCacheBefore(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      if (parent == null || parent instanceof CacheLoader == false)
         return null;

      ClassFilter filter = getParentPolicy().getBeforeFilter();
      if (filter.matchesClassName(name))
      {
         CacheLoader loader = (CacheLoader) parent;
         return loader.checkClassCache(classLoader, name, path, allExports);
      }
      return null;
   }

   /**
    * Only check parent after if we already blacklisted this resource.
    *
    * @param classLoader the classloader (possibly null)
    * @param name the name
    * @param path the path of the class resource
    * @param allExports whether to look at all exports
    * @return cached result if found in parent
    */
   @Override
   protected Class<?> checkCacheAfter(BaseClassLoader classLoader, String name, String path, boolean allExports)
   {
      if (parent == null || parent instanceof CacheLoader == false || isBlackListedClass(path) == false)
         return null;

      ClassFilter filter = getParentPolicy().getAfterFilter();
      if (filter.matchesClassName(name))
      {
         CacheLoader loader = (CacheLoader) parent;
         return loader.checkClassCache(classLoader, name, path, allExports);
      }
      return null;
   }
}
