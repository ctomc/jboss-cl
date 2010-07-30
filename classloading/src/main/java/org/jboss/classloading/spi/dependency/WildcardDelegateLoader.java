/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderPolicyFactory;
import org.jboss.classloader.spi.DelegateLoader;
import org.jboss.classloader.spi.ImportType;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.FilteredDelegateLoader;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.DependencyInfo;

/**
 * Wildcard delegate loader.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class WildcardDelegateLoader extends FilteredDelegateLoader
{
   private Controller controller;
   private Module module;
   private VersionRange range;

   public WildcardDelegateLoader(Controller controller, ClassLoaderPolicyFactory factory, ClassFilter filter, RequirementDependencyItem item)
   {
      super(factory, filter);
      if (controller == null)
         throw new IllegalArgumentException("Null controller");
      if (item == null)
         throw new IllegalArgumentException("Null item");
      Requirement requirement = item.getRequirement();
      if (requirement instanceof PackageRequirement == false)
         throw new IllegalArgumentException("Illegal package requirement: " + requirement);

      this.controller = controller;
      this.module = item.getModule();
      this.range = ((PackageRequirement)requirement).getVersionRange();
   }

   /**
    * Try finding matching delegate loader for the pckg parameter.
    *
    * @param pckg the current package we're trying to lookup.
    * @return the matching delegate loader or null if no such match
    */
   protected DelegateLoader resolve(String pckg)
   {
      Requirement requirement = new PackageRequirement(pckg, range);
      ControllerState state = module.getClassLoaderState();
      // resolve both sides of the wire at the same state
      WildcardRequirementDependencyItem item = new WildcardRequirementDependencyItem(module, requirement, state, state);
      if (item.resolve(controller))
      {
         Module resolvedModule = item.getResolvedModule();

         DelegateLoader loader = resolvedModule.getDelegateLoader(module, requirement);
         loader.setImportType(ImportType.AFTER); // allow normal imports to run before
         item.setLoader(loader); // remember the loader wrt item

         ClassLoaderPolicy policy = getPolicy();
         policy.addExtraDelegate(loader);

         module.addIDependOn(item);

         return loader;
      }
      return null;
   }

   @Override
   public String[] getPackageNames()
   {
      return null; // too dynamic to expose any explicit package
   }

   @Override
   protected Class<?> doLoadClass(String className)
   {
      DelegateLoader loader = resolve(ClassLoaderUtils.getClassPackageName(className));
      return loader != null ? loader.loadClass(className) : null;
   }

   @Override
   protected URL doGetResource(String name)
   {
      DelegateLoader loader = resolve(ClassLoaderUtils.getResourcePackageName(name));
      return loader != null ? loader.getResource(name) : null;
   }

   @Override
   protected void doGetResources(String name, Set<URL> urls) throws IOException
   {
      DelegateLoader loader = resolve(ClassLoaderUtils.getResourcePackageName(name));
      if (loader != null)
      {
         loader.getResources(name, urls);
      }
   }

   @Override
   protected Package doGetPackage(String name)
   {
      DelegateLoader loader = resolve(ClassLoaderUtils.pathToPackage(name));
      return loader != null ? loader.getPackage(name) : null;
   }

   private class WildcardRequirementDependencyItem extends RequirementDependencyItem
   {
      private DelegateLoader loader;

      private WildcardRequirementDependencyItem(Module module, Requirement requirement, ControllerState whenRequired, ControllerState dependentState)
      {
         super(module, requirement, whenRequired, dependentState);
      }

      @Override
      public boolean unresolved(Controller controller)
      {
         if (loader != null)
         {
            ClassLoaderPolicy policy = getPolicy();
            policy.removeExtraDelegate(loader);
         }

         Object iDependOn = getIDependOn();
         if (iDependOn != null)
         {
            ControllerContext context = controller.getContext(iDependOn, null);
            if (context != null)
            {
               DependencyInfo info = context.getDependencyInfo();
               info.removeDependsOnMe(this);
            }
         }

         module.removeIDependOn(this);

         super.unresolved(controller);

         return false; // return false, so we don't get unwinded
      }

      void setLoader(DelegateLoader loader)
      {
         this.loader = loader;
      }
   }

   @Override
   public ImportType getImportType()
   {
      return ImportType.AFTER; // since we add new one's after, we should be after as well by default
   }

   @Override
   public void setImportType(ImportType importType)
   {
      // ignore
   }
}
