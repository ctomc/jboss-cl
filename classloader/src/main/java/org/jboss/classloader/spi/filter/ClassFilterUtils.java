/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.classloader.spi.filter;

import java.util.List;

import org.jboss.classloader.plugins.filter.EverythingClassFilter;
import org.jboss.classloader.plugins.filter.JavaOnlyClassFilter;
import org.jboss.classloader.plugins.filter.NothingButJavaClassFilter;
import org.jboss.classloader.plugins.filter.NothingClassFilter;

/**
 * ClassFilterUtils.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ClassFilterUtils
{
   /** Match evertything */
   public static final ClassFilter EVERYTHING = EverythingClassFilter.INSTANCE;

   /** Match nothing */
   public static final ClassFilter NOTHING = NothingClassFilter.INSTANCE;

   /** Match nothing */
   public static final ClassFilter NOTHING_BUT_JAVA = NothingButJavaClassFilter.INSTANCE;

   /** Java Only */
   public static final ClassFilter JAVA_ONLY = JavaOnlyClassFilter.INSTANCE;

   /**
    * Create a package class filter<p>
    * 
    * Creates the filter from a comma seperated list
    * 
    * @param string the string
    * @return the filter
    */
   public static PackageClassFilter createPackageClassFilterFromString(String string)
   {
      return PackageClassFilter.createPackageClassFilterFromString(string);
   }
   
   /**
    * Create a new package class filter
    * 
    * @param packageNames the package names
    * @return the filter
    * @throws IllegalArgumentException for null packageNames
    */
   public static PackageClassFilter createPackageClassFilter(String... packageNames)
   {
      return PackageClassFilter.createPackageClassFilter(packageNames);
   }
   
   /**
    * Create a new package class filter
    * 
    * @param packageNames the package names
    * @return the filter
    * @throws IllegalArgumentException for null packageNames
    */
   public static PackageClassFilter createPackageClassFilter(List<String> packageNames)
   {
      return PackageClassFilter.createPackageClassFilter(packageNames);
   }

   /**
    * Create a recursive package class filter<p>
    * 
    * Creates the filter from a comma seperated list
    * 
    * @param string the string
    * @return the filter
    */
   public static RecursivePackageClassFilter createRecursivePackageClassFilterFromString(String string)
   {
      return RecursivePackageClassFilter.createRecursivePackageClassFilterFromString(string);
   }
   
   /**
    * Create a new recursive package class filter
    * 
    * @param packageNames the package names
    * @return the filter
    * @throws IllegalArgumentException for null packageNames
    */
   public static RecursivePackageClassFilter createRecursivePackageClassFilter(String... packageNames)
   {
      return RecursivePackageClassFilter.createRecursivePackageClassFilter(packageNames);
   }
   
   /**
    * Create a new recursive package class filter
    * 
    * @param packageNames the package names
    * @return the filter
    * @throws IllegalArgumentException for null packageNames
    */
   public static RecursivePackageClassFilter createRecursivePackageClassFilter(List<String> packageNames)
   {
      return RecursivePackageClassFilter.createRecursivePackageClassFilter(packageNames);
   }
}
