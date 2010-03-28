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
package org.jboss.classloading.plugins.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.classloading.plugins.visitor.AbstractResourceContext;
import org.jboss.vfs.VirtualFile;

/**
 * VFS resource context.
 * 
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class VFSResourceContext extends AbstractResourceContext
{
   private VirtualFile file;
   private VirtualFile root;

   public VFSResourceContext(VirtualFile file, String resourceName, ClassLoader classLoader)
   {
      super(resourceName, classLoader);

      if (file == null)
         throw new IllegalArgumentException("Null file");
      this.file = file;
   }

   public URL getUrl()
   {
      try
      {
         return file.toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public InputStream getInputStream() throws IOException
   {
      return file.openStream();
   }

   @Override
   public URL getRootUrl()
   {
      try
      {
         return root.toURL();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Set the root.
    *
    * @param root the root
    */
   void setRoot(VirtualFile root)
   {
      if (root == null)
         throw new IllegalArgumentException("Null root");
      this.root = root;
   }
}
