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
package org.jboss.classloader.spi.base;

import javax.management.ObjectName;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;

import org.jboss.classloader.spi.ClassLoaderCache;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloader.spi.translator.TranslatorUtils;
import org.jboss.logging.Logger;
import org.jboss.util.loading.Translator;

/**
 * Base ClassLoader policy.<p>
 * 
 * This class hides some of the implementation details and allows
 * package access to the protected methods.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class BaseClassLoaderPolicy implements Formattable
{
   /** The log */
   private static final Logger log = Logger.getLogger(BaseClassLoaderPolicy.class);
   
   /** The classloader for this policy */
   private volatile BaseClassLoader classLoader;

   /** The domain for this policy */
   private volatile BaseClassLoaderDomain domain;

   /** The classloader information */
   private volatile ClassLoaderInformation information;

   /** The cache */
   private volatile ClassLoaderCache cache;

   /** The access control context for this policy */
   private AccessControlContext access;

   /** The translators */
   private List<Translator> translators;

   /**
    * Create a new BaseClassLoaderPolicy.
    * 
    * @throws SecurityException if the caller does not have permission to create a classloader
    */
   public BaseClassLoaderPolicy()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkCreateClassLoader();
      
      access = AccessController.getContext();
   }

   /**
    * Add extra delegate loader.
    *
    * @param loader the new delegate
    */
   protected void addExtraDelegate(DelegateLoader loader)
   {
      ClassLoaderInformation info = getInformation();
      if (info != null)
         info.addDelegate(loader);
   }

   /**
    * Remove extra delegate loader.
    *
    * @param loader the old delegate
    */
   protected void removeExtraDelegate(DelegateLoader loader)
   {
      ClassLoaderInformation info = getInformation();
      if (info != null)
         info.removeDelegate(loader);
   }

   /**
    * Get the information.
    * 
    * @return the information.
    */
   ClassLoaderInformation getInformation()
   {
      return information;
   }

   /**
    * Set the information.
    * 
    * @param information the information.
    */
   void setInformation(ClassLoaderInformation information)
   {
      this.information = information;
   }

   /**
    * Get the cache.
    *
    * By default we return information if there is no cache set explicitly.
    *
    * @return the cache
    */
   protected ClassLoaderCache getCache()
   {
      ClassLoaderCache clc = cache;
      if (clc == null)
         return information;
      
      return new ClassLoaderCacheWrapper(clc, information);
   }

   /**
    * Set the cache.
    *
    * @param cache the cache
    */
   protected void setCache(ClassLoaderCache cache)
   {
      this.cache = cache;
   }

   /**
    * Get the access control context for this policy
    * 
    * @return the access control context
    */
   protected AccessControlContext getAccessControlContext()
   {
      return access;
   }
   
   /**
    * Get the delegate loader for exported stuff<p>
    *
    * NOTE: Protected access for security reasons
    * 
    * @return the delegate loader
    */
   protected abstract DelegateLoader getExported();

   /**
    * Get a simple name for the classloader
    * 
    * @return the name
    */
   protected String getName()
   {
      return "";
   }
   
   /**
    * Get the exported packages<p>
    *
    * Provides a hint for indexing
    * 
    * @return the package names
    */
   public abstract String[] getPackageNames();

   /**
    * Get the delegate loaders for imported stuff<p>
    * 
    * NOTE: Protected access for security reasons
    * 
    * @return the delegate loaders
    */
   protected abstract List<? extends DelegateLoader> getDelegates();

   /**
    * Whether to import all exports from other classloaders in the domain
    * 
    * @return true to import all
    */
   protected abstract boolean isImportAll();

   /**
    * Get the protection domain<p>
    * 
    * NOTE: Defined as protected here for security reasons
    * 
    * @param className the class name
    * @param path the path
    * @return the protection domain
    */
   protected abstract ProtectionDomain getProtectionDomain(String className, String path);
   
   /**
    * Transform the byte code<p>
    * 
    * By default, this delegates to the domain
    * 
    * @param className the class name
    * @param byteCode the byte code
    * @param protectionDomain the protection domain
    * @return the transformed byte code
    * @throws Exception for any error
    */
   protected byte[] transform(String className, byte[] byteCode, ProtectionDomain protectionDomain) throws Exception
   {
      byte[] result = byteCode;

      BaseClassLoaderDomain domain = getClassLoaderDomain();
      if (domain != null)
         result = domain.transform(getClassLoader(), className, result, protectionDomain);

      ClassLoader classLoader = getClassLoaderUnchecked();
      if (classLoader != null)
         result = TranslatorUtils.applyTranslatorsOnTransform(getTranslators(), classLoader, className, result, protectionDomain);

      return result;
   }

   /**
    * Whether to cache<p>
    * 
    * @return true to cache
    */
   protected abstract boolean isCacheable();

   /**
    * Whether to cache misses<p>
    * 
    * @return true to cache misses
    */
   protected abstract boolean isBlackListable();

   /**
    * Get the object name the classloader is registered in the MBeanServer with
    * 
    * @return the object name
    */
   public abstract ObjectName getObjectName();

   /**
    * Check whether this a request from the jdk if it is return the relevant classloader
    * 
    * @param name the class name
    * @return the classloader
    */
   protected abstract ClassLoader isJDKRequest(String name);

   /**
    * Get the shutdownPolicy.
    * 
    * @return the shutdownPolicy.
    */
   protected abstract ShutdownPolicy getShutdownPolicy();

   /**
    * Determine the shutdown policy for this domain
    * 
    * @return the shutdown policy
    */
   ShutdownPolicy determineShutdownPolicy()
   {
      BaseClassLoaderDomain domain = getClassLoaderDomain();
      if (domain == null)
      {
         ShutdownPolicy result = getShutdownPolicy();
         return (result != null) ? result : ShutdownPolicy.UNREGISTER;
      }
      return domain.determineShutdownPolicy(this);
   }
   
   /**
    * A long version of toString()
    * 
    * @return the long string
    */
   public String toLongString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@").append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{");
      String name = getName();
      if (name != null)
         builder.append("name=").append(name).append(" ");
      builder.append("domain=");
      if (domain == null)
         builder.append("null");
      else
         builder.append(domain.toLongString());
      toLongString(builder);
      builder.append('}');
      return builder.toString();
   }
   
   /**
    * For subclasses to add information for toLongString()
    * 
    * @param builder the builder
    */
   protected void toLongString(StringBuilder builder)
   {
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@").append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{").append(getName()).append("}");
      return builder.toString();
   }

   public void formatTo(Formatter formatter, int flags, int width, int precision)
   {
      // TODO perhaps decide which toXString to use depending on args?
      formatter.format(toLongString());
   }

   /**
    * Get the classloader domain name
    * 
    * @return the domain
    */
   public String getDomainName()
   {
      if (domain == null)
         return null;
      return ((ClassLoaderDomain) domain).getName();
   }
   
   /**
    * Get the classloader domain
    * 
    * @return the domain
    */
   protected ClassLoaderDomain getDomain()
   {
      return (ClassLoaderDomain) getClassLoaderDomain();
   }
   
   /**
    * Get the classloader domain
    * 
    * @return the domain
    */
   BaseClassLoaderDomain getClassLoaderDomain()
   {
      return domain;
   }
   
   /**
    * Set the classloader domain
    * 
    * @param domain the domain
    * @throws IllegalStateException if the policy already has a domain
    */
   void setClassLoaderDomain(BaseClassLoaderDomain domain)
   {
      if (this.domain != null)
         throw new IllegalStateException("Policy already has a domain " + this);
      this.domain = domain;
   }

   
   /**
    * Unset the classloader domain
    * 
    * @param domain the domain
    * @throws IllegalStateException if the policy is not part of that domain
    */
   void unsetClassLoaderDomain(BaseClassLoaderDomain domain)
   {
      if (this.domain != domain)
         throw new IllegalStateException("Policy is not a part of the domain " + this + " domain=" + domain);
      shutdownPolicy();
      this.domain = null;
   }

   /**
    * Get the classloader based on classloading task.
    *
    * Since ClassLoadingTask ctor is package protected
    * this method cannot be easily abused, since the only
    * code that can instantiate ClassLoadingTask is our ClassLoaderManager.
    *
    * @param task the classloading task info
    * @return the classloader
    */
   synchronized BaseClassLoader getClassLoader(ClassLoadingTask task)
   {
      return getClassLoader();
   }

   /**
    * Get the classloader
    * 
    * @return the classloader
    */
   synchronized BaseClassLoader getClassLoader()
   {
      if (classLoader == null)
         throw new IllegalStateException("No classloader associated with policy therefore it is no longer registered " + toLongString());
      return classLoader;
   }
   
   /**
    * Get the classloader
    * 
    * @return the classloader
    */
   synchronized BaseClassLoader getClassLoaderUnchecked()
   {
      return classLoader;
   }
   
   /**
    * Set the classloader<p>
    * 
    * NOTE: Package private for security reasons
    * 
    * @param classLoader the classloader
    * @throws IllegalStateException if the classloader is already set
    */
   synchronized void setClassLoader(BaseClassLoader classLoader)
   {
      if (this.classLoader != null)
         throw new IllegalStateException("Policy already has a classloader previous=" + classLoader);
      this.classLoader = classLoader;
   }
   
   /**
    * Shutdown the policy<p>
    * 
    * The default implementation removes and shutdowns the classloader
    */
   synchronized protected void shutdownPolicy()
   {
      log.debug(toString() + " shutdown!");
      BaseClassLoader classLoader = this.classLoader;
      this.classLoader = null;
      TranslatorUtils.applyTranslatorsAtUnregister(translators, classLoader);
      classLoader.shutdownClassLoader();
   }
   
   /**
    * Cleans the entry with the given name from the blackList
    *
    * @param name the name of the resource to clear from the blackList
    */
   protected void clearBlackList(String name)
   {
       if (domain != null)
       {
          domain.clearBlackList(name);
       }
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
