/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.classloading.spi.dependency;

import org.jboss.classloading.spi.metadata.Requirement;

/**
 * ResolutionContext.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ResolutionContext
{
   /** The domain */
   private Domain domain;
   
   /** The module */
   private Module module;
   
   /** The requirement */
   private Requirement requirement;

   /**
    * Create a new ResolutionContext.
    * 
    * @param domain the domain
    * @param module the module
    * @param requirement the requirement
    */
   public ResolutionContext(Domain domain, Module module, Requirement requirement)
   {
      this.domain = domain;
      this.module = module;
      this.requirement = requirement;
   }

   /**
    * Get the domain.
    * 
    * @return the domain.
    */
   public Domain getDomain()
   {
      return domain;
   }

   /**
    * Get the module.
    * 
    * @return the module.
    */
   public Module getModule()
   {
      return module;
   }

   /**
    * Get the requirement.
    * 
    * @return the requirement.
    */
   public Requirement getRequirement()
   {
      return requirement;
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append(getClass().getSimpleName());
      builder.append("{domain=").append(domain);
      builder.append(" module=").append(module);
      builder.append(" requirement=").append(requirement);
      builder.append('}');
      return builder.toString();
   }
}
