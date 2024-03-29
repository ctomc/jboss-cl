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
package org.jboss.test.classloader.filter.test;

import junit.framework.Test;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.RecursivePackageClassFilter;
import org.jboss.test.classloader.AbstractClassLoaderTestWithSecurity;

/**
 * RecursivePackageFilterUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class RecursivePackageFilterUnitTestCase extends AbstractClassLoaderTestWithSecurity
{
   public static Test suite()
   {
      return suite(RecursivePackageFilterUnitTestCase.class);
   }

   public RecursivePackageFilterUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testNothing() throws Exception
   {
      ClassFilter filter = RecursivePackageClassFilter.createRecursivePackageClassFilter();
      assertFilterNoMatchClassName("gibberish", filter);
      assertFilterNoMatchClassName("", filter);
      assertFilterNoMatchClassName(null, filter);
      assertFilterNoMatchResourcePath("x.xml", filter);
      assertFilterNoMatchPackageName("gibberish", filter);
      assertFilterNoMatchPackageName("", filter);
      assertFilterNoMatchPackageName(null, filter);
   }
   
   public void testJavaLang() throws Exception
   {
      ClassFilter filter = RecursivePackageClassFilter.createRecursivePackageClassFilter("java.lang");
      assertFilterMatchesClassName("java.lang.Object", filter);
      assertFilterMatchesClassName("java.lang.X", filter);
      assertFilterNoMatchClassName("java.lang", filter);
      assertFilterNoMatchClassName("java.lang.", filter);
      assertFilterMatchesClassName("java.lang.reflect.Method", filter);
      assertFilterNoMatchClassName("gibberish", filter);
      assertFilterNoMatchClassName("", filter);
      assertFilterNoMatchClassName(null, filter);
      assertFilterMatchesResourcePath("java/lang/something.xml", filter);
      assertFilterMatchesResourcePath("java/lang/x/something.xml", filter);
      assertFilterMatchesPackageName("java.lang", filter);
      assertFilterNoMatchPackageName("java.langx", filter);
      assertFilterMatchesPackageName("java.lang.X.", filter);
      assertFilterNoMatchPackageName("gibberish", filter);
      assertFilterNoMatchPackageName("", filter);
      assertFilterNoMatchPackageName(null, filter);
   }
   
   public void testJavaLangAndJavaLangReflect() throws Exception
   {
      ClassFilter filter = RecursivePackageClassFilter.createRecursivePackageClassFilter("java.lang", "java.lang.reflect");
      assertFilterMatchesClassName("java.lang.Object", filter);
      assertFilterMatchesClassName("java.lang.X", filter);
      assertFilterMatchesClassName("java.lang.reflect.Method", filter);
      assertFilterMatchesClassName("java.lang.reflect.X", filter);
      assertFilterMatchesClassName("java.lang.reflect", filter);
      assertFilterNoMatchClassName("java.lang", filter);
      assertFilterNoMatchClassName("java.lang.", filter);
      assertFilterMatchesClassName("java.lang.reflect.", filter);
      assertFilterMatchesClassName("java.lang.reflect.subpackage.Something", filter);
      assertFilterNoMatchClassName("gibberish", filter);
      assertFilterNoMatchClassName("", filter);
      assertFilterNoMatchClassName(null, filter);
      assertFilterMatchesPackageName("java.lang", filter);
      assertFilterNoMatchPackageName("java.langx", filter);
      assertFilterMatchesPackageName("java.lang.X.", filter);
      assertFilterMatchesPackageName("java.lang.reflect", filter);
      assertFilterMatchesPackageName("java.lang.reflectx", filter);
      assertFilterMatchesPackageName("java.lang.reflect.X", filter);
      assertFilterNoMatchPackageName("gibberish", filter);
      assertFilterNoMatchPackageName("", filter);
      assertFilterNoMatchPackageName(null, filter);
   }
   
   public void testFromString() throws Exception
   {
      ClassFilter filter = RecursivePackageClassFilter.createRecursivePackageClassFilterFromString("java.lang,java.lang.reflect");
      assertFilterMatchesClassName("java.lang.Object", filter);
      assertFilterMatchesClassName("java.lang.X", filter);
      assertFilterMatchesClassName("java.lang.reflect.Method", filter);
      assertFilterMatchesClassName("java.lang.reflect.X", filter);
      assertFilterMatchesClassName("java.lang.reflect", filter);
      assertFilterNoMatchClassName("java.lang", filter);
      assertFilterNoMatchClassName("java.lang.", filter);
      assertFilterMatchesClassName("java.lang.reflect.", filter);
      assertFilterMatchesClassName("java.lang.reflect.subpackage.Something", filter);
      assertFilterNoMatchClassName("gibberish", filter);
      assertFilterNoMatchClassName("", filter);
      assertFilterNoMatchClassName(null, filter);
      assertFilterMatchesPackageName("java.lang", filter);
      assertFilterNoMatchPackageName("java.langx", filter);
      assertFilterMatchesPackageName("java.lang.X.", filter);
      assertFilterMatchesPackageName("java.lang.reflect", filter);
      assertFilterMatchesPackageName("java.lang.reflectx", filter);
      assertFilterMatchesPackageName("java.lang.reflect.X", filter);
      assertFilterNoMatchPackageName("gibberish", filter);
      assertFilterNoMatchPackageName("", filter);
      assertFilterNoMatchPackageName(null, filter);
   }
   
   public void testDefaultPackage() throws Exception
   {
      ClassFilter filter = RecursivePackageClassFilter.createRecursivePackageClassFilter("");
      assertFilterMatchesClassName("Root", filter);
      assertFilterMatchesClassName("java.NotRoot", filter);
      assertFilterMatchesClassName("", filter);
      assertFilterNoMatchClassName(null, filter);
      assertFilterMatchesResourcePath("Root", filter);
      assertFilterMatchesResourcePath("Root.xml", filter);
      assertFilterMatchesResourcePath("", filter);
      assertFilterMatchesResourcePath("java/Root.xml", filter);
      assertFilterNoMatchResourcePath(null, filter);
      assertFilterMatchesPackageName("", filter);
      assertFilterMatchesPackageName("java", filter);
      assertFilterNoMatchPackageName(null, filter);
   }
}
