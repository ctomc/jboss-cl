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

import java.net.URL;

/**
 * Simple ClassLoaderCache spi.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public interface ClassLoaderCache
{
   /**
    * Get the cached loader.
    *
    * @param name the class name
    * @return cached loader or null if no such loader
    */
   Loader getCachedLoader(String name);

   /**
    * Try finding the non-cached loader.
    * If found cache it if possible.
    *
    * @param type import type
    * @param name the class name
    * @return found loader or null
    */
   Loader findLoader(ImportType type, String name);

   /**
    * Do cache loader.
    *
    * @param name the class name
    * @param loader the loader to cache
    */
   void cacheLoader(String name, Loader loader);

   /**
    * Is class black listed.
    *
    * @param name the class name
    * @return true if black listed, false otherwise
    */
   boolean isBlackListedClass(String name);

   /**
    * Do blacklist class.
    *
    * @param name the class name to black list
    */
   void blackListClass(String name);

   /**
    * Get cached resource.
    *
    * @param name the resource name
    * @return cached resource or null if no such resource
    */
   URL getCachedResource(String name);

   /**
    * Try finding non-cached the resource.
    * If found cache it if possible.
    *
    * @param type import type
    * @param name the resource name
    * @return found resource or null
    */
   URL findResource(ImportType type, String name);

   /**
    * Cache resource.
    *
    * @param name the resource name
    * @param resource the resource
    */
   void cacheResource(String name, URL resource);

   /**
    * Is the resource black listed.
    * @param name the resource name
    * @return true if resource is black listed, false otherwise
    */
   boolean isBlackListedResource(String name);

   /**
    * Do black list the resource.
    *
    * @param name the resource name
    */
   void blackListResource(String name);

   /**
    * Flush the caches
    */
   void flushCaches();

   /**
    * Cleans the entry with the given name from the blackList
    *
    * @param name the name of the resource to clear from the blackList
    */
   void clearBlackList(String name);

   /**
    * Is the cache relevant for lookup.
    *
    * @param type the import type
    * @return true if relevant, false otherwise
    */
   boolean isRelevant(ImportType type);

   /**
    * Get the cache info.
    *
    * @param type the import type
    * @return the info
    */
   String getInfo(ImportType type);
}