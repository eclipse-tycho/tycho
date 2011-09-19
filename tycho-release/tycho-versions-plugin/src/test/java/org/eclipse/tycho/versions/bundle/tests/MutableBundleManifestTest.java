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
package org.eclipse.tycho.versions.bundle.tests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.pom.tests.MutablePomFileTest;
import org.junit.Assert;
import org.junit.Test;

public class MutableBundleManifestTest {
    @Test
    public void echo() throws Exception {
        assertRoundtrip("/manifests/m01.mf");
        assertRoundtrip("/manifests/m02.mf");
        assertRoundtrip("/manifests/m03.mf");
    }

    @Test
    public void getters() throws IOException {
        MutableBundleManifest mf = getManifest("/manifests/getters.mf");

        Assert.assertEquals("1.0.0.qualifier", mf.getVersion());
        Assert.assertEquals("TYCHO0214versionChange.bundle01", mf.getSymbolicName());
    }

    @Test
    public void setVersion() throws Exception {
        MutableBundleManifest mf = getManifest("/manifests/setVersion.mf");

        mf.setVersion("1.0.1.qualifier");
        assertContents(mf, "/manifests/setVersion.mf_expected");

        mf.setVersion("1.0.1.qualifierrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr");
        assertContents(mf, "/manifests/setVersion.mf_expected72");
    }

    private void assertRoundtrip(String path) throws IOException {
        MutableBundleManifest mf = getManifest(path);

        assertContents(mf, path);
    }

    private void assertContents(MutableBundleManifest mf, String path) throws UnsupportedEncodingException, IOException {
        Assert.assertEquals(toAsciiString(toByteArray(path)), toAsciiString(mf));
    }

    private String toAsciiString(MutableBundleManifest mf) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        MutableBundleManifest.write(mf, buf);

        String actual = toAsciiString(buf.toByteArray());
        return actual;
    }

    private MutableBundleManifest getManifest(String path) throws IOException {
        return MutableBundleManifest.read(getClass().getResourceAsStream(path));
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
        BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), "ascii"));
        try {
            StringBuilder sb = new StringBuilder();
            String str;
            while ((str = r.readLine()) != null) {
                sb.append(str).append('\n');
            }

            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.close(r);
        }
    }

}
