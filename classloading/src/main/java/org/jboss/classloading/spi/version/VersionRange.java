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
package org.jboss.classloading.spi.version;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * VersionRange.
 * 
 * @author <a href="adrian@jboss.org">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VersionRange implements Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 8494384641173842116L;

   /** The low range */
   private Object low;
   
   /** Whether low is inclusive */
   private boolean lowInclusive;
   
   /** The high range */
   private Object high;
   
   /** Whether high is inclusive */
   private boolean highInclusive;
   
   /** All versions */
   public static final VersionRange ALL_VERSIONS = new VersionRange(Version.DEFAULT_VERSION);
   
   /**
    * Get the version range from a string
    * 
    * @param rangeSpec the range spec
    * @return the version range
    */
   public static VersionRange valueOf(String rangeSpec)
   {
      return parseRangeSpec(rangeSpec);
   }

   /**
    * Parse a range spec
    * 
    * @param rangeSpec string version
    * @return the version range
    */
   public static VersionRange parseRangeSpec(String rangeSpec)
   {
      if (rangeSpec == null)
         throw new IllegalArgumentException("Null rangeSpec");
      
      rangeSpec = rangeSpec.trim();

      int length = rangeSpec.length();
      if (length == 0)
         return ALL_VERSIONS;

      char start = rangeSpec.charAt(0);
      // Single version?
      if (start != '[' && start != '(')
      {
         Version version = Version.parseVersion(rangeSpec);
         return new VersionRange(version, true, version, true);
      }
      
      if (length < 2)
         throw new IllegalArgumentException("Expected a closing ] or ) for version range: " + rangeSpec);

      boolean floorIsInclusive = true;
      boolean ceilingIsInclusive = true;

      if (start == '(')
         floorIsInclusive = false;
      
      char end = rangeSpec.charAt(length-1);
      if (end == ')')
         ceilingIsInclusive = false;
      else if (end != ']')
         throw new IllegalArgumentException("Expected a closing ] or ) for version range: " + rangeSpec);

      Version floor = null;
      Version ceiling = null;
      StringTokenizer st = new StringTokenizer(rangeSpec.substring(1, length-1), ",", true);
      boolean mid = false;
      while (st.hasMoreTokens())
      {
         String token = st.nextToken();
         if (token.equals(","))
         {
            if (mid == true)
               throw new IllegalArgumentException("Expected only one , in version range: " + rangeSpec);
            mid = true;
         }
         else
         {
            try
            {
               // A version token
               if (mid == false)
                  floor = Version.parseVersion(token);
               else
                  ceiling = Version.parseVersion(token);
            }
            catch (RuntimeException e)
            {
               throw new IllegalArgumentException("Error parsing version '" + token + "' in " + rangeSpec, e);
            }
         }

      }

      // This is a hack to support [1.0.0] as a specified version
      if (ceiling == null && ceilingIsInclusive)
         ceiling = floor;
      
      return new VersionRange(floor, floorIsInclusive, ceiling, ceilingIsInclusive);
   }
   
   /**
    * Create a new VersionRange with just a low inclusive check
    * 
    * @param low the low range (null for no lower bound)
    */
   public VersionRange(Object low)
   {
      this(low, null);
   }
   
   /**
    * Create a new VersionRange with low inclusive and high non-inclusive
    * 
    * @param low the low range (null for no lower bound)
    * @param high the high range (null for no higher bound)
    */
   public VersionRange(Object low, Object high)
   {
      this(low, true, high, false);
   }
   
   /**
    * Create a new VersionRange.
    * 
    * @param low the low range (null for no lower bound)
    * @param lowInclusive whether the low bound is inclusive
    * @param high the high range (null for no higher bound)
    * @param highInclusive whether the high bound is inclusive
    * @throws IllegalArgumentException if the low or is inclusive but not in the range or the high is less than the low 
    */
   public VersionRange(Object low, boolean lowInclusive, Object high, boolean highInclusive)
   {
      if (low == null)
         low = Version.DEFAULT_VERSION;
      this.low = low;
      this.lowInclusive = lowInclusive;
      this.high = high;
      this.highInclusive = highInclusive;
      validate();
   }

   /**
    * Validate the range
    * 
    * @throws IllegalArgumentException for any error
    */
   protected void validate()
   {
      if (lowInclusive && isInRange(low) == false)
         throw new IllegalArgumentException("Inclusive low is not in the range: " + toString());
      if (high != null && highInclusive && isInRange(high) == false)
         throw new IllegalArgumentException("Inclusive high is not in the range: " + toString());
      if (high != null)
      {
         VersionComparatorRegistry registry = VersionComparatorRegistry.getInstance();
         int comparison = registry.compare(low, high);
         if (comparison > 0)
            throw new IllegalArgumentException("High is less than the low: " + toString());
         if (comparison == 0)
         {
            if (lowInclusive == false || highInclusive == false)
               throw new IllegalArgumentException("High and low don't include each other: " + toString());
         }
      }
   }
   
   /**
    * Get the low.
    * 
    * @return the low.
    */
   public Object getLow()
   {
      return low;
   }

   /**
    * Get the lowInclusive.
    * 
    * @return the lowInclusive.
    */
   public boolean isLowInclusive()
   {
      return lowInclusive;
   }

   /**
    * Get the high.
    * 
    * @return the high.
    */
   public Object getHigh()
   {
      return high;
   }

   /**
    * Get the highInclusive.
    * 
    * @return the highInclusive.
    */
   public boolean isHighInclusive()
   {
      return highInclusive;
   }

   /**
    * Test whether a version is in range
    * 
    * @param version the version to test
    * @return true when the version is in range
    * @throws IllegalArgumentException for a null version
    */
   public boolean isInRange(Object version)
   {
      if (version == null)
         throw new IllegalArgumentException("Null version");
      
      VersionComparatorRegistry comparator = VersionComparatorRegistry.getInstance();
      
      int comparison = comparator.compare(low, version);
      if (comparison > 0)
         return false;
      if (lowInclusive == false && comparison == 0)
         return false;

      if (high != null)
      {
         comparison = comparator.compare(high, version);
         if (comparison < 0)
            return false;
         if (highInclusive == false && comparison == 0)
            return false;
      }
      return true;
   }

   /**
    * Check whether two version ranges are consistent
    * 
    * @param other the other version
    * @return true when the version ranges "overlap"
    */
   public boolean isConsistent(VersionRange other)
   {
      // No version range is consistent with ours
      if (other == null)
         return true;
      
      Object otherLow = other.getLow();
      Object otherHigh = other.getHigh();
      
      VersionComparatorRegistry comparator = VersionComparatorRegistry.getInstance();
      
      // Other low is "lower"
      int comparison = comparator.compare(low, otherLow);
      if (comparison > 0 || (lowInclusive == false && comparison == 0))
      {
         // Just need to check that the other high is not lower
         if (otherHigh == null)
            return true;
         comparison = comparator.compare(low, otherHigh);
         return (comparison > 0 || (lowInclusive == false && comparison == 0)) == false;
      }

      // So the other low is "bigger" than our low
      
      // We have no high so we are done
      if (high == null)
         return true;
      
      // Check the other low is not "bigger" than our higher
      comparison = comparator.compare(high, otherLow);
      if (comparison < 0 || (highInclusive == false && comparison == 0))
         return false;
      
      // The low is in our range so we are done
      return true;
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof VersionRange == false)
         return false;
      
      VersionRange other = (VersionRange) obj;
      
      VersionComparatorRegistry comparator = VersionComparatorRegistry.getInstance();
      
      Object thisLow = other.getLow();
      Object otherLow = other.getLow();
      if (comparator.same(thisLow, otherLow) == false)
         return false;
      
      if (isLowInclusive() != other.isLowInclusive())
         return false;
      
      Object thisHigh = this.getHigh();
      Object otherHigh = other.getHigh();
      if (thisHigh == null)
      {
         if (otherHigh != null)
            return false;
      }
      else if (comparator.same(thisHigh, otherHigh) == false)
         return false;
      
      if (thisHigh != null && isHighInclusive() != other.isHighInclusive())
         return false;
      
      return true;
   }
   
   @Override 
   public int hashCode()
   {
      if (high != null)
         return high.hashCode();
      return low.hashCode();
   }
   
   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      if (isLowInclusive())
         builder.append("[");
      else
         builder.append("(");
      builder.append(low);
      builder.append(",");
      if (high != null)
      {
         builder.append(high);
         if (isHighInclusive())
            builder.append("]");
         else
            builder.append(")");
      }
      else
      {
         builder.append("?)");
      }
      return builder.toString();
   }
}
