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

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.managed.api.annotation.ManagementProperty;

/**
 * FilterMetaData.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
@XmlType(name="filter", propOrder= {"value"})
public class FilterMetaData implements Serializable, Cloneable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   private String filterClassName = PackageClassFilter.class.getName();
   private Object value;

   /**
    * Create filter.
    *
    * @return the filter
    */
   public ClassFilter createFilter()
   {
      // perhaps it's JavaBean
      if (value instanceof ClassFilter)
      {
         return (ClassFilter) value;
      }
      else
      {
         try
         {
            Class<?> clazz = getClass().getClassLoader().loadClass(filterClassName);
            Constructor<?> ctor = (value != null) ? clazz.getDeclaredConstructor(value.getClass()) : clazz.getDeclaredConstructor();
            return (ClassFilter) ctor.newInstance(value);
         }
         catch (Throwable t)
         {
            throw new RuntimeException("Cannot instantiate filter: " + filterClassName + " / " + value, t);
         }
      }
   }

   public String getFilterClassName()
   {
      return filterClassName;
   }

   @XmlAttribute(name = "filter-classname")
   public void setFilterClassName(String filterClassName)
   {
      this.filterClassName = filterClassName;
   }

   public Object getValue()
   {
      return value;
   }

   @XmlAnyElement
   @ManagementProperty(ignored = true)
   public void setValueObject(Object value)
   {
      if (value instanceof String)
         setValueString((String) value);
      else
         this.value = value;
   }

   @XmlValue
   @ManagementProperty(ignored = true)
   public void setValueString(String value)
   {
      if (value != null)
         this.value = value.split(",");
   }

   public int hashCode()
   {
      return 3 * filterClassName.hashCode() + 7 * (value != null ? value.hashCode() : 0);
   }

   public boolean equals(Object obj)
   {
      if (obj instanceof FilterMetaData == false)
         return false;

      FilterMetaData other = (FilterMetaData) obj;
      return filterClassName.equals(other.filterClassName) && ClassLoadingMetaData.equals(value, other.value);
   }

   @Override
   public FilterMetaData clone()
   {
      try
      {
         return (FilterMetaData) super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         throw new RuntimeException("Unexpected", e);
      }
   }
}
