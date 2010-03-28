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
package org.jboss.classloading.plugins.visitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.RootAwareResource;

/**
 * Abstract resource context.
 * Doesn't take url - super class should impl getURL.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractResourceContext implements ResourceContext, RootAwareResource
{
   /** The classloader */
   private ClassLoader classLoader;

   /** The resource name */
   private String resourceName;

   /**
    * Create a new ResourceContext.
    *
    * @param resourceName the resource name
    * @param classLoader the classloader
    */
   public AbstractResourceContext(String resourceName, ClassLoader classLoader)
   {
      if (resourceName == null)
         throw new IllegalArgumentException("Null resourceName");
      if (classLoader == null)
         throw new IllegalArgumentException("Null classloader");

      this.resourceName = resourceName;
      this.classLoader = classLoader;
   }

   /**
    * Get root url.
    *
    * @return the root url
    */
   public URL getRootUrl()
   {
      throw new RuntimeException("Not implemented, override in non-abstract class / implementation.");
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
    * Do isClass check before,
    * unless you want to handle exception
    * when resource is not actually a class.
    *
    * @return the class from resource
    * @throws RuntimeException for any errors during class loading
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
    * @throws java.io.IOException for any error
    */
   public InputStream getInputStream() throws IOException
   {
      URL url = getUrl();
      if (url == null)
         throw new IllegalArgumentException("Null url: " + resourceName);

      return url.openStream();
   }

   /**
    * Get the bytes for the resource
    *
    * @return the byte array
    * @throws java.io.IOException for any error
    */
   public byte[] getBytes() throws IOException
   {
      return ClassLoaderUtils.loadBytes(getInputStream());
   }
}