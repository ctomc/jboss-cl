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

import java.net.URL;

/**
 * Default resource context.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class DefaultResourceContext extends AbstractResourceContext
{
   /** The url of the resource */
   private URL url;

   /**
    * Create a new ResourceContext.
    *
    * @param url the url
    * @param resourceName the resource name
    * @param classLoader the classloader
    */
   public DefaultResourceContext(URL url, String resourceName, ClassLoader classLoader)
   {
      super(resourceName, classLoader);

      if (url == null)
         throw new IllegalArgumentException("Null url");
      this.url = url;
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
}
