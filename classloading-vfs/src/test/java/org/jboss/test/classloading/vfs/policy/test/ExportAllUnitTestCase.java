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
package org.jboss.test.classloading.vfs.policy.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.test.BaseTestCase;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * ExportAllUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ExportAllUnitTestCase extends BaseTestCase
{
   protected void testExportAll(ExportAll exportAll, Map<String, String> expected, String... urls) throws Exception
   {
      testExportAll(exportAll, expected, urls, null);
   }

   protected void testExportAll(ExportAll exportAll, Map<String, String> expected, Set<String> empty, String... urls) throws Exception
   {
      testExportAll(exportAll, expected, empty, urls, null);
   }
   protected void testExportAll(ExportAll exportAll, Map<String, String> expected, String[] urls, String[] excluded) throws Exception
   {
      Set<String> empty = Collections.emptySet();
      testExportAll(exportAll, expected, empty, urls, excluded);
   }

   protected void testExportAll(ExportAll exportAll, Map<String, String> expected, Set<String> empty, String[] urls, String[] excluded) throws Exception
   {
      testExportAllAbsolute(exportAll, expected, empty, urls, excluded);
      testExportAllFromBase(exportAll, expected, empty, urls, excluded);
   }

   protected void testExportAllCommon(ExportAll exportAll, Map<String, String> expected, Set<String> empty, VirtualFile[] files, VirtualFile[] excluded) throws Exception
   {
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(files, excluded);
      policy.setExportAll(exportAll);
      
      String[] packageNames = policy.getPackageNames();
      Set<String> actual = makeSet(packageNames);
      assertEquals(expected.keySet(), actual);
      
      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      
      for (Map.Entry<String, String> entry : expected.entrySet())
      {
         String packageName = entry.getKey();
         String resource = packageName.replace('.', '/') + "/notempty";
         InputStream is = classLoader.getResourceAsStream(resource);
         if (empty.contains(packageName))
            assertNull("Did not expect resource: " + resource, is);
         else
         {
            assertNotNull("Did not find resource: " + resource, is);
            String contents = getContents(is);
            assertEquals(entry.getValue(), contents);
         }
      }
   }

   protected void testExportAllFromBase(ExportAll exportAll, Map<String, String> expected, Set<String> empty, String[] urls, String[] excluded) throws Exception
   {
      URL baseURL = getResource("/classloader");
      assertNotNull(baseURL);
      VirtualFile base = VFS.getChild(baseURL);
      VirtualFile[] files = new VirtualFile[urls.length];
      for (int i = 0; i < urls.length; ++i)
      {
         files[i] = base.getChild(urls[i]);
         if (files[i].exists() == false && i > 0)
         {
            files[i] = files[0].getChild(urls[i]);
         }
         if (files[i].exists() == false)
            fail("Can't find " + urls[i]);
      }
      
      VirtualFile[] excludedFiles = null;
      if (excluded != null)
      {
         excludedFiles = new VirtualFile[excluded.length];
         for (int i = 0; i < excluded.length; ++i)
         {
            excludedFiles[i] = base.getChild(excluded[i]);
            if (excludedFiles[i].exists() == false)
            {
               excludedFiles[i] = files[0].getChild(excluded[i]);
            }
            if (excludedFiles[i].exists() == false)
               fail("Can't find " + excluded[i]);
         }
      }
      
      testExportAllCommon(exportAll, expected, empty, files, excludedFiles);
   }

   protected void testExportAllAbsolute(ExportAll exportAll, Map<String, String> expected, Set<String> empty, String[] urls, String[] excluded) throws Exception
   {
      VirtualFile[] files = new VirtualFile[urls.length];
      for (int i = 0; i < urls.length; ++i)
      {
         String urlString = "/classloader/" + urls[i];
         URL url = getResource(urlString);
         if (url != null)
         {
            files[i]= VFS.getChild(url);
         }
         else
         {
            if (i > 0)
               files[i] = files[0].getChild(urls[i]);
            if (files[i] == null)
               fail("Expected to find resource: " + urlString);
         }
      }
      
      VirtualFile[] excludedFiles = null;
      if (excluded != null)
      {
         excludedFiles = new VirtualFile[excluded.length];
         for (int i = 0; i < excluded.length; ++i)
         {
            String urlString = "/classloader/" + excluded[i];
            URL url = getResource(urlString);
            if (url != null)
            {
               excludedFiles[i]= VFS.getChild(url);
            }
            else
            {
               excludedFiles[i] = files[0].getChild(excluded[i]);
               if (excludedFiles[i] == null)
                  fail("Expected to find resource: " + files[0].getName() + "/" + excluded[i]);
            }
         }
      }
      
      testExportAllCommon(exportAll, expected, empty, files, excludedFiles);
   }
   
   public void testExportAllJar1() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar1",
            "",
            "package1",
            "package2", 
            "package2.subpackage1",
            "package2.subpackage2",
            "package2.subpackage3"
      );

      testExportAll(ExportAll.ALL, expected, "testjar1");
   }
   public void testJar1Resources()
      throws Exception
   {
      URL testjar1URL = getResource("/classloader/testjar1");
      VirtualFile testjar1 = VFS.getChild(testjar1URL);
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(testjar1);
      policy.setExportAll(ExportAll.ALL);
      
      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      URL notempty = classLoader.getResource("notempty");
      assertNotNull(notempty);
   }
   public void testWar1Resources()
      throws Exception
   {
      URL testwar1URL = getResource("/classloader/testwar1.war");
      VirtualFile testwar1 = VFS.getChild(testwar1URL);
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(testwar1);
      policy.setExportAll(ExportAll.NON_EMPTY);
      policy.setImportAll(true);

      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      URL resURL = classLoader.getResource("test-resource.txt");
      assertNull(resURL);
      resURL = classLoader.getResource("WEB-INF/test-resource.txt");
      assertNotNull(resURL);
   }

   public void testExportAllJar1NonEmpty() throws Exception
   {
      Map<String, String> expected = makeSimpleMap("testjar1",
            "",
            "package1",
            "package2", 
            "package2.subpackage1",
            "package2.subpackage2",
            "package2.subpackage3"
      );

      testExportAll(ExportAll.NON_EMPTY, expected, "testjar1");
   }
   
   public void testExportAllJar2() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar2",
            "",
            "package1"
      );
      
      Set<String> empty = makeSet("");

      testExportAll(ExportAll.ALL, expected, empty, "testjar2");
   }

   public void testExportAllJar2NonEmpty() throws Exception
   {
      Map<String, String> expected = makeSimpleMap("testjar2",
            "package1"
      );

      testExportAll(ExportAll.NON_EMPTY, expected, "testjar2");
   }
   
   public void testExportAllJar1And2() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar1",
            "",
            "package1",
            "package2", 
            "package2.subpackage1",
            "package2.subpackage2",
            "package2.subpackage3"
      );

      testExportAll(ExportAll.ALL, expected, "testjar1", "testjar2");
   }

   public void testExportAllJar1And2NonEmpty() throws Exception
   {
      Map<String, String> expected = makeSimpleMap("testjar1",
            "",
            "package1",
            "package2", 
            "package2.subpackage1",
            "package2.subpackage2",
            "package2.subpackage3"
      );

      testExportAll(ExportAll.NON_EMPTY, expected, "testjar1", "testjar2");
   }
   
   public void testExportAllJar2And1() throws Exception
   {
      Map<String,String> expected = makeComplexMap(
            "", "testjar1",
            "package1", "testjar2",
            "package2", "testjar1",
            "package2.subpackage1", "testjar1",
            "package2.subpackage2", "testjar1",
            "package2.subpackage3", "testjar1"
      );

      testExportAll(ExportAll.ALL, expected, "testjar2", "testjar1");
   }

   public void testExportAllJar2And1NonEmpty() throws Exception
   {
      Map<String, String> expected = makeComplexMap(
            "", "testjar1",
            "package1", "testjar2",
            "package2", "testjar1",
            "package2.subpackage1", "testjar1",
            "package2.subpackage2", "testjar1",
            "package2.subpackage3", "testjar1"
      );

      testExportAll(ExportAll.NON_EMPTY, expected, "testjar2", "testjar1");
   }
   
   public void testExportAllJar3() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar3",
            "",
            "package1",
            "package2"
      );

      testExportAll(ExportAll.ALL, expected, "testjar3", "subjar1.jar");
   }
   
   public void testExportAllSimpleExcluded() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar1",
            "",
            "package1"
      );

      testExportAll(ExportAll.ALL, expected, new String[] { "testjar1" } , new String[] { "package2" });
   }
   
   public void testExportAllMultipleRootsExcluded() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar1",
            "",
            "package1"
      );

      testExportAll(ExportAll.ALL, expected, new String[] { "testjar1", "testjar2" } , new String[] { "package2" });
   }
   
   public void testExportAllMultipleExcluded() throws Exception
   {
      Map<String,String> expected = makeSimpleMap("testjar1",
            ""
      );

      testExportAll(ExportAll.ALL, expected, new String[] { "testjar1" } , new String[] { "package1", "package2" });
   }
   
   public void testJar3Resources()
      throws Exception
   {
      URL testjar3URL = getResource("/classloader/testjar3");
      VirtualFile testjar3 = VFS.getChild(testjar3URL);
      VirtualFile testjar3subjar = testjar3.getChild("subjar1.jar");
      assertNotNull(testjar3subjar);
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(testjar3, testjar3subjar);
      policy.setExportAll(ExportAll.NON_EMPTY);
      
      ClassLoaderSystem system = new DefaultClassLoaderSystem();
      ClassLoader classLoader = system.registerClassLoaderPolicy(policy);
      URL notempty = classLoader.getResource("notempty");
      assertNotNull(notempty);
      notempty = classLoader.getResource("package1/notempty");
      assertNotNull(notempty);
      notempty = classLoader.getResource("package2/notempty");
      assertNotNull(notempty);
   }

   protected String getContents(InputStream is) throws Exception
   {
      StringBuilder builder = new StringBuilder();
      InputStreamReader reader = new InputStreamReader(is);
      int character = reader.read();
      while (character != -1)
      {
         builder.append((char) character);
         character = reader.read();
      }
      return builder.toString();
   }
   
   protected Set<String> makeSet(String... elements)
   {
      assertNotNull(elements);
      return new HashSet<String>(Arrays.asList(elements));
   }
   
   protected Map<String,String> makeSimpleMap(String prefix, String... elements)
   {
      assertNotNull(prefix);
      assertNotNull(elements);
      Map<String, String> result = new HashMap<String, String>();
      for (String string : elements)
         result.put(string, prefix + "." + string);
      return result;
   }
   
   protected Map<String,String> makeComplexMap(String... elements)
   {
      assertNotNull(elements);
      Map<String, String> result = new HashMap<String, String>();
      for (int i = 0; i < elements.length; i += 2)
         result.put(elements[i], elements[i+1] + '.' + elements[i]);
      return result;
   }

   public static Test suite()
   {
      return new TestSuite(ExportAllUnitTestCase.class);
   }

   public ExportAllUnitTestCase(String name) throws Throwable
   {
      super(name);
   }
}
