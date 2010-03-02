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
package org.jboss.classloading.spi.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.logging.Logger;
import org.jboss.util.collection.ConcurrentSet;

/**
 * ClassLoading.
 *
 * @author <a href="adrian@jboss.org">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class ClassLoading implements Resolver, ClassLoadingAdmin
{
   /** The log */
   private static final Logger log = Logger.getLogger(ClassLoading.class);

   /** An empty default domain */
   private Domain EMPTY_DOMAIN = new Domain(this, ClassLoaderSystem.DEFAULT_DOMAIN_NAME, null, true);
   
   /** The classloading domains by name */
   private final Map<String, Domain> domains = new ConcurrentHashMap<String, Domain>();

   /** The global capabilities provider */
   private final Set<GlobalCapabilitiesProvider> globalCapabilitiesProviders = new ConcurrentSet<GlobalCapabilitiesProvider>();

   /** The module registries */
   private final Set<ModuleRegistry> moduleRegistries = new ConcurrentSet<ModuleRegistry>();

   /** The resolvers */
   private List<Resolver> resolvers = null;

   /**
    * Add a module
    *
    * @param module the module
    * @throws IllegalArgumentException for a null module
    */
   public void addModule(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");
      
      String domainName = module.getDeterminedDomainName();
      boolean parentFirst = module.isJ2seClassLoadingCompliance();
      String parentDomainName = module.getDeterminedParentDomainName();
      Domain domain = getDomain(domainName, parentDomainName, parentFirst);
      domain.addModule(module);

      Set<ModuleRegistry> added = new HashSet<ModuleRegistry>();
      try
      {
         for (ModuleRegistry mr : moduleRegistries)
         {
            mr.addModule(module);
            added.add(mr);
         }
      }
      catch (Exception e)
      {
         for (ModuleRegistry mr : added)
         {
            try
            {
               mr.removeModule(module);
            }
            catch (Exception ignored)
            {
            }
         }
         module.release();

         throw new IllegalArgumentException("Exception while registering Module.", e);
      }
   }
   
   /**
    * Remove a module
    * 
    * @param module the module
    * @throws IllegalArgumentException for a null module
    */
   public void removeModule(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null module");

      for (ModuleRegistry mr : moduleRegistries)
      {
         try
         {
            mr.removeModule(module);
         }
         catch (Exception e)
         {
            log.warn("Exception unregistering module, registry: " + mr + ", cause: " + e);
         }
      }

      module.release();
   }
   
   /**
    * Add a global capabilities provider
    * @param provider the provider
    * @throws IllegalArgumentException for a null provider
    */
   public void addGlobalCapabilitiesProvider(GlobalCapabilitiesProvider provider)
   {
      if (provider == null)
         throw new IllegalArgumentException("Null global capabilities provider");
      
      globalCapabilitiesProviders.add(provider);
   }
   
   /**
    * Remove a global capabilities provider
    * @param provider the provider
    * @throws IllegalArgumentException for a null provider
    */
   public void removeGlobalCapabilitiesProvider(GlobalCapabilitiesProvider provider)
   {
      if (provider == null)
         throw new IllegalArgumentException("Null global capabilities provider");
      
      globalCapabilitiesProviders.remove(provider);
   }

   /**
    * Add a resolver
    * 
    * @param resolver the resolver
    */
   public void addResolver(Resolver resolver)
   {
      if (resolver == null)
         throw new IllegalArgumentException("Null resolver");
      
      if (resolvers == null)
         resolvers = new CopyOnWriteArrayList<Resolver>();
      
      resolvers.add(resolver);
   }

   /**
    * Remove a resolver
    * 
    * @param resolver the resolver
    */
   public void removeResolver(Resolver resolver)
   {
      if (resolver == null)
         throw new IllegalArgumentException("Null resolver");
      
      if (resolvers == null)
         return;
      
      resolvers.remove(resolver);
   }
   
   public boolean resolve(ResolutionContext context)
   {
      if (resolvers != null && resolvers.isEmpty() == false)
      {
         for (Resolver resolver : resolvers)
         {
            try
            {
               if (resolver.resolve(context))
                  return true;
            }
            catch (Throwable t)
            {
               log.warn("Error in resolver: " + resolver + " context=" + context, t);
            }
         }
      }
      return false;
   }
   
   /**
    * Get or create the domain
    * 
    * @param domainName the domain name
    * @param parentDomainName the parent domain name
    * @param parentFirst whether to look in the parent first
    * @return the domain
    * @throws IllegalArgumentException for a null domain
    */
   protected Domain getDomain(String domainName, String parentDomainName, boolean parentFirst)
   {
      Domain domain;
      // FINDBUGS: This synchronization is correct - more than addIfNotPresent behaviour
      synchronized (domains)
      {
         domain = domains.get(domainName);
         if (domain == null)
         {
            domain = createDomain(domainName, parentDomainName, parentFirst);
            domains.put(domainName, domain);
         }
      }
      return domain;
   }

   /**
    * Get a domain
    * 
    * @param domainName the domain name
    * @return the domain or null if it doesn't exist
    */
   protected Domain getDomain(String domainName)
   {
      if (domainName == null)
         throw new IllegalArgumentException("Null domain name");

      Domain domain = domains.get(domainName);
      // This is hack, but it is a situation that probably only occurs in the tests
      // i.e. there are no classloaders in the default domain so it doesn't exist
      if (domain == null && ClassLoaderSystem.DEFAULT_DOMAIN_NAME.equals(domainName))
         domain = EMPTY_DOMAIN;
      return domain;
   }
   
   /**
    * Create a domain
    * 
    * @param domainName the domain name
    * @param parentDomainName the parent domain name
    * @param parentFirst whether to look in the parent first
    * @return the domain
    * @throws IllegalArgumentException for a null domain name
    */
   protected Domain createDomain(String domainName, String parentDomainName, boolean parentFirst)
   {
      if (domainName == null)
         throw new IllegalArgumentException("Null domain name");
      return new Domain(this, domainName, parentDomainName, parentFirst);
   }
   
   public Module getModuleForClass(Class<?> clazz)
   {
      return Module.getModuleForClass(clazz);
   }
   
   public Collection<Module> getModules(String name, VersionRange range)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      
      Collection<Module> result = new HashSet<Module>();
      for (Domain domain : domains.values())
         domain.getModulesInternal(name, range, result);
      return result;
   }
   
   public Collection<ImportModule> getImportedModules(String name, VersionRange range)
   {
      Collection<ImportModule> result = new HashSet<ImportModule>();
      for (Domain domain : domains.values())
         domain.getImportingModulesInternal(name, range, result);
      return result;
   }
   
   public Collection<ExportPackage> getExportedPackages(Module module)
   {
      if (module == null)
         throw new IllegalArgumentException("Null modules");
      return module.getExportedPackages();
   }

   public Collection<ExportPackage> getExportedPackages(String name, VersionRange range)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      
      Collection<ExportPackage> result = new HashSet<ExportPackage>();
      for (Domain domain : domains.values())
         domain.getExportedPackagesInternal(name, range, result);
      return result;
   }

   public void refreshModules(Module... modules) throws Exception
   {
      Module.refreshModules(modules);
   }

   public boolean resolveModules(Module... modules) throws Exception
   {
      return Module.resolveModules(modules);
   }

   /**
    * Find the module for a classloader
    * 
    * @param loader the classloader
    * @return the module or null if the classloader does not correspond to a registered module classloader
    * @throws SecurityException if the caller doesn't have <code>new RuntimePermision("getClassLoader")</code>
    */
   public static Module getModuleForClassLoader(ClassLoader loader)
   {
      return Module.getModuleForClassLoader(loader);
   }
   
   /**
    * Find the classloader for a module
    * 
    * @param module the module
    * @return the classloader or null if the module does not correspond to a registered classloader module
    * @throws SecurityException if the caller doesn't have <code>new RuntimePermision("getClassLoader")</code>
    */
   public static ClassLoader getClassLoaderForModule(Module module)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkPermission(new RuntimePermission("getClassLoader"));
      
      if (module == null)
         return null;
      return module.getClassLoader();
   }
   
   /**
    * Merges the capabilities provided by our global capabilities provider with the passed in capabilities.
    *
    * @param capabilities the capabilities list into which we want to add the global capabilities
    * @return the passed in capabilities with the global capabilities merged in
    */
   List<Capability> mergeGlobalCapabilities(List<Capability> capabilities)
   {
      if (capabilities == null)
         throw new IllegalArgumentException("Null capabilities");
      
      if (globalCapabilitiesProviders != null && globalCapabilitiesProviders.size() > 0)
      {
         for (GlobalCapabilitiesProvider provider : globalCapabilitiesProviders)
         {
            capabilities.addAll(provider.getCapabilities());
         }
      }
      return capabilities;
   }

   /**
    * Add module registry.
    *
    * @param moduleRegistry the module registry
    * @return see Set#add
    */
   public boolean addModuleRegistry(ModuleRegistry moduleRegistry)
   {
      return moduleRegistries.add(moduleRegistry);
   }

   /**
    * Remove module registry.
    *
    * @param moduleRegistry the module registry
    * @return see Set#remove
    */
   public boolean removeModuleRegistry(ModuleRegistry moduleRegistry)
   {
      return moduleRegistries.remove(moduleRegistry);
   }
}
