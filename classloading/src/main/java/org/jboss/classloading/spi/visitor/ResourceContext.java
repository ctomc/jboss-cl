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
package org.jboss.classloading.spi.visitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * ResourceContext.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public interface ResourceContext
{
   /**
    * Get the url.
    * 
    * @return the url.
    */
   URL getUrl();

   /**
    * Get the classLoader.
    * 
    * @return the classLoader.
    */
   ClassLoader getClassLoader();

   /**
    * Get the resourceName.
    * 
    * @return the resourceName.
    */
   String getResourceName();

   /**
    * Get the class name
    * 
    * @return the class name or null if it is not a class
    */
   String getClassName();

   /**
    * Whether the resource is a class
    * 
    * @return true when the resource name ends with .class
    */
   boolean isClass();

   /**
    * Load a class
    *
    * Do isClass check before,
    * unless you want to handle exception
    * when resource is not actually a class. 
    * 
    * @return the class from resource
    * @throws RuntimeException for any errors during class loading
    */
   Class<?> loadClass();

   /**
    * Get the input stream for the resource
    * 
    * @return the input stream
    * @throws IOException for any error
    */
   InputStream getInputStream() throws IOException;

   /**
    * Get the bytes for the resource
    * 
    * @return the byte array
    * @throws IOException for any error
    */
   byte[] getBytes() throws IOException;
}
