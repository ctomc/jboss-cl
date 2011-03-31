/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.classloading.spi.metadata.helpers;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.jboss.util.loading.Translator;

/**
 * Reflection based translator.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ReflectionTranslator implements Translator
{
   private Object translator;
   private Method method;

   public ReflectionTranslator(Object translator, String methodName) throws Exception
   {
      if (translator == null)
         throw new IllegalArgumentException("Null translator");
      if (methodName == null)
         throw new IllegalArgumentException("Null method name");

      this.translator = translator;
      this.method = translator.getClass().getMethod(methodName, ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class);
   }

   public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws Exception
   {
      return (byte[]) method.invoke(translator, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
   }

   public void unregisterClassLoader(ClassLoader loader)
   {
   }
}
