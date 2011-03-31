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
package org.jboss.test.classloading.metadata.xml.test;

import java.util.List;

import junit.framework.Test;
import org.jboss.classloading.spi.metadata.ClassLoadingTranslatorMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingTranslatorsMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingTranslatorsMetaData10;
import org.jboss.classloading.spi.metadata.TranslatorScope;
import org.jboss.test.classloading.metadata.xml.AbstractJBossXBTest;

/**
 * ClassLoadingDomainMetaDataXmlUnitTestCase.
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class ClassLoadingTranslatorsMetaDataXmlUnitTestCase extends AbstractJBossXBTest
{
   public static Test suite()
   {
      return suite(ClassLoadingTranslatorsMetaDataXmlUnitTestCase.class);
   }

   public ClassLoadingTranslatorsMetaDataXmlUnitTestCase(String name)
   {
      super(name);
   }

   public void testTranslators() throws Exception
   {
      ClassLoadingTranslatorsMetaData result = unmarshal();
      assertNotNull(result);
      List<ClassLoadingTranslatorMetaData> translators = result.getTranslators();
      assertNotNull(translators);
      assertEquals(3, translators.size());
      assertEquals(translators.get(0), "org.jboss.acme.X", null, TranslatorScope.SYSTEM);
      assertEquals(translators.get(1), "org.jboss.acme.X", null, TranslatorScope.DOMAIN);
      assertEquals(translators.get(2), "org.jboss.acme.X", "bable", TranslatorScope.SYSTEM);
   }

   protected void assertEquals(ClassLoadingTranslatorMetaData translator, String className, String method, TranslatorScope scope)
   {
      assertEquals(className, translator.getClassName());
      assertEquals(method, translator.getMethod());
      assertEquals(scope, translator.getScope());
   }

   protected ClassLoadingTranslatorsMetaData unmarshal(Class<?>... extra) throws Exception
   {
      return unmarshalObject(ClassLoadingTranslatorsMetaData.class, ClassLoadingTranslatorsMetaData10.class, extra);
   }
}
