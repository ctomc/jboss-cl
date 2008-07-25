/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.classloading.vfs.client.support.launcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ClientContainer
{
   private Class<?> mainClass;
   private Object metaData;
   private String applicationClientName;

   public ClientContainer(Object metaData, Class<?> mainClass, String applicationClientName)
   {
      this.metaData = metaData;
      this.mainClass = mainClass;
      this.applicationClientName = applicationClientName;
   }

   public Class<?> getMainClass()
   {
      return mainClass;
   }

   public void invokeMain(String args[])
      throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException,
      InvocationTargetException
   {
      Class<?> parameterTypes[] = { args.getClass() };
      Method method = mainClass.getDeclaredMethod("main", parameterTypes);
      method.invoke(null, (Object) args);
   }

}
