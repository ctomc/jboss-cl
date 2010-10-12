/*
* JBoss, Home of Professional Open Source
* Copyright 2008, JBoss Inc., and individual contributors as indicated
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
package org.jboss.classloading.spi.metadata;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.ClassFilterUtils;

/**
 * ParentPolicyMetaData.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
@XmlType(name="parentPolicy", propOrder= {"beforeFilter", "afterFilter", "description"})
@XmlRootElement(name="parent-policy", namespace="urn:jboss:classloading:1.0")
public class ParentPolicyMetaData implements Serializable, Cloneable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   private String name;
   private FilterMetaData beforeFilter;
   private FilterMetaData afterFilter;
   private String description;

   private transient ParentPolicy parentPolicy;

   /**
    * Create parent policy.
    *
    * @return the parent policy
    */
   public ParentPolicy createParentPolicy()
   {
      if (parentPolicy == null)
      {
         if (name != null)
         {
            String upper = name.toUpperCase();
            try
            {
               Field instance = ParentPolicy.class.getField(upper);
               parentPolicy = (ParentPolicy) instance.get(null);
            }
            catch (Throwable t)
            {
               throw new RuntimeException("Cannot create parent-policy, wrong name perhaps? - " + name, t);
            }
         }
         else
         {
            ClassFilter before = (beforeFilter != null) ? beforeFilter.createFilter() : ClassFilterUtils.EVERYTHING;
            ClassFilter after = (afterFilter != null) ? afterFilter.createFilter() : ClassFilterUtils.NOTHING;
            parentPolicy = new ParentPolicy(before, after, description);
         }
      }
      return parentPolicy;
   }

   public String getName()
   {
      return name;
   }

   @XmlAttribute
   public void setName(String name)
   {
      this.name = name;
   }

   public FilterMetaData getBeforeFilter()
   {
      return beforeFilter;
   }

   @XmlElement(name = "before-filter")
   public void setBeforeFilter(FilterMetaData beforeFilter)
   {
      this.beforeFilter = beforeFilter;
   }

   public FilterMetaData getAfterFilter()
   {
      return afterFilter;
   }

   @XmlElement(name = "after-filter")
   public void setAfterFilter(FilterMetaData afterFilter)
   {
      this.afterFilter = afterFilter;
   }

   public String getDescription()
   {
      return description;
   }

   public void setDescription(String description)
   {
      this.description = description;
   }

   public int hashCode()
   {
      return hash(name) + 3 * hash(beforeFilter) + 7 * hash(afterFilter) + 11 * hash(description); 
   }

   private static int hash(Object obj)
   {
      return (obj != null) ? obj.hashCode() : 0;
   }

   public boolean equals(Object obj)
   {
      if (obj instanceof ParentPolicyMetaData == false)
         return false;

      ParentPolicyMetaData other = (ParentPolicyMetaData) obj;
      if (ClassLoadingMetaData.equals(name, other.name) == false)
         return false;
      if (ClassLoadingMetaData.equals(beforeFilter, other.beforeFilter) == false)
         return false;
      if (ClassLoadingMetaData.equals(afterFilter, other.afterFilter) == false)
         return false;
      if (ClassLoadingMetaData.equals(description, other.description) == false)
         return false;
      return true;
   }

   @Override
   public ParentPolicyMetaData clone()
   {
      try
      {
         ParentPolicyMetaData clone = (ParentPolicyMetaData) super.clone();
         clone.beforeFilter = beforeFilter.clone();
         clone.afterFilter = afterFilter.clone();
         return clone;
      }
      catch (CloneNotSupportedException e)
      {
         throw new RuntimeException("Unexpected", e);
      }
   }
}
