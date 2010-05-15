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
package org.jboss.classloading.spi.dependency.wildcard;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.RequirementDependencyItem;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;

/**
 * WildcardRequirementDependencyItem.

 * We don't want to be undeployed when our
 * wildcarded dependency goes away, hence ignoring iDependOn and dependsOnMe.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class WildcardRequirementDependencyItem extends RequirementDependencyItem
{
   public WildcardRequirementDependencyItem(Module module, Requirement requirement, ControllerState state)
   {
      super(module, requirement, state);
   }

   public WildcardRequirementDependencyItem(Module module, Requirement requirement, ControllerState whenRequired, ControllerState dependentState)
   {
      super(module, requirement, whenRequired, dependentState);
   }

   @Override
   public boolean resolve(Controller controller)
   {
      // Always resolved
      setResolved(true);
      return isResolved();
   }

   @Override
   protected void addDependsOnMe(Controller controller, ControllerContext context)
   {
      // ignore
   }

   /**
    * We depend on this module.
    * e.g. add dependency for refresh callback.
    *
    * @param module the module
    */
   void addIDependOn(Module module)
   {
      addDepends(module);
   }
}