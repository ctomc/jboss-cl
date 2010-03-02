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
package org.jboss.classloading.spi.dependency.policy.mock;

import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.test.support.MockClassLoaderHelper;
import org.jboss.classloader.test.support.MockClassLoaderPolicy;
import org.jboss.classloading.plugins.visitor.DefaultResourceContext;
import org.jboss.classloading.spi.dependency.Domain;
import org.jboss.classloading.spi.dependency.policy.ClassLoaderPolicyModule;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaDataFactory;
import org.jboss.classloading.spi.visitor.ResourceContext;
import org.jboss.classloading.spi.visitor.ResourceFilter;
import org.jboss.classloading.spi.visitor.ResourceVisitor;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.kernel.spi.dependency.KernelControllerContextAware;

/**
 * VFSClassLoaderPolicyModule.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockClassLoaderPolicyModule extends ClassLoaderPolicyModule implements KernelControllerContextAware
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   /**
    * Create a new VFSClassLoaderPolicyModule.
    * 
    * @param classLoadingMetaData the classloading metadata
    * @param contextName the context name
    */
   public MockClassLoaderPolicyModule(MockClassLoadingMetaData classLoadingMetaData, String contextName)
   {
      super(classLoadingMetaData, contextName);
   }

   @Override
   public Domain checkDomain()
   {
      return super.checkDomain();
   }
   
   /**
    * Get collection from string array.
    *
    * @param strings the strings
    * @return string collection
    */
   private static Collection<String> toCollection(String[] strings)
   {
      if (strings == null || strings.length == 0)
         return Collections.emptySet();
      else
         return Arrays.asList(strings);
   }

   /**
    * Get URL for path param.
    *
    * @param path the path
    * @return path's URL
    */
   protected URL getURL(String path)
   {
      ClassLoader classLoader = getClassLoader();
      if (classLoader == null)
         throw new IllegalStateException("ClassLoader has not been constructed for " + getContextName());

      return classLoader.getResource(path);
   }

   /**
    * Get file from path's url.
    *
    * @param url the path's url
    * @return path's file
    */
   protected File getFile(URL url)
   {
      try
      {
         return new File(url.toURI());
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void visit(ResourceVisitor visitor, ResourceFilter filter, ResourceFilter recurseFilter, URL... urls)
   {
      MockClassLoadingMetaData mclmd = getClassLoadingMetaData();
      String[] paths = mclmd.getPaths();
      if (paths != null && paths.length > 0)
      {
         ClassLoader classLoader = getClassLoader();
         if (classLoader == null)
            throw new IllegalStateException("ClassLoader has not been constructed for " + getContextName());

         Collection<String> included = toCollection(mclmd.getIncludedClasses());
         ClassFilter includedFilter = getIncluded();
         Collection<String> excluded = toCollection(mclmd.getExcludedClasses());
         ClassFilter excludedFilter = getExcluded();

         for (String path : paths)
         {
            visitPath(null, path, visitor, filter, recurseFilter, classLoader, included, includedFilter, excluded, excludedFilter, null);
         }
      }
   }

   /**
    * Visit path.
    *
    * @param file the current path file
    * @param path the path
    * @param visitor the visitor
    * @param filter the filter
    * @param recurseFilter the recurse filter
    * @param classLoader the classloader
    * @param included the included
    * @param includedFilter the included filter
    * @param excluded the excluded
    * @param excludedFilter the excluded filter
    * @param context the current context
    */
   protected void visitPath(File file, String path, ResourceVisitor visitor, ResourceFilter filter, ResourceFilter recurseFilter, ClassLoader classLoader, Collection<String> included, ClassFilter includedFilter, Collection<String> excluded, ClassFilter excludedFilter, ResourceContext context)
   {
      boolean visit = includePath(path, included, includedFilter, excluded, excludedFilter);

      URL url = getURL(path);
      
      if (visit)
      {
         if (context == null)
            context = new DefaultResourceContext(url, path, classLoader);
         if (filter == null || filter.accepts(context))
            visitor.visit(context);
      }

      if (file == null)
         file = getFile(url);
      
      if (file.isFile())
         return;

      File[] files = file.listFiles();
      if (files != null && files.length > 0)
      {
         if (path.endsWith("/") == false)
            path += "/";

         for (File child : files)
         {
            String childPath = path + child.getName();
            ResourceContext childContext = new DefaultResourceContext(getURL(childPath), childPath, classLoader);
            if (recurseFilter == null || recurseFilter.accepts(childContext))
            {
               visitPath(child, childPath, visitor, filter, recurseFilter, classLoader, included, includedFilter, excluded, excludedFilter, childContext);
            }
         }
      }
   }

   /**
    * Should we include path in visit.
    *
    * @param path the path
    * @param included the included
    * @param includedFilter the included filter
    * @param excluded the excluded
    * @param excludedFilter the excluded filter
    * @return true if path should be included in visit
    */
   protected boolean includePath(String path, Collection<String> included, ClassFilter includedFilter, Collection<String> excluded, ClassFilter excludedFilter)
   {
      if (included.isEmpty() == false && included.contains(path) == false)
         return false;
      if (includedFilter != null && includedFilter.matchesResourcePath(path) == false)
         return false;
      if (excluded.isEmpty() == false && excluded.contains(path))
         return false;
      if (excludedFilter != null && excludedFilter.matchesResourcePath(path))
         return false;

      return true;
   }

   @Override
   protected List<Capability> determineCapabilities()
   {
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

      MockClassLoadingMetaData metadata = getClassLoadingMetaData();
      String[] exported = metadata.getExportedPackages();
      // Do we determine package capabilities?
      if (exported != null)
      {
         for (String packageName : exported)
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
   protected MockClassLoadingMetaData getClassLoadingMetaData()
   {
      return (MockClassLoadingMetaData) super.getClassLoadingMetaData();
   }

   @Override
   public MockClassLoaderPolicy getPolicy()
   {
      return (MockClassLoaderPolicy) super.getPolicy();
   }
   
   @Override
   protected MockClassLoaderPolicy determinePolicy()
   {
      MockClassLoadingMetaData metaData = getClassLoadingMetaData();
      MockClassLoaderPolicy policy = MockClassLoaderHelper.createMockClassLoaderPolicy(getContextName());
      policy.setPrefix(metaData.getPrefix());
      policy.setPackageNames(getPackageNames());
      policy.setPaths(metaData.getPaths());
      policy.setIncluded(metaData.getIncludedClasses());
      policy.setExcluded(metaData.getExcludedClasses());
      policy.setImportAll(isImportAll());
      policy.setShutdownPolicy(getShutdownPolicy());
      policy.setDelegates(getDelegates());
      return policy;
   }

   @Override
   public ClassLoader getClassLoader()
   {
      return super.getClassLoader();
   }
}
