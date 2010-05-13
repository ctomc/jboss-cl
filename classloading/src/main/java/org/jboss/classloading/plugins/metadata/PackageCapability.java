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
package org.jboss.classloading.plugins.metadata;

import java.util.Collections;
import java.util.Set;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.ExportPackages;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.metadata.helpers.AbstractCapability;

/**
 * PackageCapability.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class PackageCapability extends AbstractCapability implements ExportPackages
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 948069557003560933L;

   /** The supported values for the split package poicy */
   public enum SplitPackagePolicy 
   {
      /** The first module wins */
      First, 
      /** The last module wins */
      Last, 
      /** Split packages generate an error. This is the default. */
      Error 
   }
   
   /** The split package policy. Default is {@link SplitPackagePolicy#Error} */
   private SplitPackagePolicy splitPolicy = SplitPackagePolicy.Error;
   
   /**
    * Create a new PackageCapability.
    */
   public PackageCapability()
   {
   }
   
   /**
    * Create a new PackageCapability with the default version
    * 
    * @param name the name
    * @throws IllegalArgumentException for a null name
    */
   public PackageCapability(String name)
   {
      super(name);
   }
   
   /**
    * Create a new PackageCapability.
    * 
    * @param name the name
    * @param version the version - pass null for default version
    * @throws IllegalArgumentException for a null name
    */
   public PackageCapability(String name, Object version)
   {
      super(name, version);
   }

   /**
    * Create a new PackageCapability.
    * 
    * @param name the name
    * @param version the version - pass null for default version
    * @param splitPolicy the split package policy
    * @throws IllegalArgumentException for a null name
    */
   public PackageCapability(String name, Object version, SplitPackagePolicy splitPolicy)
   {
      super(name, version);
      this.splitPolicy = splitPolicy;
   }

   /** 
    * Get the split package policy
    * 
    * @return the split package policy
    */
   public SplitPackagePolicy getSplitPackagePolicy()
   {
      return splitPolicy;
   }

   /** 
    * Set the split package policy
    * 
    * @param policy the split package policy
    */
   public void setSplitPackagePolicy(SplitPackagePolicy policy)
   {
      this.splitPolicy = policy;
   }

   public boolean resolves(Module reqModule, Requirement requirement)
   {
      if (requirement instanceof PackageRequirement == false)
         return false;

      PackageRequirement requirePackage = (PackageRequirement) requirement;
      if (requirePackage.isWildcard())
      {
         ClassFilter filter = requirePackage.toClassFilter();
         if (filter.matchesPackageName(getName()) == false)
            return false;
      }
      else // for non-wildcard, we intentionaly still use direct string equals
      {
         if (getName().equals(requirePackage.getName()) == false)
            return false;
      }
      return requirePackage.getVersionRange().isInRange(getVersion());
   }
   
   public Set<String> getPackageNames(Module module)
   {
      return Collections.singleton(getName());
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof PackageCapability == false)
         return false;
      return super.equals(obj);
   }
   
   // FINDBUGS: Just to keep it happy
   @Override
   public int hashCode()
   {
      return super.hashCode();
   }
}
