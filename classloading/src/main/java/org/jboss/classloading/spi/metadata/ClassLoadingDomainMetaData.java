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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.helpers.NameAndVersionSupport;
import org.jboss.classloading.spi.version.Version;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;

/**
 * ClassLoadingDomainMetaData.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
@ManagementObject(properties=ManagementProperties.EXPLICIT, name="org.jboss.classloading.spi.metadata.ClassLoadingDomainMetaData")
public class ClassLoadingDomainMetaData extends NameAndVersionSupport
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The parent domain */
   private String parentDomain;

   /** The parent */
   private LoaderMetaData parent;

   /** The parent policy */
   private ParentPolicyMetaData parentPolicy;

   /** The shutdown policy */
   private ShutdownPolicy shutdownPolicy;

   /** The use load class for parent */
   private Boolean useLoadClassForParent;

   // ignore the version property
   public void setTheVersion(Version version)
   {
      // ignored
   }

   /**
    * Get the parentDomain.
    * 
    * @return the parentDomain.
    */
   public String getParentDomain()
   {
      return parentDomain;
   }

   /**
    * Set the parentDomain.
    * 
    * @param parentDomain the parentDomain.
    */
   @ManagementProperty
   @XmlAttribute
   public void setParentDomain(String parentDomain)
   {
      this.parentDomain = parentDomain;
   }

   /**
    * Get the parent loader.
    *
    * @return the parent loader
    */
   public LoaderMetaData getParent()
   {
      return parent;
   }

   /**
    * Set the parent loader.
    *
    * @param parent the parent loader
    */
   @XmlElement
   @ManagementProperty(ignored = true)
   public void setParent(LoaderMetaData parent)
   {
      this.parent = parent;
   }

   /**
    * Get parent policy.
    *
    * @return the parent policy
    */
   public ParentPolicyMetaData getParentPolicy()
   {
      return parentPolicy;
   }

   /**
    * Set parent policy.
    *
    * @param parentPolicy the parent policy
    */
   @ManagementProperty
   public void setParentPolicy(ParentPolicyMetaData parentPolicy)
   {
      this.parentPolicy = parentPolicy;
   }

   /**
    * Get the shutdown policy
    *
    * @return the shutdown policy.
    */
   public ShutdownPolicy getShutdownPolicy()
   {
      return shutdownPolicy;
   }

   /**
    * Set the shutdown policy.
    *
    * @param shutdownPolicy the sjutdown policy
    */
   @ManagementProperty(name="shutdown")
   @XmlAttribute(name="shutdown")
   public void setShutdownPolicy(ShutdownPolicy shutdownPolicy)
   {
      this.shutdownPolicy = shutdownPolicy;
   }

   public Boolean getUseLoadClassForParent()
   {
      return useLoadClassForParent;
   }

   @XmlAttribute
   @ManagementProperty
   public void setUseLoadClassForParent(Boolean useLoadClassForParent)
   {
      this.useLoadClassForParent = useLoadClassForParent;
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
      builder.append("name=").append(getName());
      String parentDomain = getParentDomain();
      if (parentDomain != null)
         builder.append(" parentDomain=").append(parentDomain);
      if (parent != null)
         builder.append(" parent=").append(parent);
      if (parentPolicy != null)
         builder.append(" parent-policy=").append(parentPolicy);
      if (shutdownPolicy != null)
         builder.append(" ").append(shutdownPolicy);
      builder.append(" use-load-class-for-parent=").append(useLoadClassForParent);
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof ClassLoadingDomainMetaData == false)
         return false;
      if (super.equals(obj) == false)
         return false;
      ClassLoadingDomainMetaData other = (ClassLoadingDomainMetaData) obj;
      if (equals(this.getParentDomain(), other.getParentDomain()) == false)
         return false;
      if (equals(this.getParent(), other.getParent()) == false)
         return false;
      if (equals(this.getParentPolicy(), other.getParentPolicy()) == false)
         return false;
      if (equals(this.getShutdownPolicy(), other.getShutdownPolicy()) == false)
         return false;
      if (equals(this.getUseLoadClassForParent(), other.getUseLoadClassForParent()) == false)
         return false;
      return true;
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
   public ClassLoadingDomainMetaData clone()
   {
      ClassLoadingDomainMetaData clone = (ClassLoadingDomainMetaData) super.clone();
      if (parentPolicy != null)
         clone.parentPolicy = parentPolicy.clone();
      return clone;
   }
}
