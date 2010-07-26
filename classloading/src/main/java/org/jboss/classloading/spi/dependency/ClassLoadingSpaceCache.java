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
package org.jboss.classloading.spi.dependency;

import java.net.URL;

import org.jboss.classloader.spi.ImportType;
import org.jboss.classloader.spi.Loader;
import org.jboss.classloader.spi.helpers.AbstractClassLoaderCache;

/**
 * ClassLoadingSpace shared cache.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class ClassLoadingSpaceCache extends AbstractClassLoaderCache
{
   private ClassLoadingSpace space;

   ClassLoadingSpaceCache(ClassLoadingSpace space)
   {
      this.space = space;
   }

   public Loader findLoader(ImportType type, String name)
   {
      return null; // leave it to the CLI wrapper
   }

   public URL findResource(ImportType type, String name)
   {
      return null; // leave it to the CLI wrapper
   }

   public boolean isRelevant(ImportType type)
   {
      return true;
   }

   public String getInfo(ImportType type)
   {
      return space.toString();
   }

   void merge(ClassLoadingSpaceCache other)
   {
      super.merge(other);
   }
}
