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
package org.jboss.classloader.spi.helpers;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.classloader.spi.ClassLoaderCache;
import org.jboss.classloader.spi.Loader;
import org.jboss.util.collection.ConcurrentSet;

/**
 * AbstractClassLoaderCache.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractClassLoaderCache implements ClassLoaderCache
{
   /** The class cache */
   private volatile Map<String, Loader> classCache;

   /** The class black list */
   private volatile Set<String> classBlackList;

   /** The resource cache */
   private volatile Map<String, URL> resourceCache;

   /** The resource black list */
   private volatile Set<String> resourceBlackList;

   /**
    * Restore cache.
    */
   protected void restoreCache()
   {
      classCache = new ConcurrentHashMap<String, Loader>();
      resourceCache = new ConcurrentHashMap<String, URL>();
   }

   /**
    * Destroy cache.
    */
   protected void destroyCache()
   {
      classCache = null;
      resourceCache = null;
   }

   /**
    * Restore black list.
    */
   protected void restoreBlackList()
   {
      classBlackList = new ConcurrentSet<String>();
      resourceBlackList = new ConcurrentSet<String>();
   }

   /**
    * Destroy black list.
    */
   protected void destroyBlackList()
   {
      classBlackList = null;
      resourceBlackList = null;
   }

   /**
    * Merge caches.
    *
    * @param other the other cache
    */
   protected void merge(AbstractClassLoaderCache other)
   {
      if (other.classCache != null)
      {
         if (classCache != null)
            classCache.putAll(other.classCache);
         else
            classCache = new ConcurrentHashMap<String, Loader>(other.classCache);
      }
      if (other.resourceCache != null)
      {
         if (resourceCache != null)
            resourceCache.putAll(other.resourceCache);
         else
            resourceCache = new ConcurrentHashMap<String, URL>(other.resourceCache);
      }

      // previously bl resources can now become available - flush it
      flushBlackLists();
   }

   public void flushCaches()
   {
      if (classCache != null)
         classCache.clear();
      if (resourceCache != null)
         resourceCache.clear();
      flushBlackLists();
   }

   private void flushBlackLists()
   {
      if (classBlackList != null)
         classBlackList.clear();
      if (resourceBlackList != null)
         resourceBlackList.clear();
   }

   public Loader getCachedLoader(String name)
   {
      Map<String, Loader> classCache = this.classCache;
      if (classCache != null)
         return classCache.get(name);
      return null;
   }
   
   public void cacheLoader(String name, Loader loader)
   {
      Map<String, Loader> classCache = this.classCache;
      if (classCache != null)
         classCache.put(name, loader);
   }
   
   public boolean isBlackListedClass(String name)
   {
      Set<String> classBlackList = this.classBlackList;
      return classBlackList != null && classBlackList.contains(name);
   }
   
   public void blackListClass(String name)
   {
      Set<String> classBlackList = this.classBlackList;
      if (classBlackList != null)
         classBlackList.add(name);
   }
   
   public URL getCachedResource(String name)
   {
      Map<String, URL> resourceCache = this.resourceCache;
      if (resourceCache != null)
         return resourceCache.get(name);
      return null;
   }
   
   public void cacheResource(String name, URL url)
   {
      Map<String, URL> resourceCache = this.resourceCache;
      if (resourceCache != null)
         resourceCache.put(name, url);
   }
   
   public boolean isBlackListedResource(String name)
   {
      Set<String> resourceBlackList = this.resourceBlackList;
      return resourceBlackList != null && resourceBlackList.contains(name);
   }
   
   public void blackListResource(String name)
   {
      Set<String> resourceBlackList = this.resourceBlackList;
      if (resourceBlackList != null)
         resourceBlackList.add(name);
   }
   
   public void clearBlackList(String name)
   {
      if (classBlackList != null)
         classBlackList.remove(name);
      if (resourceBlackList != null)
         resourceBlackList.remove(name);
   }
}
