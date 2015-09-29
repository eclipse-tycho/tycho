/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.versions.bundle.ManifestAttribute;
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
        Assert.assertEquals("host-bundle", mf.getFragmentHostSymbolicName());
        Assert.assertEquals("1.0.0.qualifier", mf.getFragmentHostVersion());

        Map<String, String> expectedRequiredBundleVersion = new HashMap<>();
        expectedRequiredBundleVersion.put("bundle1", "1.0.0");
        expectedRequiredBundleVersion.put("bundle2", "1.1.0");
        expectedRequiredBundleVersion.put("bundle3", null);
        Assert.assertEquals(expectedRequiredBundleVersion, mf.getRequiredBundleVersions());

        Map<String, String> expectedImportPackage = new HashMap<>();
        expectedImportPackage.put("com.package1", null);
        expectedImportPackage.put("com.package2", "2.6.0");
        Assert.assertEquals(expectedImportPackage, mf.getImportPackagesVersions());

    }

    @Test
    public void setFragmentHostVersion() throws IOException {
        MutableBundleManifest mf = getManifest("/manifests/setFragmentHostVersion.mf");
        mf.setFragmentHostVersion("1.0.1");
        assertContents(mf, "/manifests/setFragmentHostVersion.mf_expected");
    }

    @Test
    public void updateRequiredBundleVersions() throws IOException {
        MutableBundleManifest mf = getManifest("/manifests/updateRequiredBundleVersions.mf");
        Map<String, String> requiredBundleVersionChanges = new HashMap<>();
        requiredBundleVersionChanges.put("bundle1", "1.0.1");
        requiredBundleVersionChanges.put("bundle2", "1.1.1");
        mf.updateRequiredBundleVersions(requiredBundleVersionChanges);
        assertContents(mf, "/manifests/updateRequiredBundleVersions.mf_expected");
    }

    @Test
    public void updateImportedPackageVersions() throws IOException {
        MutableBundleManifest mf = getManifest("/manifests/updateImportedPackageVersions.mf");

        Map<String, String> importPackageVersionChanges = new HashMap<>();
        importPackageVersionChanges.put("com.package1", "1.0.1");
        importPackageVersionChanges.put("com.package2", "1.1.1");
        mf.updateImportedPackageVersions(importPackageVersionChanges);
        assertContents(mf, "/manifests/updateImportedPackageVersions.mf_expected");
    }

    @Test
    public void updateExportedPackageVersions() throws IOException {
        MutableBundleManifest mf = getManifest("/manifests/updateExportedPackageVersions.mf");

        Map<String, String> importPackageVersionChanges = new HashMap<>();
        importPackageVersionChanges.put("com.package1", "1.0.1");
        importPackageVersionChanges.put("com.package2", "1.1.0");
        mf.updateExportedPackageVersions(importPackageVersionChanges);
        assertContents(mf, "/manifests/updateExportedPackageVersions.mf_expected");
    }

    @Test
    public void updateExportedPackageVersionsDoesNotReformatIfNotNecessary() throws Exception {
        MutableBundleManifest mf = getManifest("/manifests/updateExportedPackageVersions.mf");

        // change nothing: the specified versions are already the version presents in the manifest
        Map<String, String> importPackageVersionChanges = new HashMap<>();
        importPackageVersionChanges.put("com.package1", "1.0.0");
        importPackageVersionChanges.put("com.package2", "1.0.0");

        // expect that nothing is changed (that is the formatting remains intact)
        assertContents(mf, "/manifests/updateExportedPackageVersions.mf");
    }

    @Test
    public void addAttribute() throws Exception {
        MutableBundleManifest mf = getManifest("/manifests/addheader.mf");
        mf.add(new ManifestAttribute("header", "value"));
        assertContents(mf, "/manifests/addheader.mf_expected");
    }

    @Test
    public void shouldRoundtripWithoutLineEnding() throws Exception {
        // given
        String manifestStr = "Bundle-SymbolicName: name";

        // when
        InputStream manifestIs = new ByteArrayInputStream(manifestStr.getBytes("ascii"));
        MutableBundleManifest manifest = MutableBundleManifest.read(manifestIs);
        String written = toAsciiString(manifest);

        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
    }

    @Test
    public void shouldPreserveWindowsLineEndings() throws Exception {
        // given
        String manifestStr = "Bundle-SymbolicName: name\r\nBundle-Version: version\r\n\r\nUnparsed1\r\nUnparsed2\r\n";

        // when
        InputStream manifestIs = new ByteArrayInputStream(manifestStr.getBytes("ascii"));
        MutableBundleManifest manifest = MutableBundleManifest.read(manifestIs);
        String written = toAsciiString(manifest);

        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    @Test
    public void shouldPreserveUnixLineEndings() throws Exception {
        // given
        String manifestStr = "Bundle-SymbolicName: name\nBundle-Version: version\n\nUnparsed1\nUnparsed2\n";

        // when
        InputStream manifestIs = new ByteArrayInputStream(manifestStr.getBytes("ascii"));
        MutableBundleManifest manifest = MutableBundleManifest.read(manifestIs);
        String written = toAsciiString(manifest);

        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    @Test
    public void shouldPreserveOldMacLineEndings() throws Exception {
        // given
        String manifestStr = "Bundle-SymbolicName: name\rBundle-Version: version\r\rUnparsed1\rUnparsed2\r";

        // when
        InputStream manifestIs = new ByteArrayInputStream(manifestStr.getBytes("ascii"));
        MutableBundleManifest manifest = MutableBundleManifest.read(manifestIs);
        String written = toAsciiString(manifest);

        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    private void assertRoundtrip(String path) throws IOException {
        MutableBundleManifest mf = getManifest(path);

        assertContents(mf, path);
    }

    private void assertContents(MutableBundleManifest mf, String path)
            throws UnsupportedEncodingException, IOException {
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
        return new String(bytes, "ascii");
    }

}
