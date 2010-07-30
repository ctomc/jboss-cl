/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.util.Set;

import org.jboss.classloader.spi.ClassLoaderCache;
import org.jboss.classloader.spi.ImportType;
import org.jboss.classloader.spi.Loader;

/**
 * Wrap finder method with additional ClassLoaderInformation lookup.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ClassLoaderCacheWrapper implements ClassLoaderCache
{
   private ClassLoaderCache delegate;
   private ClassLoaderInformation info;

   ClassLoaderCacheWrapper(ClassLoaderCache delegate, ClassLoaderInformation info)
   {
      this.delegate = delegate;
      this.info = info;
   }

   /**
    * Is the resource imported by our classloader.
    *
    * @param type the type
    * @param name the resource name
    * @return true if it's imported, false otherwise
    */
   protected boolean isImported(ImportType type, String name)
   {
      if (info != null)
      {
         Set<String> imports = info.getImportedPackages(type);
         if (imports.isEmpty() == false)
         {
            String pckg = ClassLoaderInformation.getResourcePackageName(name);
            return imports.contains(pckg);
         }
      }
      return false;
   }

   public Loader getCachedLoader(String name)
   {
      if (isImported(ImportType.ALL, name))
         return delegate.getCachedLoader(name);
      else
         return null;
   }

   public Loader findLoader(ImportType type, String name)
   {
      Loader loader = delegate.findLoader(type, name);
      if (loader != null)
      {
         cacheLoader(name, loader);
         return loader;
      }

      if (info != null)
      {
         loader = info.findLoader(type, name);
         if (loader != null)
            cacheLoader(name, loader);
      }
      return loader;
   }

   public void cacheLoader(String name, Loader loader)
   {
      delegate.cacheLoader(name, loader);
   }

   public boolean isBlackListedClass(String name)
   {
      return delegate.isBlackListedClass(name);
   }

   public void blackListClass(String name)
   {
      if (isImported(ImportType.ALL, name))
         delegate.blackListClass(name);
   }

   public URL getCachedResource(String name)
   {
      if (isImported(ImportType.ALL, name))
         return delegate.getCachedResource(name);
      else
         return null;
   }

   public URL findResource(ImportType type, String name)
   {
      URL url = delegate.findResource(type, name);
      if (url != null)
      {
         cacheResource(name, url);
         return url;
      }

      if (info != null)
      {
         url = info.findResource(type, name);
         if (url != null)
            cacheResource(name, url);
      }
      return url;
   }

   public void cacheResource(String name, URL resource)
   {
      delegate.cacheResource(name, resource);
   }

   public boolean isBlackListedResource(String name)
   {
      return delegate.isBlackListedResource(name);
   }

   public void blackListResource(String name)
   {
      if (isImported(ImportType.ALL, name))
         delegate.blackListResource(name);
   }

   public void flushCaches()
   {
      delegate.flushCaches();
   }

   public void clearBlackList(String name)
   {
      delegate.clearBlackList(name);
   }

   public boolean isRelevant(ImportType type)
   {
      return delegate.isRelevant(type);
   }

   public String getInfo(ImportType type)
   {
      return delegate.getInfo(type);
   }
}
