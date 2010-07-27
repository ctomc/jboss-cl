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

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ImportType;
import org.jboss.classloader.spi.Loader;
import org.jboss.classloader.spi.helpers.AbstractClassLoaderCache;

/**
 * ClassLoaderInformation.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class ClassLoaderInformation extends AbstractClassLoaderCache
{
   /** The classloader */
   private BaseClassLoader classLoader;
   
   /** The policy */
   private BaseClassLoaderPolicy policy;

   /** The order */
   private int order;
   
   /** The exports of the classloader */
   private BaseDelegateLoader exported;

   /** The delegates */
   private volatile Map<ImportType, List<DelegateLoader>> delegates;

   /** The # of delegates who cant cache */
   private int cantCache;

   /** The # of delegates who cant blacklist */
   private int cantBlacklist;

   /** The package to delegate index */
   private Map<ImportType, Map<String, List<Loader>>> index;

   /**
    * Create a new ClassLoaderInformation.
    * 
    * @param classLoader the classloader
    * @param policy the policy
    * @param order the added order
    * @throws IllegalArgumentException for a null parameter
    */
   public ClassLoaderInformation(BaseClassLoader classLoader, BaseClassLoaderPolicy policy, int order)
   {
      if (classLoader == null)
         throw new IllegalArgumentException("Null classloader");
      if (policy == null)
         throw new IllegalArgumentException("Null policy");
      this.classLoader = classLoader;
      this.policy = policy;
      this.order = order;
      this.exported = policy.getExported();
      
      boolean canCache = policy.isCacheable();
      boolean canBlackList = policy.isBlackListable();

      // package to delegate index
      index = new ConcurrentHashMap<ImportType, Map<String, List<Loader>>>();

      List<? extends DelegateLoader> delegates = policy.getDelegates();
      if (delegates != null && delegates.isEmpty() == false)
      {
         Map<ImportType, List<DelegateLoader>> temp = new HashMap<ImportType, List<DelegateLoader>>();
         // prepare ALL
         List<DelegateLoader> all = new CopyOnWriteArrayList<DelegateLoader>();
         temp.put(ImportType.ALL, all);

         for (DelegateLoader delegate : delegates)
         {
            if (delegate == null)
               throw new IllegalStateException(policy + " null delegate in " + delegates);

            ImportType importType = delegate.getImportType();
            List<DelegateLoader> loaders = temp.get(importType);
            if (loaders == null)
            {
               loaders = new CopyOnWriteArrayList<DelegateLoader>();
               temp.put(importType, loaders);
            }
            loaders.add(delegate); // add to specific type
            all.add(delegate); // add to all

            BaseDelegateLoader baseDelegate = delegate;
            BaseClassLoaderPolicy delegatePolicy = baseDelegate.getPolicy();
            if (delegatePolicy == null || delegatePolicy.isCacheable() == false)
            {
               canCache = false;
               cantCache++;
            }
            if (delegatePolicy == null || delegatePolicy.isBlackListable() == false)
            {
               canBlackList = false;
               cantBlacklist++;
            }

            addLoaderToIndex(baseDelegate, delegatePolicy, importType, ImportType.ALL);
         }

         this.delegates = Collections.synchronizedMap(temp);
      }

      if (canCache)
      {
         restoreCache();
      }
      
      if (canBlackList)
      {
         restoreBlackList();
      }
   }

   /**
    * Get the classLoader.
    * 
    * @return the classLoader.
    */
   public BaseClassLoader getClassLoader()
   {
      return classLoader;
   }

   /**
    * Get the policy.
    * 
    * @return the policy.
    */
   public BaseClassLoaderPolicy getPolicy()
   {
      return policy;
   }

   /**
    * Get the order.
    * 
    * @return the order.
    */
   public int getOrder()
   {
      return order;
   }

   /**
    * Get the exported.
    * 
    * @return the exported.
    */
   public BaseDelegateLoader getExported()
   {
      return exported;
   }

   /**
    * Get the delegates.
    * 
    * @return the delegates.
    * @deprecated use same method with import type parameter
    */
   @Deprecated
   public List<? extends DelegateLoader> getDelegates()
   {
      return getDelegates(ImportType.BEFORE);
   }
   
   /**
    * Get the delegates.
    *
    * @param type the import type
    * @return the delegates.
    */
   public List<? extends DelegateLoader> getDelegates(ImportType type)
   {
      if (delegates == null)
         return Collections.emptyList();

      return delegates.get(type);
   }

   /**
    * Add a delegate loader.
    *
    * @param loader the delegate loader
    */
   void addDelegate(DelegateLoader loader)
   {
      if (loader == null)
         throw new IllegalArgumentException("Null delegate");

      if (delegates == null)
         delegates = Collections.synchronizedMap(new HashMap<ImportType, List<DelegateLoader>>());

      BaseDelegateLoader baseDelegate = loader;
      BaseClassLoaderPolicy policy = baseDelegate.getPolicy();
      boolean canCache = (policy != null && policy.isCacheable());
      boolean canBlackList = (policy != null && policy.isBlackListable());

      ImportType type = loader.getImportType();
      //noinspection SynchronizeOnNonFinalField
      synchronized (delegates)
      {
         List<DelegateLoader> list = delegates.get(type);
         if (list == null)
         {
            list = new CopyOnWriteArrayList<DelegateLoader>();
            delegates.put(type, list);
         }
         list.add(0, loader); // add at the begining
         // all
         List<DelegateLoader> all = delegates.get(ImportType.ALL);
         if (all == null)
         {
            all = new CopyOnWriteArrayList<DelegateLoader>();
            delegates.put(ImportType.ALL, all);
         }
         all.add(loader);

         if (canCache == false)
         {
            // we can cache atm, but the new one can't
            if (cantCache == 0)
               destroyCache();

            cantCache++;
         }

         if (canBlackList == false)
         {
            // we can blacklist atm, but the new one can't
            if (cantBlacklist == 0)
               destroyBlackList();

            cantBlacklist++;
         }

         addLoaderToIndex(loader, policy, type, ImportType.ALL);
      }
   }

   /**
    * Remove a delegate loader.
    *
    * @param loader the delegate loader
    */
   void removeDelegate(DelegateLoader loader)
   {
      if (loader == null)
         throw new IllegalArgumentException("Null delegate");

      if (delegates == null)
         return;

      BaseDelegateLoader baseDelegate = loader;
      BaseClassLoaderPolicy policy = baseDelegate.getPolicy();
      boolean canCache = (policy != null && policy.isCacheable());
      boolean canBlackList = (policy != null && policy.isBlackListable());

      ImportType type = loader.getImportType();
      //noinspection SynchronizeOnNonFinalField
      synchronized (delegates)
      {
         List<DelegateLoader> list = delegates.get(type);
         if (list != null)
         {
            if (list.remove(loader) && list.isEmpty())
               delegates.remove(type);
         }

         boolean member = false;
         // all
         List<DelegateLoader> all = delegates.get(ImportType.ALL);
         if (all != null)
         {
            member = all.remove(loader);
            if (member && all.isEmpty())
               delegates.remove(ImportType.ALL);
         }

         // make sure we only handle our members
         if (member)
         {
            if (canCache == false)
            {
               cantCache--;

               // we can again cache
               if (cantCache == 0)
                  restoreCache();
            }

            if (canBlackList == false)
            {
               cantBlacklist--;

               // we can again blacklist
               if (cantBlacklist == 0)
                  restoreBlackList();
            }

            removeLoaderFromIndex(loader, policy, type, ImportType.ALL);
         }
      }
   }

   /**
    * Add loader to index.
    *
    * @param loader the loader
    * @param policy the policy
    * @param types the types
    */
   private void addLoaderToIndex(Loader loader, BaseClassLoaderPolicy policy, ImportType... types)
   {
      if (policy == null)
         return;

      String[] packageNames = policy.getPackageNames();
      if (packageNames != null && packageNames.length > 0)
      {
         for (ImportType type : types)
         {
            Map<String, List<Loader>> map = index.get(type);
            if (map == null)
            {
               map = new ConcurrentHashMap<String, List<Loader>>();
               index.put(type, map);
            }
            for (String pn : packageNames)
            {
               List<Loader> loaders = map.get(pn);
               if (loaders == null)
               {
                  loaders = new CopyOnWriteArrayList<Loader>();
                  map.put(pn, loaders);
               }
               loaders.add(loader);
            }
         }
      }
   }

   /**
    * Remove loader from index.
    *
    * @param loader the loader
    * @param policy the policy
    * @param types the types
    */
   private void removeLoaderFromIndex(Loader loader, BaseClassLoaderPolicy policy, ImportType... types)
   {
      if (policy == null)
         return;

      String[] packageNames = policy.getPackageNames();
      if (packageNames != null && packageNames.length > 0)
      {
         for (ImportType type : types)
         {
            Map<String, List<Loader>> map = index.get(type);
            if (map != null)
            {
               for (String pn : packageNames)
               {
                  List<Loader> loaders = map.get(pn);
                  if (loaders != null)
                  {
                     if (loaders.remove(loader) && loaders.isEmpty())
                     {
                        map.remove(pn); // remove returns loaders
                        if (map.isEmpty())
                           index.remove(type);
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isRelevant(ImportType type)
   {
      List<? extends DelegateLoader> loaders = getDelegates(type);
      return loaders != null && loaders.isEmpty() == false;
   }

   /**
    * Get the package name for a resource
    *
    * @param resource the resource name
    * @return the package name or the empty string if there is no package
    */
   private static String getResourcePackageName(final String resource)
   {
      int i = resource.lastIndexOf('/');
      if (i == -1)
         return "";
      return resource.substring(0, i).replace('/', '.');
   }

   public Loader findLoader(ImportType type, String name)
   {
      return findLoaderInternal(type, name, new URL[1]);
   }

   public URL findResource(ImportType type, String name)
   {
      URL[] result = new URL[1];
      findLoaderInternal(type, name, result);
      return result[0];
   }

   /**
    * Find loader with requesting resource.
    *
    * @param type the import type
    * @param name the resource name
    * @param result the temp result holder
    * @return found loader or null if not found
    */
   private Loader findLoaderInternal(ImportType type, String name, URL[] result)
   {
      // try index first
      Map<String, List<Loader>> map = index.get(type);
      if (map != null && map.isEmpty() == false)
      {
         String pckg = getResourcePackageName(name);
         List<Loader> loaders = map.get(pckg);
         if (loaders != null && loaders.isEmpty() == false)
         {
            for (Loader loader : loaders)
            {
               URL url = loader.getResource(name);
               if (url != null)
               {
                  result[0] = url;
                  cacheLoader(name, loader);
                  cacheResource(name, url);
                  return loader;
               }
            }
         }
      }

      // fall back to old all delegates list, some policies don't expose packages
      List<? extends DelegateLoader> delegates = getDelegates(type);
      if (delegates != null && delegates.isEmpty() == false)
      {
         for (DelegateLoader loader : delegates)
         {
            URL url = loader.getResource(name);
            if (url != null)
            {
               result[0] = url;
               cacheLoader(name, loader);
               cacheResource(name, url);
               return loader;
            }
         }
      }

      return null;
   }

   public String getInfo(ImportType type)
   {
      StringBuilder builder = new StringBuilder();
      List<? extends DelegateLoader> delegates = getDelegates(type);
      if (delegates != null && delegates.isEmpty() == false)
         builder.append("delegates: ").append(delegates);
      builder.append(getClassLoader());
      return builder.toString();
   }

   @Override
   public String toString()
   {
      return policy.toString();
   }
}
