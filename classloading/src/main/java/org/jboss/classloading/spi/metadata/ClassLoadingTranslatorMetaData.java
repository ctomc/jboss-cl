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
package org.jboss.classloading.spi.metadata;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;

import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;

/**
 * ClassLoadingTranslatorMetaData.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
@ManagementObject(properties=ManagementProperties.EXPLICIT, name="org.jboss.classloading.spi.metadata.ClassLoadingTranslatorMetaData")
public class ClassLoadingTranslatorMetaData implements Serializable, Cloneable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   private String className;
   private String method;
   private TranslatorScope scope;

   public String getClassName()
   {
      return className;
   }

   @XmlAttribute(name = "class", required = true)
   public void setClassName(String className)
   {
      this.className = className;
   }

   public String getMethod()
   {
      return method;
   }

   @XmlAttribute
   public void setMethod(String method)
   {
      this.method = method;
   }

   public TranslatorScope getScope()
   {
      return scope;
   }

   @XmlAttribute
   public void setScope(TranslatorScope scope)
   {
      this.scope = scope;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("@");
      builder.append(Integer.toHexString(System.identityHashCode(this)));
      builder.append("{");
      toString(builder);
      builder.append("}");
      return builder.toString();
   }
   
   /**
    * For subclasses to override the toString contents
    * 
    * @param builder the builder
    */
   protected void toString(StringBuilder builder)
   {
      builder.append("class=").append(getClassName());
      if (getMethod() != null)
         builder.append(" method=").append(getMethod());
      builder.append(" scope=").append(getScope());
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof ClassLoadingTranslatorMetaData == false)
         return false;
      ClassLoadingTranslatorMetaData other = (ClassLoadingTranslatorMetaData) obj;
      if (equals(getClassName(), other.getClassName()) == false)
         return false;
      if (equals(getMethod(), other.getMethod()) == false)
         return false;
      if (equals(getScope(), other.getScope()) == false)
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
   protected ClassLoadingTranslatorMetaData clone() throws CloneNotSupportedException
   {
      return (ClassLoadingTranslatorMetaData) super.clone();
   }

   static boolean equals(Object one, Object two)
   {
      if (one == null)
         return two == null;
      return one.equals(two);
   }
}
