/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.classloading.metadata.xml.support;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.jboss.classloader.spi.Loader;

/**
 * Test laoder.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TestLoader implements Loader
{
   public Class<?> loadClass(String className)
   {
      return null;
   }

   public URL getResource(String name)
   {
      return null;
   }

   public void getResources(String name, Set<URL> urls) throws IOException
   {
   }

   public Package getPackage(String name)
   {
      return null;
   }

   public void getPackages(Set<Package> packages)
   {
   }
}
