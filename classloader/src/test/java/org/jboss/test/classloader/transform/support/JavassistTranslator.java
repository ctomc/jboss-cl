/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloader.transform.support;

import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.jboss.util.loading.Translator;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class JavassistTranslator implements Translator
{
   private ClassPool pool = ClassPool.getDefault();

   public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws Exception
   {
      // do not modify MethodHelper
      if (className.contains("MethodHelper"))
         return classfileBuffer;

      CtClass ctClass = pool.getCtClass(className);
      ctClass.defrost();
      addMethods(ctClass);
      return ctClass.toBytecode();
   }

   protected abstract void addMethods(CtClass ctClass) throws Exception;

   protected void addMethod(CtClass ctClass, String methodName) throws Exception
   {
      CtMethod body = pool.getCtClass(MethodHelper.class.getName()).getDeclaredMethod(methodName);
      CtMethod newMethod = CtNewMethod.delegator(body, ctClass);
      ctClass.addMethod(newMethod);
   }

   public void unregisterClassLoader(ClassLoader loader)
   {
   }
}