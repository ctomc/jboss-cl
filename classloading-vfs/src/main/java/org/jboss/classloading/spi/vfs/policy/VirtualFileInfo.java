package org.jboss.classloading.spi.vfs.policy;

import org.jboss.virtual.VirtualFile;

/**
 * A cache entry in the policy that combines the entry with its associated root.
 * 
 * @author <a href="adrian@jboss.org">Adrian Brock</a> 
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class VirtualFileInfo
{
   /** The file */
   private VirtualFile file;

   /** The root */
   private VirtualFile root;

   public VirtualFileInfo(VirtualFile file, VirtualFile root)
   {
      this.file = file;
      this.root = root;
   }

   /**
    * Get the file.
    * 
    * @return the file.
    */
   public VirtualFile getFile()
   {
      return file;
   }

   /**
    * Get the root.
    * 
    * @return the root.
    */
   public VirtualFile getRoot()
   {
      return root;
   }
}