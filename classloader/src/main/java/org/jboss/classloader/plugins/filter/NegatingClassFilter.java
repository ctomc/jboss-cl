/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.classloader.plugins.filter;

import org.jboss.classloader.spi.filter.ClassFilter;

/**
 * NegatingClassFilter.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class NegatingClassFilter implements ClassFilter
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The filter to negate */
   private ClassFilter filter;

   public NegatingClassFilter(ClassFilter filter)
   {
      if (filter == null)
         throw new IllegalArgumentException("Null filter");
      this.filter = filter;
   }

   public boolean matchesClassName(String className)
   {
      return filter.matchesClassName(className) == false;
   }

   public boolean matchesResourcePath(String resourcePath)
   {
      return filter.matchesResourcePath(resourcePath) == false;
   }

   public boolean matchesPackageName(String packageName)
   {
      return filter.matchesPackageName(packageName) == false;
   }
}