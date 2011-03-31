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

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.util.loading.Translator;

/**
 * TranslatorScope.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public enum TranslatorScope
{
   SYSTEM
         {
            @Override
            public void addTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
               system.addTranslator(translator);
            }

            @Override
            public void removeTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
               system.removeTranslator(translator);
            }
         },
   DOMAIN
         {
            @Override
            public void addTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
            }

            @Override
            public void removeTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
            }
         },
   POLICY
         {
            @Override
            public void addTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
               policy.addTranslator(translator);
            }

            @Override
            public void removeTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator)
            {
               policy.removeTranslator(translator);
            }
         };

   /**
    * Add translator.
    *
    * @param system     the CL system
    * @param domain     the CL domain
    * @param policy     the CL policy
    * @param translator the trsnslator
    */
   public abstract void addTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator);

   /**
    * Remove translator.
    *
    * @param system     the CL system
    * @param domain     the CL domain
    * @param policy     the CL policy
    * @param translator the trsnslator
    */
   public abstract void removeTranslator(ClassLoaderSystem system, ClassLoaderDomain domain, ClassLoaderPolicy policy, Translator translator);
}