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
package org.jboss.classloading.spi.metadata;

import java.io.Serializable;

/**
 * Requirement.
 * 
 * @author <a href="adrian@jboss.org">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public interface Requirement extends Serializable
{
   /**
    * Whether to re-export the requirement
    * 
    * @return true to re-export
    */
   boolean isReExport();
   
   /**
    * Whether we want re-exports
    * 
    * @return true to process re-exports
    */
   boolean wantReExports();
   
   /**
    * Whether the requirement is optional
    * 
    * @return true if the requirement is optional
    */
   boolean isOptional();
   
   /**
    * Whether the requirement is dynamic
    * 
    * @return true if the requirement is dynamic
    */
   boolean isDynamic();
   
   /**
    * Check whether this requirement is consistent with another requirement.<p>
    * 
    * Typically they will be inconsistent if they are the same type,
    * have the same name but a different version
    * 
    * @param other the other requirement
    * @return true when consistent, false when inconsistent
    */
   boolean isConsistent(Requirement other);
}
