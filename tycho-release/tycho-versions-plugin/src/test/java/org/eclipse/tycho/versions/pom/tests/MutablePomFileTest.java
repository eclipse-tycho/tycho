/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.pom.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.versions.pom.MutablePomFile;
import org.junit.Assert;
import org.junit.Test;

public class MutablePomFileTest {
    @Test
    public void setVersion001() throws Exception {
        MutablePomFile pom = getPom("/poms/setVersion001.xml");
        pom.setVersion("1.2.3.qualifier");
        assertContent(pom, "/poms/setVersion001_expected.xml");
    }

    @Test
    public void setVersion002() throws Exception {
        MutablePomFile pom = getPom("/poms/setVersion002.xml");
        pom.setVersion("1.2.3.qualifier");
        assertContent(pom, "/poms/setVersion002_expected.xml");
    }

    private MutablePomFile getPom(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        try {
            return MutablePomFile.read(is);
        } finally {
            IOUtil.close(is);
        }
    }

    private static void assertContent(MutablePomFile pom, String path) throws IOException {
        byte[] expected = toByteArray(path);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        MutablePomFile.write(pom, buf);
        byte[] actual = buf.toByteArray();

        Assert.assertEquals(toAsciiString(expected), toAsciiString(actual));
    }

    private static byte[] toByteArray(String path) throws IOException {
        byte expected[];
        InputStream is = MutablePomFileTest.class.getResourceAsStream(path);
        try {
            expected = IOUtil.toByteArray(is);
        } finally {
            IOUtil.close(is);
        }
        return expected;
    }

    private static String toAsciiString(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes, "ascii");
    }

}
