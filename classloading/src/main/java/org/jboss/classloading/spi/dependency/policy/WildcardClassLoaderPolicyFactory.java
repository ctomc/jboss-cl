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
package org.jboss.classloading.spi.dependency.policy;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderPolicyFactory;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Domain;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.RequirementDependencyItem;
import org.jboss.classloading.spi.metadata.Requirement;

/**
 * WildcardClassLoaderPolicyFactory.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class WildcardClassLoaderPolicyFactory implements ClassLoaderPolicyFactory
{
   /** The domain */
   private Domain domain;

   /** The package requirement */
   private PackageRequirement requirement;

   /** The module */
   private Module module;

   /**
    * Create a new WildcardClassLoaderPolicyFactory.
    *
    * @param domain the domain
    * @param item the requirement item
    */
   public WildcardClassLoaderPolicyFactory(Domain domain, RequirementDependencyItem item)
   {
      if (domain == null)
         throw new IllegalArgumentException("Null domain");
      if (item == null)
         throw new IllegalArgumentException("Null item");
      Requirement requirement = item.getRequirement();
      if (requirement == null || requirement instanceof PackageRequirement == false)
         throw new IllegalArgumentException("Illegal requirement: " + requirement);
      PackageRequirement pr = (PackageRequirement) requirement;
      if (pr.isWildcard() == false)
         throw new IllegalArgumentException("Requirement is not wildcard: " + pr);
      if (item.getModule() == null)
         throw new IllegalArgumentException("Null module");
      this.domain = domain;
      this.requirement = pr;
      this.module = item.getModule();
   }

   public ClassLoaderPolicy createClassLoaderPolicy()
   {
      WildcardClassLoaderPolicy policy = new WildcardClassLoaderPolicy(domain, requirement, module);
      ClassLoading classLoading = domain.getClassLoading();
      classLoading.addModuleRegistry(policy); // so we know when to reset on module change
      return policy;
   }
}