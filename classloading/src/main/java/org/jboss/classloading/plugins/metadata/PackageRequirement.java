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

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.OptionalPackages;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.metadata.helpers.AbstractRequirement;
import org.jboss.classloading.spi.version.VersionRange;

/**
 * PackageRequirement.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class PackageRequirement extends AbstractRequirement implements OptionalPackages
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -7552921085464308835L;

   /**
    * Create a new PackageRequirement.
    */
   public PackageRequirement()
   {
   }
   
   /**
    * Create a new PackageRequirement with no version constraint
    * 
    * @param name the name
    * @throws IllegalArgumentException for a null name
    */
   public PackageRequirement(String name)
   {
      super(name);
   }
   
   /**
    * Create a new PackageRequirement.
    * 
    * @param name the name
    * @param versionRange the version range - pass null for all versions
    * @throws IllegalArgumentException for a null name
    */
   public PackageRequirement(String name, VersionRange versionRange)
   {
      super(name, versionRange);
   }
   
   public Set<String> getOptionalPackageNames(Module module)
   {
      if (isOptional() == false)
         return null;
      return Collections.singleton(getName());
   }

   @Override
   public boolean isConsistent(Requirement other)
   {
      return isConsistent(other, PackageRequirement.class);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof PackageRequirement == false)
         return false;
      return super.equals(obj);
   }
}