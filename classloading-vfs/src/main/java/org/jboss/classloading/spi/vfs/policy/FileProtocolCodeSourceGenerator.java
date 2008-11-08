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
package org.jboss.classloading.spi.vfs.policy;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;

/**
 * JBCL-64: Generates a CodeSource from a vfs URL
 * with the substitution of file as protocol
 * @author Anil.Saldhana@redhat.com
 * @since Nov 8, 2008
 */
public class FileProtocolCodeSourceGenerator implements CodeSourceGenerator
{  
   /**
    * Constant representing the URL file protocol
    */
   private static final String FILE_PROTOCOL = "file";
   
   /**
    * @see CodeSourceGenerator#getCodeSource(URL, Certificate[])s
    */
   public CodeSource getCodeSource(URL vfsURL, Certificate[] certs)
   throws MalformedURLException
   {
      URL codesourceURL = new URL(FILE_PROTOCOL,
            vfsURL.getHost(), vfsURL.getPort(),
            vfsURL.getFile());
      
      return new CodeSource(codesourceURL, certs);
   } 
}
