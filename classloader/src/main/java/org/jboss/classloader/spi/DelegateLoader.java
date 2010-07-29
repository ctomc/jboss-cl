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
package org.jboss.classloader.spi;

import org.jboss.classloader.spi.base.BaseDelegateLoader;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.ClassFilterUtils;

/**
 * DelegateLoader.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class DelegateLoader extends BaseDelegateLoader
{
   /** The import type */
   private ImportType importType = ImportType.BEFORE;

   /**
    * Create a new DelegateLoader.
    * 
    * @param delegate the delegate
    * @throws IllegalArgumentException for a null delegate
    */
   public DelegateLoader(ClassLoaderPolicy delegate)
   {
      super(delegate);
   }

   /**
    * Create a new DelegateLoader.
    * 
    * @param factory the factory
    * @throws IllegalArgumentException for a null delegate
    */
   public DelegateLoader(ClassLoaderPolicyFactory factory)
   {
      super(factory);
   }

   /**
    * Get the filter.
    *
    * @return the filter
    */
   public ClassFilter getFilter()
   {
      return ClassFilterUtils.EVERYTHING;
   }

   /**
    * Get the ClassLoaderPolicy associated with this DelegateLoader.
    *
    * @return the class loader policy
    */
   public ClassLoaderPolicy getPolicy()
   {
      return (ClassLoaderPolicy)super.getPolicy();
   }

   /**
    * Get import type.
    *
    * @return the import type
    */
   public ImportType getImportType()
   {
      return importType;
   }

   /**
    * Set import type.
    *
    * @param importType the import type
    */
   public void setImportType(ImportType importType)
   {
      if (importType == null)
         throw new IllegalArgumentException("Null import type");
      this.importType = importType;
   }
}
