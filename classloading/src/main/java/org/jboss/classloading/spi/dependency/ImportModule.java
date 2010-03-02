package org.jboss.classloading.spi.dependency;

import java.util.Collection;

import org.jboss.classloading.plugins.metadata.ModuleRequirement;


/**
 * An imported module
 *
 * @author adrian@jboss.org
 */
public class ImportModule
{
   /** The module */
   private Module module;

   /**
    * Create a new ExportPackage
    *
    * @param module the module
    */
   ImportModule(Module module)
   {
      this.module = module;
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
      return module.getName();
   }
   
   /**
    * Get the version
    * 
    * @return the version
    */
   public Object getVersion()
   {
      return module.getVersion();
   }
   
   /**
    * Get the importing modules
    * 
    * @return the importing modules
    */
   public Collection<Module> getImportingModules()
   {
      return module.getImportingModules(ModuleRequirement.class);
   }

   /**
    * Whether the module is unregistered
    * 
    * @return true when unregistered
    */
   public boolean isUnregistered()
   {
      return module.getClassLoader() == null;
   }
   
   @Override
   public String toString()
   {
      return module.toString();
   }
   
   @Override
   public int hashCode()
   {
      return module.hashCode();
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null || obj instanceof ImportModule == false)

         return false;
      
      ImportModule other = (ImportModule) obj;
      return module.equals(other.module);
   }
}
