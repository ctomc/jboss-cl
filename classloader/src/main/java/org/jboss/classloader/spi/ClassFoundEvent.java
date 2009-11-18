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
 * ClassFoundEvent.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassFoundEvent extends EventObject
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -8258925782380851534L;

   /** The class */
   private Class<?> clazz;
   
   /**
    * Create a new ClassFoundEvent.
    * 
    * @param classLoader classLoader
    * @param clazz the class
    */
   public ClassFoundEvent(ClassLoader classLoader, Class<?> clazz)
   {
      super(classLoader);
      this.clazz = clazz;
   }

   /**
    * Get the className.
    * 
    * @return the className.
    */
   public String getClassName()
   {
      return clazz.getName();
   }

   /**
    * Get the class.
    * 
    * @return the class.
    */
   public Class<?> getClazz()
   {
      return clazz;
   }

   /**
    * Get the classLoader.
    * 
    * @return the classLoader
    */
   public ClassLoader getClassLoader()
   {
      return (ClassLoader) getSource();
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + "[classLoader=" + getClassLoader() + " class=" + getClassName() + "]";
   }
}
