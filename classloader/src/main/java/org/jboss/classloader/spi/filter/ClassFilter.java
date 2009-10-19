/*
 * JBoss, Home of Professional Open Source.
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
package org.jboss.classloader.spi.filter;

import java.io.Serializable;

/**
 * ClassFilter.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public interface ClassFilter extends Serializable
{
   /** 
    * Whether the class name matches the filter
    * 
    * @param className the class name
    * @return true when it matches the filter
    */
   boolean matchesClassName(String className);
   
   /** 
    * Whether the resource name matches the filter
    * 
    * @param resourcePath the resource path
    * @return true when it matches the filter
    */
   boolean matchesResourcePath(String resourcePath);
   
   /** 
    * Whether the package name matches the filter
    * 
    * @param packageName the package path
    * @return true when it matches the filter
    */
   boolean matchesPackageName(String packageName);
}
