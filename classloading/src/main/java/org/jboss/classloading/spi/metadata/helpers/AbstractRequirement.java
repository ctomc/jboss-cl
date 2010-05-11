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
package org.jboss.classloading.spi.metadata.helpers;

import javax.xml.bind.annotation.XmlAttribute;

import org.jboss.classloader.spi.ImportType;
import org.jboss.classloading.spi.helpers.NameAndVersionRangeSupport;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.metadata.RequirementWithImportType;
import org.jboss.classloading.spi.version.VersionRange;

/**
 * AbstractRequirement.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class AbstractRequirement extends NameAndVersionRangeSupport implements RequirementWithImportType
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -7898148730704557596L;

   /** Whether the requirement is optional */
   private boolean optional = false;

   /** Whether the requirement is dynamic */
   private boolean dynamic = false;

   /** Whether to re-export */
   private boolean reExport = false;

   /** The import type */
   private ImportType importType = ImportType.BEFORE;

   /**
    * Create a new AbstractRequirement
    */
   public AbstractRequirement()
   {
   }
   
   /**
    * Create a new AbstractRequirement
    * 
    * @param name the name
    * @throws IllegalArgumentException for a null name
    */
   public AbstractRequirement(String name)
   {
      super(name);
   }
   
   /**
    * Create a new AbstractRequirement.
    * 
    * @param name the name
    * @param versionRange the version range - pass null for all versions
    * @throws IllegalArgumentException for a null name
    */
   public AbstractRequirement(String name, VersionRange versionRange)
   {
      super(name, versionRange);
   }

   public boolean isOptional()
   {
      return optional;
   }
   
   /**
    * Set the optional.
    * 
    * @param optional the optional.
    */
   @XmlAttribute(name="optional")
   public void setOptional(boolean optional)
   {
      this.optional = optional;
   }

   public boolean isDynamic()
   {
      return dynamic;
   }

   /**
    * Set the dynamic.
    * 
    * @param dynamic the dynamic.
    */
   @XmlAttribute(name="dynamic")
   public void setDynamic(boolean dynamic)
   {
      this.dynamic = dynamic;
   }

   public boolean wantReExports()
   {
      return false;
   }

   public boolean isReExport()
   {
      return reExport;
   }

   /**
    * Set the reExport.
    * 
    * @param reExport the reExport.
    */
   @XmlAttribute(name="reExport")
   public void setReExport(boolean reExport)
   {
      this.reExport = reExport;
   }

   public ImportType getImportType()
   {
      return importType;
   }

   @XmlAttribute(name = "importType")
   public void setImportType(ImportType importType)
   {
      this.importType = importType;
   }

   public boolean isConsistent(Requirement other)
   {
      return isConsistent(other, null);
   }

   /**
    * Check whether the requirements are consistent
    * 
    * @param other the other requirement
    * @param requirementType the class to check when looking for inconsistencies (uses getClass() when null)
    * @return true when consistent, false otherwise
    */
   protected boolean isConsistent(Requirement other, Class<? extends AbstractRequirement> requirementType)
   {
      if (other == null)
         throw new IllegalArgumentException("Null requirement");
      if (requirementType == null)
         requirementType = getClass();
      
      // Not our type
      if (requirementType.isInstance(other) == false)
         return true;
      
      AbstractRequirement otherRequirement = (AbstractRequirement) other;
      // Not the same name
      String name = getName();
      String otherName = otherRequirement.getName();
      if (name.equals(otherName) == false)
         return true;

      // Check the version ranges are consistent
      VersionRange range = getVersionRange();
      VersionRange otherRange = otherRequirement.getVersionRange();
      return range.isConsistent(otherRange);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof AbstractRequirement == false)
         return false;
      if (super.equals(obj) == false)
         return false;
      AbstractRequirement other = (AbstractRequirement) obj;
      if (this.isOptional() != other.isOptional())
         return false;
      if (this.isReExport() != other.isReExport())
         return false;
      return true;
   }
   
   // FINDBUGS: Just to keep it happy
   @Override
   public int hashCode()
   {
      return super.hashCode();
   }

   @Override
   public String toString()
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append(getClass().getSimpleName());
      buffer.append("{");
      toString(buffer);
      buffer.append("}");
      return buffer.toString();
   }
   
   /**
    * For subclasses to override toString()
    * 
    * @param buffer the buffer
    */
   protected void toString(StringBuffer buffer)
   {
      buffer.append(getName());
      buffer.append(" ").append(getVersionRange());
      if (isOptional())
         buffer.append(" OPTIONAL");
      if (isReExport())
         buffer.append(" RE-EXPORT");
   }
}
