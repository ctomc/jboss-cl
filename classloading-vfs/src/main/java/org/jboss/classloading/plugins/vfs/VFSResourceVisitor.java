/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.classloading.plugins.vfs;

import java.net.URL;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileVisitor;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.vfs.helpers.AbstractVirtualFileFilterWithAttributes;

/**
 * Visits a virtual file system recursively
 * to determine resources
 * 
 * @author <a href="adrian@jboss.org">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VFSResourceVisitor extends AbstractVirtualFileFilterWithAttributes implements VirtualFileVisitor
{
   /** The roots */
   private VirtualFile[] roots;
   
   /** The excluded roots */
   private VirtualFile[] excludedRoots;
   
   /** The current root */
   private VirtualFile root;
   
   /** The root */
   private String rootPath;
   
   /** The root with slash*/
   private String rootPathWithSlash;

   /** The included packages */
   private ClassFilter included;

   /** The excluded packages */
   private ClassFilter excluded;

   /** The classLoader */
   private ClassLoader classLoader;

   /** The resource visitor */
   private ResourceVisitor visitor;
   
   /** The resource filter */
   private ResourceFilter filter;
   
   /** The resource filter */
   private ResourceFilter recurseFilter;

   /**
    * Visit the resources
    * 
    * @param roots the roots
    * @param excludedRoots the excluded roots
    * @param included the included packages
    * @param excluded the excluded packages
    * @param classLoader the classLoader
    * @param visitor the visitor
    * @param filter the filter
    * @param recurseFilter the recurse filter
    * @param urls the urls
    */
   public static void visit(VirtualFile[] roots, VirtualFile[] excludedRoots, ClassFilter included, ClassFilter excluded, ClassLoader classLoader, ResourceVisitor visitor, ResourceFilter filter, ResourceFilter recurseFilter, URL... urls)
   {
      VFSResourceVisitor vfsVisitor = new VFSResourceVisitor(roots, excludedRoots, included, excluded, classLoader, visitor, filter, recurseFilter);
      for (VirtualFile root : roots)
      {
         try
         {
            if (urls == null || urls.length == 0 || matchRootWithUrls(root, urls))
            {
               vfsVisitor.setRoot(root);
               root.visit(vfsVisitor);
            }
         }
         catch (Exception e)
         {
            throw new Error("Error visiting " + root, e);
         }
      }
   }

   /**
    * Match root with urls.
    *
    * @param root one of the roots
    * @param urls the urls
    * @return true if root matches one of the urls
    * @throws Exception for any error
    */
   protected static boolean matchRootWithUrls(VirtualFile root, URL[] urls) throws Exception
   {
      URL rootURL = root.toURL();
      for (URL url : urls)
      {
         if (rootURL.equals(url))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * Create a new VFSResourceVisitor.
    *
    * @param roots the roots
    * @param excludedRoots the excluded roots
    * @param included the included packages
    * @param excluded the excluded packages
    * @param classLoader the classloader
    * @param visitor the visitor
    * @param filter the filter
    * @param recurseFilter the recurse filter
    */
   VFSResourceVisitor(VirtualFile[] roots, VirtualFile[] excludedRoots, ClassFilter included, ClassFilter excluded, ClassLoader classLoader, ResourceVisitor visitor, ResourceFilter filter, ResourceFilter recurseFilter)
   {
      if (roots == null)
         throw new IllegalArgumentException("Null roots");
      if (classLoader == null)
         throw new IllegalArgumentException("Null classloader");
      if (visitor == null)
         throw new IllegalArgumentException("Null visitor");

      this.roots = roots;
      this.excludedRoots = excludedRoots;
      this.included = included;
      this.excluded = excluded;
      this.classLoader = classLoader;
      this.visitor = visitor;
      this.filter = filter;
      this.recurseFilter = recurseFilter;
   }

   /**
    * Set the root
    * 
    * @param root the root
    * @throws IllegalArgumentException for a null root
    */
   void setRoot(VirtualFile root)
   {
      if (root == null)
         throw new IllegalArgumentException("Null root");
      this.root = root;
      rootPath = root.getPathName();
      rootPathWithSlash = rootPath + "/";
   }

   public VisitorAttributes getAttributes()
   {
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeRoot(false);
      attributes.setRecurseFilter(this);
      return attributes;
   }   

   /**
    * Determine the file's path.
    *
    * @param file the file
    * @return file's path
    */
   protected String determinePath(VirtualFile file)
   {
      String path = file.getPathName();
      if (path.equals(rootPath))
         path = "";
      else if (path.startsWith(rootPathWithSlash))
         path = path.substring(rootPathWithSlash.length());
      return path;
   }

   public boolean accepts(VirtualFile file)
   {
      if (recurseFilter != null)
      {
         try
         {
            String path = determinePath(file);
            ResourceContext resource = new VFSResourceContext(file, path, classLoader);
            if (recurseFilter.accepts(resource) == false)
               return false;
         }
         catch (Exception e)
         {
            throw new Error("Error accepting " + file, e);
         }
      }

      // This is our current root
      if (file.equals(root))
         return true;

      // Some other root, it will be handled later
      for (VirtualFile other : roots)
      {
         if (file.equals(other))
            return false;
      }
      // Is it an excluded root?
      if (excludedRoots != null)
      {
         for (VirtualFile other : excludedRoots)
         {
            if (file.equals(other))
               return false;
         }
      }
      
      // Ok
      return true;
   }
   
   public void visit(VirtualFile file)
   {
      try
      {
         // We don't want directories
         if (file.isLeaf() == false)
            return;

         // Determine the resource name
         String path = determinePath(file);

         // Check for inclusions/exclusions
         if (included != null && included.matchesResourcePath(path) == false)
            return;
         if (excluded != null && excluded.matchesResourcePath(path))
            return;
         
         ResourceContext resource = new VFSResourceContext(file, path, classLoader);
         
         //Check the filter and visit
         if (filter == null || filter.accepts(resource))
            visitor.visit(resource);
      }
      catch (Exception e)
      {
         throw new Error("Error visiting " + file, e);
      }
   }
}