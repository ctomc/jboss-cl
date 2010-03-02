package org.jboss.classloading.spi.dependency;

import java.util.Collection;

import org.jboss.classloading.spi.version.VersionRange;

/**
 * ClassLoading admin
 *
 * @author adrian@jboss.org
 */
public interface ClassLoadingAdmin
{
   /**
    * Get the module for a class
    * 
    * @param clazz the class
    * @return the module or null if there is no such module
    */
   Module getModuleForClass(Class<?> clazz);

   /**
    * Get the modules matching the name and version range
    * 
    * @param name the name
    * @param range the version range or null if all are required
    * @return the modules
    */
   Collection<Module> getModules(String name, VersionRange range);
   
   /**
    * Get the exported packages for a module
    * 
    * @param module the module
    * @return the exported packages
    */
   Collection<ExportPackage> getExportedPackages(Module module);
   
   /**
    * Get the exported packages by name and version
    * 
    * @param name the name of the package
    * @param range the version range or null if all are required
    * @return the exported packages
    */
   Collection<ExportPackage> getExportedPackages(String name, VersionRange range);
   
   /**
    * Get the imported modules for a module
    * 
    * @param name the name of the module or null for all
    * @param range the version range or null if all are required
    * @return the imported modules
    */
   Collection<ImportModule> getImportedModules(String name, VersionRange range);
   
   /**
    * Resolve the specified modules
    * 
    * @param modules the modules
    * @return the true if all the modules are resolved
    * @throws Exception for any error
    */
   boolean resolveModules(Module... modules) throws Exception;
   
   /**
    * Refresh stale modules
    * 
    * @param modules the modules
    * @throws Exception for any error
    */
   void refreshModules(Module... modules) throws Exception;
}
