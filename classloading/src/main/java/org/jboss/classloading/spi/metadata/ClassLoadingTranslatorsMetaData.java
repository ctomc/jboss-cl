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
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;

/**
 * ClassLoadingTranslatorsMetaData.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
@ManagementObject(properties=ManagementProperties.EXPLICIT, name="org.jboss.classloading.spi.metadata.ClassLoadingTranslatorsMetaData")
public class ClassLoadingTranslatorsMetaData implements Serializable, Cloneable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   private List<ClassLoadingTranslatorMetaData> translators;
   private TranslatorScope scope = TranslatorScope.POLICY;

   protected void addTranslator(ClassLoadingTranslatorMetaData translator)
   {
      if (translators == null)
         translators = new ArrayList<ClassLoadingTranslatorMetaData>();

      if (translator.getScope() == null)
         translator.setScope(getScope());

      translators.add(translator);
   }

   public List<ClassLoadingTranslatorMetaData> getTranslators()
   {
      return translators;
   }

   @XmlElement(name = "translator")
   public void setTranslators(List<ClassLoadingTranslatorMetaData> translators)
   {
      if (translators != null)
      {
         for (ClassLoadingTranslatorMetaData translator : translators)
            addTranslator(translator);
      }
      else
      {
         this.translators = null;
      }
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
      builder.append("translators=").append(getTranslators());
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof ClassLoadingTranslatorsMetaData == false)
         return false;
      ClassLoadingTranslatorsMetaData other = (ClassLoadingTranslatorsMetaData) obj;
      return equals(getTranslators(), other.getTranslators()) && equals(getScope(), other.getScope());
   }
   
   // FINDBUGS: Just to keep it happy
   @Override
   public int hashCode()
   {
      return super.hashCode();
   }
   
   static boolean equals(Object one, Object two)
   {
      if (one == null)
         return two == null;
      return one.equals(two);
   }

   @Override
   public Object clone() throws CloneNotSupportedException
   {
      ClassLoadingTranslatorsMetaData clone = (ClassLoadingTranslatorsMetaData) super.clone();
      if (getTranslators() != null)
      {
         List<ClassLoadingTranslatorMetaData> cloneTranslators = new ArrayList<ClassLoadingTranslatorMetaData>(getTranslators().size());
         for (ClassLoadingTranslatorMetaData cltmd : getTranslators())
            cloneTranslators.add(cltmd.clone());
         clone.setTranslators(cloneTranslators);
      }
      return clone;
   }
}
