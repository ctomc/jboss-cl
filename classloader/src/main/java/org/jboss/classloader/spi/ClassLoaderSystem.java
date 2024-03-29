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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.plugins.system.ClassLoaderSystemBuilder;
import org.jboss.classloader.spi.base.BaseClassLoaderSystem;
import org.jboss.classloader.spi.translator.TranslatorUtils;
import org.jboss.logging.Logger;
import org.jboss.util.loading.Translator;

/**
 * ClassLoaderSystem.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class ClassLoaderSystem extends BaseClassLoaderSystem implements ClassLoaderSystemMBean, MBeanRegistration, ClassNotFoundHandler, ClassFoundHandler, ClassLoaderEventHandler
{
   /** The log */
   private static final Logger log = Logger.getLogger(ClassLoaderSystem.class);

   /** The name of the default domain */
   public static final String DEFAULT_DOMAIN_NAME = "DefaultDomain";

   /** The default domain */
   private ClassLoaderDomain defaultDomain;
   
   /** The registered domains by name */
   private Map<String, ClassLoaderDomain> registeredDomains = new HashMap<String, ClassLoaderDomain>();

   /** Any translators */
   private List<Translator> translators;
   
   /** The shutdown policy */
   private ShutdownPolicy shutdownPolicy;
   
   /** Whether the system is shutdown */
   private boolean shutdown = false;
   
   /** The MBeanServer */
   private MBeanServer mbeanServer;
   
   /** The object name */
   private ObjectName objectName;

   /** The class not found handlers */
   private List<ClassNotFoundHandler> classNotFoundHandlers;

   /** The class found handlers */
   private List<ClassFoundHandler> classFoundHandlers;

   /** The class loader event handlers */
   private List<ClassLoaderEventHandler> classLoaderEventHandlers;
   
   /**
    * Get the classloading system instance
    * 
    * @return the instance
    * @throws SecurityException if the caller does not have authority to create a classloader
    */
   public static final ClassLoaderSystem getInstance()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkCreateClassLoader();
      return ClassLoaderSystemBuilder.get();
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
    * Get the default classloading domain
    * 
    * @return the default domain
    */
   public synchronized ClassLoaderDomain getDefaultDomain()
   {
      if (shutdown)
         throw new IllegalStateException("The classloader system is shutdown: " + toLongString());
      
      // Already constructed
      if (defaultDomain != null)
         return defaultDomain;
      
      // See if explicitly registered
      defaultDomain = registeredDomains.get(DEFAULT_DOMAIN_NAME);
      if (defaultDomain != null)
         return defaultDomain;
      
      // Create it
      defaultDomain = createDefaultDomain();
      
      // Register it
      internalRegisterDomain(DEFAULT_DOMAIN_NAME, defaultDomain);
      
      return defaultDomain;
   }
   
   /**
    * Create the default domain<p>
    * 
    * By default this just invokes {@link #createDomain(String)} with {@link #DEFAULT_DOMAIN_NAME}
    * 
    * @return the default domain
    */
   protected ClassLoaderDomain createDefaultDomain()
   {
      return createDomain(DEFAULT_DOMAIN_NAME);
   }
   
   /**
    * Create a domain
    * 
    * @param name the name of the domain
    * @return the domain
    * @throws IllegalArgumentException for a null name
    */
   protected abstract ClassLoaderDomain createDomain(String name);
   
   /**
    * Create and register a domain
    * 
    * @param name the name of the domain
    * @return the domain
    * @throws IllegalArgumentException for a null name
    * @throws IllegalStateException if there already is a domain with that name
    */
   public ClassLoaderDomain createAndRegisterDomain(String name)
   {
      return createAndRegisterDomain(name, ParentPolicy.BEFORE, null);
   }
   
   /**
    * Create and register a domain with the given parent classloading policy
    * 
    * @param name the name of the domain
    * @param parentPolicy the parent classloading policy
    * @return the domain
    * @throws IllegalArgumentException for a null name or policy
    * @throws IllegalStateException if there already is a domain with that name
    */
   public ClassLoaderDomain createAndRegisterDomain(String name, ParentPolicy parentPolicy)
   {
      return createAndRegisterDomain(name, parentPolicy, null);
   }
   
   /**
    * Create and register a domain with the given parent classloading policy
    * 
    * @param name the name of the domain
    * @param parentPolicy the parent classloading policy
    * @param parent the parent
    * @return the domain
    * @throws IllegalArgumentException for a null argument
    * @throws IllegalStateException if there already is a domain with that name
    */
   public ClassLoaderDomain createAndRegisterDomain(String name, ParentPolicy parentPolicy, Loader parent)
   {
      return createAndRegisterDomain(name, parentPolicy, parent, null);
   }

   /**
    * Create and register a domain with the given parent classloading policy
    *
    * @param name the name of the domain
    * @param parentPolicy the parent classloading policy
    * @param parent the parent
    * @param shutdownPolicy the shutdown policy
    * @return the domain
    * @throws IllegalArgumentException for a null argument
    * @throws IllegalStateException if there already is a domain with that name
    */
   public ClassLoaderDomain createAndRegisterDomain(String name, ParentPolicy parentPolicy, Loader parent, ShutdownPolicy shutdownPolicy)
   {
      return createAndRegisterDomain(name, parentPolicy, parent, shutdownPolicy, null);
   }

   /**
    * Create and register a domain with the given parent classloading policy
    *
    * @param name the name of the domain
    * @param parentPolicy the parent classloading policy
    * @param parent the parent
    * @param shutdownPolicy the shutdown policy
    * @param useLoadClassForParent the use classloader for parent flag
    * @return the domain
    * @throws IllegalArgumentException for a null argument
    * @throws IllegalStateException if there already is a domain with that name
    */
   public ClassLoaderDomain createAndRegisterDomain(String name, ParentPolicy parentPolicy, Loader parent, ShutdownPolicy shutdownPolicy, Boolean useLoadClassForParent)
   {
      ClassLoaderDomain result = createDomain(name);
      if (result == null)
         throw new IllegalArgumentException("Created null domain: " + name);

      if (useLoadClassForParent != null)
         result.setUseLoadClassForParent(useLoadClassForParent);
      result.setParentPolicy(parentPolicy);
      result.setParent(parent);
      if (shutdownPolicy != null)
         result.setShutdownPolicy(shutdownPolicy);
      registerDomain(result);
      return result;
   }

   /**
    * Get a domain
    * 
    * @param name the domain name
    * @return the domain
    * @throws IllegalArgumentException for a null name
    */
   public synchronized ClassLoaderDomain getDomain(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      if (shutdown)
         throw new IllegalStateException("The classloader system is shutdown: " + toLongString());

      ClassLoaderDomain result = registeredDomains.get(name);
      
      // See whether this is the default domain
      if (result == null && DEFAULT_DOMAIN_NAME.equals(name))
         result = getDefaultDomain();
      
      return result;
   }

   /**
    * Is a domain name registered
    * 
    * @param name the domain name
    * @return true when the domain is registered
    * @throws IllegalArgumentException for a null name
    */
   public boolean isRegistered(String name)
   {
      return getDomain(name) != null;
   }

   /**
    * Is a domain registered
    * 
    * @param domain the domain
    * @return true when the domain is registered
    * @throws IllegalArgumentException for a null domain
    */
   public boolean isDomainRegistered(ClassLoaderDomain domain)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      return getDomain(domain.getName()) == domain;
   }

   /**
    * Register a domain
    * 
    * @param domain the domain
    * @throws IllegalArgumentException for a null domain
    * @throws IllegalStateException if a domain is already registered with this name
    */
   public synchronized void registerDomain(ClassLoaderDomain domain)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      
      String name = domain.getName();
      if (isRegistered(name))
         throw new IllegalStateException("A domain is already registered with name " + name);
      
      internalRegisterDomain(name, domain);
   }

   /**
    * Do the registration
    * 
    * @param name the name
    * @param domain the domain
    */
   private void internalRegisterDomain(String name, ClassLoaderDomain domain)
   {
      if (shutdown)
         throw new IllegalStateException("The classloader system is shutdown: " + toLongString());

      registeredDomains.put(name, domain);
      super.registerDomain(domain);

      registerDomainMBean(domain);
      
      log.debug(this + " registered domain=" + domain.toLongString());
   }
   
   /**
    * Unregister a domain
    * 
    * @param domain the domain
    * @throws IllegalArgumentException for a null domain or if you attempt to unregister the default domain
    * @throws IllegalStateException if a domain is not registered
    */
   public synchronized void unregisterDomain(ClassLoaderDomain domain)
   {
      if (isDomainRegistered(domain) == false)
         throw new IllegalStateException("Domain is not registered " + domain);

      if (DEFAULT_DOMAIN_NAME.equals(domain.getName()))
         throw new IllegalArgumentException("Cannot unregister the default domain");
      
      internalUnregisterDomain(domain);
   }
   
   /**
    * Unregister a domain
    * 
    * @param domain the domain
    * @throws IllegalArgumentException for a null domain or if you attempt to unregister the default domain
    * @throws IllegalStateException if a domain is not registered
    */
   private synchronized void internalUnregisterDomain(ClassLoaderDomain domain)
   {
      unregisterDomainMBean(domain);
      
      registeredDomains.remove(domain.getName());
      super.unregisterDomain(domain);
      
      log.debug(this + " unregistered domain=" + domain.toLongString());
   }
   
   /**
    * Register a policy with the default domain<p>
    * 
    * Equivalent to {@link #registerClassLoaderPolicy(ClassLoaderDomain, ClassLoaderPolicy)} using
    * {@link #getDefaultDomain()} as the ClassLoaderDomain
    * 
    * @param policy the policy
    * @return the classloader
    * @throws IllegalArgumentException if a parameter is null
    * @throws IllegalStateException if the policy is already registered with a domain  
    */
   public ClassLoader registerClassLoaderPolicy(ClassLoaderPolicy policy)
   {
      return registerClassLoaderPolicy(getDefaultDomain(), policy);
   }

   /**
    * Register a classloader policy, possibly constructing the domain with a BEFORE parent policy
    * 
    * @param domainName the domain name
    * @param policy the classloader policy
    * @return the policy
    * @throws IllegalArgumentException for a null parameter
    */
   public ClassLoader registerClassLoaderPolicy(String domainName, ClassLoaderPolicy policy)
   {
      return registerClassLoaderPolicy(domainName, ParentPolicy.BEFORE, (Loader) null, policy);
   }

   /**
    * Register a classloader policy, possibly constructing the domain
    * 
    * @param domainName the domain name
    * @param parentPolicy the parent policy
    * @param policy the classloader policy
    * @return the policy
    * @throws IllegalArgumentException for a null parameter
    */
   public ClassLoader registerClassLoaderPolicy(String domainName, ParentPolicy parentPolicy, ClassLoaderPolicy policy)
   {
      return registerClassLoaderPolicy(domainName, parentPolicy, (Loader) null, policy);
   }

   /**
    * Register a classloader policy, possibly constructing the domain
    * 
    * @param domainName the domain name
    * @param parentPolicy the parent policy
    * @param parentDomainName the parent domain (can be null)
    * @param policy the classloader policy
    * @return the policy
    * @throws IllegalArgumentException for a null parameter
    * @throws IllegalStateException if the parent domain does not exist
    */
   public ClassLoader registerClassLoaderPolicy(String domainName, ParentPolicy parentPolicy, String parentDomainName, ClassLoaderPolicy policy)
   {
      if (domainName == null)
         throw new IllegalArgumentException("Null domain name");
      if (parentPolicy == null)
         throw new IllegalArgumentException("Null parent policy");
      if (policy == null)
         throw new IllegalArgumentException("Null classloader policy");
      
      ClassLoaderDomain domain;
      synchronized (this)
      {
         // See whether the domain already exists
         domain = getDomain(domainName);
         if (domain == null)
         {
            if (parentDomainName != null)
            {
               // See whether the parent domain exists
               ClassLoaderDomain parentDomain = getDomain(parentDomainName);
               if (parentDomain == null)
                  throw new IllegalStateException("Parent domain: " + parentDomainName + " does not exist.");
               
               // Create the domain with a parent
               domain = createAndRegisterDomain(domainName, parentPolicy, parentDomain);
            }
            else
            {
               // Create a domain without a parent
               domain = createAndRegisterDomain(domainName, parentPolicy);
            }
         }
      }
      
      // Register the classloader policy in the domain
      return registerClassLoaderPolicy(domain, policy);
   }

   /**
    * Register a classloader policy, possibly constructing the domain
    * 
    * @param domainName the domain name
    * @param parentPolicy the parent policy
    * @param parent the parent
    * @param policy the classloader policy
    * @return the policy
    * @throws IllegalArgumentException for a null parameter
    * @throws IllegalStateException if the parent domain does not exist
    */
   public ClassLoader registerClassLoaderPolicy(String domainName, ParentPolicy parentPolicy, Loader parent, ClassLoaderPolicy policy)
   {
      if (domainName == null)
         throw new IllegalArgumentException("Null domain name");
      if (parentPolicy == null)
         throw new IllegalArgumentException("Null parent policy");
      if (policy == null)
         throw new IllegalArgumentException("Null classloader policy");
      
      ClassLoaderDomain domain;
      synchronized (this)
      {
         // See whether the domain already exists
         domain = getDomain(domainName);
         if (domain == null)
         {
            // Create a domain without a parent
            domain = createAndRegisterDomain(domainName, parentPolicy, parent);
         }
      }
      
      // Register the classloader policy in the domain
      return registerClassLoaderPolicy(domain, policy);
   }
   
   /**
    * Register a policy with a domain
    * 
    * @param domain the domain
    * @param policy the policy
    * @return the classloader
    * @throws IllegalArgumentException if a parameter is null
    * @throws IllegalStateException if the domain is not registered or if the policy is already registered with a domain  
    */
   public ClassLoader registerClassLoaderPolicy(ClassLoaderDomain domain, ClassLoaderPolicy policy)
   {
      if (isDomainRegistered(domain) == false)
         throw new IllegalStateException("Domain is not registered: " + domain);
      
      synchronized (this)
      {
         if (shutdown)
            throw new IllegalStateException("The classloader system is shutdown: " + toLongString());
      }
      return super.registerClassLoaderPolicy(domain, policy);
   }
   
   /**
    * Unregister a policy from its domain
    * 
    * @param policy the policy
    * @throws IllegalArgumentException if a parameter is null
    * @throws IllegalStateException if the policy is not registered with the default domain  
    */
   public void unregisterClassLoaderPolicy(ClassLoaderPolicy policy)
   {
      super.unregisterClassLoaderPolicy(policy);
   }
   
   /**
    * Unregister a classloader from its domain
    * 
    * @param classLoader classLoader
    * @throws IllegalArgumentException if a parameter is null
    * @throws IllegalStateException if the policy is not registered with the default domain  
    */
   public void unregisterClassLoader(ClassLoader classLoader)
   {
      super.unregisterClassLoader(classLoader);
   }

   /**
    * Shutdown the classloader system<p>
    * 
    * Unregisters all domains by default
    */
   public synchronized void shutdown()
   {
      if (shutdown)
         return;

      log.debug(toLongString() + " SHUTDOWN!");
      shutdown = true;
      
      while (true)
      {
         List<ClassLoaderDomain> domains = new ArrayList<ClassLoaderDomain>(registeredDomains.values());
         Iterator<ClassLoaderDomain> iterator = domains.iterator();
         if (iterator.hasNext() == false)
            break;
         
         while (iterator.hasNext())
         {
            ClassLoaderDomain domain = iterator.next();
            internalUnregisterDomain(domain);
         }
      }
   }
   
   /**
    * Get the translator.
    * 
    * @return the translator.
    * @deprecated use translator list
    */
   @Deprecated
   public synchronized Translator getTranslator()
   {
      if (translators == null || translators.isEmpty())
         return null;

      return translators.get(0);
   }

   /**
    * Set the translator.
    * 
    * @param translator the translator.
    * @deprecated use translator list
    */
   @Deprecated
   public synchronized void setTranslator(Translator translator)
   {
      log.debug(this + " set translator to " + translator);

      if (translator != null)
         translators = Collections.singletonList(translator);
      else
         translators = null;
   }

   @Override
   protected byte[] transform(ClassLoader classLoader, String className, byte[] byteCode, ProtectionDomain protectionDomain) throws Exception
   {
      byte[] result = TranslatorUtils.applyTranslatorsOnTransform(getTranslators(), classLoader, className, byteCode, protectionDomain);
      return super.transform(classLoader, className, result, protectionDomain);
   }

   @Override
   protected void afterUnregisterClassLoader(ClassLoader classLoader)
   {
      try
      {
         TranslatorUtils.applyTranslatorsAtUnregister(getTranslators(), classLoader);
      }
      catch (Throwable t)
      {
         log.warn("Error unregistering classloader from translator " + classLoader, t);
      }
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
         for (ClassLoaderDomain domain : registeredDomains.values())
            registerDomainMBean(domain);
      }
      else
      {
         postDeregister();
      }
   }

   public void preDeregister() throws Exception
   {
      for (ClassLoaderDomain domain : registeredDomains.values())
         unregisterDomainMBean(domain);
   }
   
   public void postDeregister()
   {
      this.mbeanServer = null;
      this.objectName = null;
   }

   public Set<String> getDomainNames()
   {
      return registeredDomains.keySet();
   }

   public Set<ObjectName> getDomains()
   {
      Set<ObjectName> names = new HashSet<ObjectName>();
      for (ClassLoaderDomain domain : registeredDomains.values())
         names.add(domain.getObjectName());
      return names;
   }

   /**
    * Get an object name for the domain
    * 
    * @param domain the domain
    * @return the object name
    */
   protected ObjectName getObjectName(ClassLoaderDomain domain)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      
      Hashtable<String, String> properties = new Hashtable<String, String>();
      properties.put("domain", "\"" + domain.getName() + "\"");
      properties.put("system", "" + System.identityHashCode(this));
      try
      {
         return ObjectName.getInstance("jboss.classloader", properties);
      }
      catch (MalformedObjectNameException e)
      {
         throw new RuntimeException("Unexpected error", e);
      }
   }
   
   /**
    * Register a domain with the MBeanServer
    * 
    * @param domain the domain
    */
   protected void registerDomainMBean(ClassLoaderDomain domain)
   {
      if (mbeanServer == null)
         return;

      try
      {
         ObjectName name = getObjectName(domain);
         mbeanServer.registerMBean(domain, name);
      }
      catch (Exception e)
      {
         log.warn("Error registering domain: " + domain, e);
      }
   }

   /**
    * Unregister a domain from the MBeanServer
    * 
    * @param domain the domain
    */
   protected void unregisterDomainMBean(ClassLoaderDomain domain)
   {
      if (mbeanServer == null)
         return;

      try
      {
         ObjectName name = getObjectName(domain);
         mbeanServer.unregisterMBean(name);
      }
      catch (Exception e)
      {
         log.warn("Error unregistering domain: " + domain, e);
      }
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
      return false;
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

   public void fireRegisterClassLoader(ClassLoaderEvent event)
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
   }

   public void fireUnregisterClassLoader(ClassLoaderEvent event)
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
   }

   @Override
   protected void toLongString(StringBuilder builder)
   {
      if (shutdown)
         builder.append("SHUTDOWN! ");
      super.toLongString(builder);
   }


   /**
    * Get the policy's translators.
    *
    * @return the translators
    */
   public synchronized List<Translator> getTranslators()
   {
      if (translators == null || translators.isEmpty())
         return Collections.emptyList();
      else
         return Collections.unmodifiableList(translators);
   }

   /**
    * Set the translators.
    *
    * @param translators the translators
    */
   public synchronized void setTranslators(List<Translator> translators)
   {
      this.translators = translators;
   }

   /**
    * Add the translator.
    *
    * @param translator the translator to add
    * @throws IllegalArgumentException for null translator
    */
   public synchronized void addTranslator(Translator translator)
   {
      if (translator == null)
         throw new IllegalArgumentException("Null translator");

      if (translators == null)
         translators = new ArrayList<Translator>();

      translators.add(translator);
   }

   /**
    * Remove the translator.
    *
    * @param translator the translator to remove
    * @throws IllegalArgumentException for null translator
    */
   public synchronized void removeTranslator(Translator translator)
   {
      if (translator == null)
         throw new IllegalArgumentException("Null translator");

      if (translators != null)
         translators.remove(translator);
   }
}
