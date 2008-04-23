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

import org.jboss.classloader.plugins.ClassLoaderUtils;

/**
 * ResourceContext.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ResourceContext
{
   /** The url of the resource */
   private URL url;
   
   /** The classloader */
   private ClassLoader classLoader;
   
   /** The resource name */
   private String resourceName;
   
   /**
    * Create a new ResourceContext.
    * 
    * @param url the url
    * @param resourceName the resource name
    * @param classLoader the classloader
    */
   public ResourceContext(URL url, String resourceName, ClassLoader classLoader)
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");
      if (resourceName == null)
         throw new IllegalArgumentException("Null resourceName");
      if (classLoader == null)
         throw new IllegalArgumentException("Null classloader");
      this.url = url;
      this.resourceName = resourceName;
      this.classLoader = classLoader;
   }

   /**
    * Get the url.
    * 
    * @return the url.
    */
   public URL getUrl()
   {
      return url;
   }

   /**
    * Get the classLoader.
    * 
    * @return the classLoader.
    */
   public ClassLoader getClassLoader()
   {
      return classLoader;
   }

   /**
    * Get the resourceName.
    * 
    * @return the resourceName.
    */
   public String getResourceName()
   {
      return resourceName;
   }
   
   /**
    * Get the class name
    * 
    * @return the class name or null if it is not a class
    */
   public String getClassName()
   {
      return ClassLoaderUtils.resourceNameToClassName(getResourceName());
   }
   
   /**
    * Whether the resource is a class
    * 
    * @return true when the resource name ends with .class
    */
   public boolean isClass()
   {
      return resourceName.endsWith(".class");
   }
   
   /**
    * Load a class
    * 
    * @return the class or null if it is not a class
    */
   public Class<?> loadClass()
   {
      String className = getClassName();
      try
      {
         return classLoader.loadClass(className);
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException("Unexpected error loading class: " + className, e);
      }
   }
   
   /**
    * Get the input stream for the resource
    * 
    * @return the input stream
    * @throws IOException for any error
    */
   public InputStream getInputStream() throws IOException
   {
      return url.openStream();
   }
   
   /**
    * Get the bytes for the resource
    * 
    * @return the byte array
    * @throws IOException for any error
    */
   public byte[] getBytes() throws IOException
   {
      return ClassLoaderUtils.loadBytes(getInputStream());
   }
}
