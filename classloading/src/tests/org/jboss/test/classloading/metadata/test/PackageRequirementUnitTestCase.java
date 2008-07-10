/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloading.metadata.test;

import junit.framework.Test;

import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.plugins.metadata.UsesPackageRequirement;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.test.classloading.AbstractClassLoadingTestWithSecurity;
import org.jboss.test.classloading.metadata.xml.support.TestRequirement;

/**
 * PackageRequirementUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class PackageRequirementUnitTestCase extends AbstractClassLoadingTestWithSecurity
{
   public static Test suite()
   {
      return suite(PackageRequirementUnitTestCase.class);
   }

   public PackageRequirementUnitTestCase(String name)
   {
      super(name);
   }
   
   public void testConstructors() throws Exception
   {
      PackageRequirement test = new PackageRequirement();
      assertNotNull(test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());

      test = new PackageRequirement("test");
      assertEquals("test", test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());

      VersionRange range = new VersionRange("1.0.0", "2.0.0");
      test = new PackageRequirement("test", range);
      assertEquals("test", test.getName());
      assertEquals(range, test.getVersionRange());
      
      try
      {
         fail("Should not be here for: " + new PackageRequirement(null));
      }
      catch (Throwable t)
      {
         checkDeepThrowable(IllegalArgumentException.class, t);
      }
      
      try
      {
         fail("Should not be here for: " + new PackageRequirement(null, range));
      }
      catch (Throwable t)
      {
         checkDeepThrowable(IllegalArgumentException.class, t);
      }
   }
   
   public void testSetName() throws Exception
   {
      PackageRequirement test = new PackageRequirement();
      assertNotNull(test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());
      test.setName("name");
      assertEquals("name", test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());
      
      test = new PackageRequirement();
      try
      {
         test.setName(null);
      }
      catch (Throwable t)
      {
         checkDeepThrowable(IllegalArgumentException.class, t);
      }
   }
   
   public void testSetVersionRange() throws Exception
   {
      PackageRequirement test = new PackageRequirement();
      assertNotNull(test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());
      VersionRange range = new VersionRange("1.0.0", "2.0.0");
      test.setVersionRange(range);
      assertNotNull(test.getName());
      assertEquals(range, test.getVersionRange());
      test.setVersionRange(null);
      assertNotNull(test.getName());
      assertEquals(VersionRange.ALL_VERSIONS, test.getVersionRange());
   }
      
   public void testEquals() throws Exception
   {
      testEquals("a", VersionRange.ALL_VERSIONS, "a", VersionRange.ALL_VERSIONS, true);
      testEquals("a", VersionRange.ALL_VERSIONS, "a", null, true);
      VersionRange range1 = new VersionRange("1.0.0", true, "1.0.0", true);
      testEquals("b", range1, "b", range1, true);
      
      testEquals("a", VersionRange.ALL_VERSIONS, "b", VersionRange.ALL_VERSIONS, false);
      testEquals("a", range1, "a", VersionRange.ALL_VERSIONS, false);
      VersionRange range2 = new VersionRange("1.0.0", true, "2.0.0", true);
      testEquals("a", range1, "a", range2, false);
   }
   
   public void testIsConsistent() throws Exception
   {
      testIsConsistent("a", VersionRange.ALL_VERSIONS, "a", VersionRange.ALL_VERSIONS, true);
      testIsConsistent("a", VersionRange.ALL_VERSIONS, "a", null, true);
      testIsConsistent("a", null, "a", VersionRange.ALL_VERSIONS, true);

      testIsConsistent("a", "1.0.0", "2.0.0", "a", "1.0.0", "2.0.0", true);
      testIsConsistent("a", "1.0.0", "2.0.0", "a", "2.0.0", "2.0.0", true);
      testIsConsistent("a", "1.0.0", "2.0.0", "a", "1.0.0", "1.0.0", true);

      testIsConsistent("a", "1.0.0", "2.0.0", "b", "1.0.0", "2.0.0", true);
      testIsConsistent("a", "1.0.0", "2.0.0", "b", "0.0.1", "0.0.1", true);
      testIsConsistent("a", "1.0.0", "2.0.0", "b", "2.0.1", "2.0.1", true);

      testIsConsistent("a", "1.0.0", "2.0.0", "a", "0.0.1", "0.0.1", false);
      testIsConsistent("a", "1.0.0", "2.0.0", "a", "2.0.1", "2.0.1", false);

      testIsConsistentOther("a", "1.0.0", "2.0.0", "b", "1.0.0", "2.0.0", true);
      testIsConsistentOther("a", "1.0.0", "2.0.0", "b", "0.0.1", "0.0.1", true);
      testIsConsistentOther("a", "1.0.0", "2.0.0", "b", "2.0.1", "2.0.1", true);
      testIsConsistentOther("a", "1.0.0", "2.0.0", "a", "1.0.0", "2.0.0", true);
      testIsConsistentOther("a", "1.0.0", "2.0.0", "a", "0.0.1", "0.0.1", true);
      testIsConsistentOther("a", "1.0.0", "2.0.0", "a", "2.0.1", "2.0.1", true);

      testIsConsistentUses("a", "1.0.0", "2.0.0", "b", "1.0.0", "2.0.0", true);
      testIsConsistentUses("a", "1.0.0", "2.0.0", "b", "0.0.1", "0.0.1", true);
      testIsConsistentUses("a", "1.0.0", "2.0.0", "b", "2.0.1", "2.0.1", true);
      testIsConsistentUses("a", "1.0.0", "2.0.0", "a", "1.0.0", "2.0.0", true);
      testIsConsistentUses("a", "1.0.0", "2.0.0", "a", "0.0.1", "0.0.1", false);
      testIsConsistentUses("a", "1.0.0", "2.0.0", "a", "2.0.1", "2.0.1", false);
      
   }
   
   protected void testIsConsistent(String name1, String low1, String high1, String name2, String low2, String high2, boolean result)
   {
      VersionRange range1 = new VersionRange(low1, true, high1, true);
      VersionRange range2 = new VersionRange(low2, true, high2, true);
      testIsConsistent(name1, range1, name2, range2, result);
      testIsConsistent(name2, range2, name1, range1, result);
   }
   
   protected void testIsConsistentOther(String name1, String low1, String high1, String name2, String low2, String high2, boolean result)
   {
      VersionRange range1 = new VersionRange(low1, true, high1, true);
      VersionRange range2 = new VersionRange(low2, true, high2, true);
      PackageRequirement test1 = new PackageRequirement(name1, range1);
      TestRequirement test2 = new TestRequirement(name2, range2);
      testIsConsistent(test1, test2, result);
      testIsConsistent(test1, test2, result);
   }
   
   protected void testIsConsistentUses(String name1, String low1, String high1, String name2, String low2, String high2, boolean result)
   {
      VersionRange range1 = new VersionRange(low1, true, high1, true);
      VersionRange range2 = new VersionRange(low2, true, high2, true);
      PackageRequirement test1 = new PackageRequirement(name1, range1);
      UsesPackageRequirement test2 = new UsesPackageRequirement(name2, range2);
      testIsConsistent(test1, test2, result);
      testIsConsistent(test1, test2, result);
   }
   
   protected void testIsConsistent(String name1, VersionRange range1, String name2, VersionRange range2, boolean result)
   {
      PackageRequirement test1 = new PackageRequirement(name1, range1);
      PackageRequirement test2 = new PackageRequirement(name2, range2);
      testIsConsistent(test1, test2, result);
   }
   
   protected void testIsConsistent(Requirement test1, Requirement test2, boolean result)
   {
      if (result)
         assertTrue("Expected " + test1 + ".isConsistent(" + test2 + ") to be true", test1.isConsistent(test2));
      else
         assertFalse("Expected " + test1 + ".isConsistent(" + test2 + ") to be false", test1.isConsistent(test2));
   }
   
   public void testSerialization() throws Exception
   {
      PackageRequirement test = new PackageRequirement("a", VersionRange.ALL_VERSIONS);
      PackageRequirement other = serializeDeserialize(test, PackageRequirement.class);
      assertEquals(test, other);
   }
   
   protected void testEquals(String name1, VersionRange range1, String name2, VersionRange range2, boolean result)
   {
      PackageRequirement test1 = new PackageRequirement(name1, range1);
      PackageRequirement test2 = new PackageRequirement(name2, range2);
      if (result)
      {
         assertTrue("Expected " + test1 + ".equals(" + test2 + ") to be true", test1.equals(test2));
         assertTrue("Expected " + test2 + ".equals(" + test1 + ") to be true", test2.equals(test1));
      }
      else
      {
         assertFalse("Expected " + test1 + ".equals(" + test2 + ") to be false", test1.equals(test2));
         assertFalse("Expected " + test2 + ".equals(" + test1 + ") to be false", test2.equals(test1));
      }
   }
}
