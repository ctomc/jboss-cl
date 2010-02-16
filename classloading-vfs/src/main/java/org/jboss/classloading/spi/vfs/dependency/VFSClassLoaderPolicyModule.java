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
package org.jboss.classloading.spi.vfs.dependency;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.plugins.vfs.PackageVisitor;
import org.jboss.classloading.plugins.vfs.VFSResourceVisitor;
import org.jboss.classloading.spi.dependency.policy.ClassLoaderPolicyModule;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.metadata.ExportAll;
import org.jboss.classloading.spi.vfs.metadata.VFSClassLoaderFactory;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.kernel.spi.dependency.KernelControllerContextAware;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.automount.Automounter;

/**
 * VFSClassLoaderPolicyModule.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VFSClassLoaderPolicyModule extends ClassLoaderPolicyModule implements KernelControllerContextAware
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The roots */
   private List<String> roots;
   
   /** Our cached vfs roots */
   private VirtualFile[] vfsRoots;

   private static final VirtualFile[] NO_VIRTUAL_FILES = new VirtualFile[0];

   /**
    * Create a new VFSClassLoaderPolicyModule.
    * 
    * @param classLoadingMetaData the classloading metadata
    * @param contextName the context name
    */
   public VFSClassLoaderPolicyModule(VFSClassLoaderFactory classLoadingMetaData, String contextName)
   {
      super(classLoadingMetaData, contextName);
   }
   
   /**
    * Get the roots.
    * 
    * @return the roots.
    */
   public List<String> getRoots()
   {
      return roots;
   }

   /**
    * Set the roots.
    * 
    * @param roots the roots.
    */
   public void setRoots(List<String> roots)
   {
      this.roots = roots;
   }

   @Override
   protected List<Capability> determineCapabilities()
   {
      // While we are here, check the roots
      VirtualFile[] roots = determineVFSRoots();

      List<Capability> capabilities = super.determineCapabilities();
      if (capabilities != null)
         return capabilities;
         
      // We need to work it out
      ClassLoadingMetaDataFactory factory = ClassLoadingMetaDataFactory.getInstance();
      capabilities = new CopyOnWriteArrayList<Capability>();

      // We have a module capability
      Object version = getVersion();
      Capability capability = factory.createModule(getName(), version);
      capabilities.add(capability);
      
      // Do we determine package capabilities
      ClassFilter included = getIncluded();
      ClassFilter excluded = getExcluded();
      ClassFilter excludedExport = getExcludedExport();
      ExportAll exportAll = getExportAll();
      if (exportAll != null)
      {
         Set<String> exportedPackages = PackageVisitor.determineAllPackages(roots, null, exportAll, included, excluded, excludedExport);
         for (String packageName : exportedPackages)
         {
            capability = factory.createPackage(packageName, version);
            capabilities.add(capability);
         }
      }
      
      return capabilities;
   }

   public void setKernelControllerContext(KernelControllerContext context) throws Exception
   {
      setControllerContext(context);
   }

   public void unsetKernelControllerContext(KernelControllerContext context) throws Exception
   {
      setControllerContext(null);
   }

   @Override
   protected VFSClassLoaderFactory getClassLoadingMetaData()
   {
      return (VFSClassLoaderFactory) super.getClassLoadingMetaData();
   }
   
   /**
    * Get the virtual file roots
    * 
    * @return the roots
    */
   protected VirtualFile[] determineVFSRoots()
   {
      if (vfsRoots != null)
         return vfsRoots;
      
      if (roots == null)
      {
         vfsRoots = NO_VIRTUAL_FILES;
      }
      else
      {
         VirtualFile[] vfsRoots = new VirtualFile[roots.size()];
         for (int i = 0; i < roots.size(); ++i)
         {
            String root = roots.get(i);
            try
            {
               URI uri = new URI(root);
               vfsRoots[i] = VFS.getChild(uri);
            }
            catch (URISyntaxException e)
            {
               throw new RuntimeException("Error creating VFS file for " + root, e);
            }
         }
         this.vfsRoots = vfsRoots;
      }
      return vfsRoots;
   }

   @Override
   public VFSClassLoaderPolicy getPolicy()
   {
      return (VFSClassLoaderPolicy) super.getPolicy();
   }
   
   @Override
   protected VFSClassLoaderPolicy determinePolicy()
   {
      VirtualFile[] roots = determineVFSRoots();
      VFSClassLoaderPolicy policy = VFSClassLoaderPolicy.createVFSClassLoaderPolicy(getContextName(), roots);
      
      String[] packageNames = getPackageNames();
      policy.setExportedPackages(packageNames);
      policy.setIncluded(getIncluded());
      policy.setExcluded(getExcluded());
      policy.setExcludedExport(getExcludedExport());
      policy.setExportAll(getExportAll());
      policy.setImportAll(isImportAll());
      policy.setShutdownPolicy(getShutdownPolicy());
      policy.setCacheable(isCacheable());
      policy.setBlackListable(isBlackListable());
      policy.setDelegates(getDelegates());
      return policy;
   }

   @Override
   public void reset()
   {
      super.reset();
      vfsRoots = null;
   }

   /**
    * Mounts all VFS roots when the module is created. 
    */
   public void create()
   {
      final VirtualFile[] vfsRoots = determineVFSRoots();
      for (VirtualFile root : vfsRoots) 
      {
         try 
         {
            Automounter.mount(this, root);
         }
         catch (IOException e) 
         {
            throw new RuntimeException("Failed to mount root " + root, e);
         }
      }
   }

   /**
    * Cleanup all mounts. 
    */
   public void destroy() 
   {
      Automounter.cleanup(this);
   }

   @Override
   public void visit(ResourceVisitor visitor, ResourceFilter filter, ResourceFilter recurseFilter, URL... urls)
   {
      ClassLoader classLoader = getClassLoader();
      if (classLoader == null)
         throw new IllegalStateException("ClassLoader has not been constructed for " + getContextName());

      VirtualFile[] roots = determineVFSRoots();
      if (roots != null && roots.length > 0)
      {
         ClassFilter included = getIncluded();
         ClassFilter excluded = getExcluded();
         VFSResourceVisitor.visit(roots, null, included, excluded, classLoader, visitor, filter, recurseFilter, urls);
      }
   } 
}
