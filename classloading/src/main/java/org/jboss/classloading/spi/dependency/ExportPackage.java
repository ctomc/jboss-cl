package org.jboss.classloading.spi.dependency;

import java.util.Collection;

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.plugins.metadata.PackageRequirement;

/**
 * An exported package
 *
 * @author adrian@jboss.org
 */
public class ExportPackage
{
   /** The module exporting the package */
   private Module module;
   
   /** The package capability */
   private PackageCapability capability;

   /**
    * Create a new ExportPackage
    *
    * @param module the module
    * @param capability the capability
    */
   ExportPackage(Module module, PackageCapability capability)
   {
      this.module = module;
      this.capability = capability;
   }

   /**
    * Get the module
    *
    * @return the module
    */
   public Module getModule()
   {
      return module;
   }

   /**
    * Get the name
    * 
    * @return the name
    */
   public String getName()
   {
      return capability.getName();
   }
   
   /**
    * Get the version
    * 
    * @return the version
    */
   public Object getVersion()
   {
      return capability.getVersion();
   }
   
   /**
    * Get the importing modules
    * 
    * @return the importing modules
    */
   public Collection<Module> getImportingModules()
   {
      return module.getImportingModules(PackageRequirement.class);
   }

   /**
    * Whether the package is unregistered
    * 
    * @return true when unregistered
    */
   public boolean isUnregistered()
   {
      return module.getClassLoader() == null;
   }
   
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof ExportPackage == false)
         return false;

      ExportPackage other = (ExportPackage) obj;
      if (module.equals(other.getModule()) == false)
         return false;
      return capability.equals(other.capability);
   }
   
   public int hashCode()
   {
      return capability.hashCode();
   }
   
   public String toString()
   {
      return module + "/" + capability;
   }
}
