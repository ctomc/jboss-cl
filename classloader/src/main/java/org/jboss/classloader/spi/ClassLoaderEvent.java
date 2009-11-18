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
package org.jboss.classloader.spi;

import java.util.EventObject;

/**
 * ClassLoaderEvent.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoaderEvent extends EventObject
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -5874702798336257018L;

   /** The classLoader */
   private ClassLoader classLoader;
   
   /**
    * Create a new ClassLoaderEvent.
    *
    * @param classLoaderDomain the classLoaderDomain
    * @param classLoader classLoader
    */
   public ClassLoaderEvent(ClassLoaderDomain classLoaderDomain, ClassLoader classLoader)
   {
      super(classLoaderDomain);
      this.classLoader = classLoader;
   }

   /**
    * Get the classLoader.
    * 
    * @return the classLoader
    */
   public ClassLoader getClassLoader()
   {
      return classLoader;
   }

   /**
    * Get the classLoaderDomain.
    * 
    * @return the classLoaderDomain
    */
   public ClassLoaderDomain getClassLoaderDomain()
   {
      return (ClassLoaderDomain) getSource();
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + "[classLoaderDomain=" + getClassLoaderDomain() + " classLoader=" + getClassLoader() + "]";
   }
}
