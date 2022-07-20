/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationFactory;
import org.eclipse.sisu.osgi.launching.BundleReference;
import org.eclipse.sisu.osgi.launching.BundleStartLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultEquinoxInstallationFactoryTest {

    public class DefaultArtifactKey implements BundleReference {

        private String id;
        private String version;
        private File file;

        public DefaultArtifactKey(String id, String version, File file) {
            this.id = id;
            this.version = version;
            this.file = file;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public File getLocation() {
            return file;
        }

    }

    private Map<BundleReference, File> bundles;
    private DefaultEquinoxInstallationDescription instDesc;
    private BundleStartLevel defaultLevel;

    private DefaultEquinoxInstallationFactory subject;

    @BeforeEach
    public void setup() {
        bundles = new HashMap<>();
        File mockFile1 = mockFile("absolute/path/to/bundle1");
        bundles.put(new DefaultArtifactKey("org.example.bundle1", "1.0", mockFile1), mockFile1);
        File mockFile2 = mockFile("absolute/path/to/bundle2");
        bundles.put(new DefaultArtifactKey("org.example.bundle2", "1.0", mockFile2), mockFile2);

        instDesc = new DefaultEquinoxInstallationDescription();
        defaultLevel = new BundleStartLevel(null, 7, false);

        subject = new DefaultEquinoxInstallationFactory(mock(Logger.class));
    }

    @Test
    public void testExplicitlyConfiguredStartLevel() throws IOException {
        instDesc.addBundleStartLevel(new BundleStartLevel("org.example.bundle1", 6, false));

        List<String> config = splitAtComma(
                subject.toOsgiBundles(bundles, instDesc.getBundleStartLevel(), defaultLevel));
        assertTrue(config.contains("reference:file:absolute/path/to/bundle1@6"));
    }

    @Test
    public void testExplicitlyConfiguredStartLevelAndAutoStart() throws IOException {
        instDesc.addBundleStartLevel(new BundleStartLevel("org.example.bundle1", 6, true));

        List<String> config = splitAtComma(
                subject.toOsgiBundles(bundles, instDesc.getBundleStartLevel(), defaultLevel));
        assertTrue(config.contains("reference:file:absolute/path/to/bundle1@6:start"));
    }

    @Test
    public void testDefaultStartLevelIsNotSet() throws Exception {
        List<String> config = splitAtComma(
                subject.toOsgiBundles(bundles, instDesc.getBundleStartLevel(), defaultLevel));
        assertTrue(config.contains("reference:file:absolute/path/to/bundle2")); // don't need @n here because this would be redundant
    }

    @Test
    public void testDefaultAutoStartIsSet() throws Exception {
        defaultLevel = new BundleStartLevel(null, 7, true);

        List<String> config = splitAtComma(
                subject.toOsgiBundles(bundles, instDesc.getBundleStartLevel(), defaultLevel));
        assertTrue(config.contains("reference:file:absolute/path/to/bundle2@start"));
    }

    @Test
    public void testExplicitlyConfiguredAutoStart() throws Exception {
        instDesc.addBundleStartLevel(new BundleStartLevel("org.example.bundle1", 0, true)); // level attribute omitted

        List<String> config = splitAtComma(
                subject.toOsgiBundles(bundles, instDesc.getBundleStartLevel(), defaultLevel));
        assertTrue(config.contains("reference:file:absolute/path/to/bundle1@start")); // implicitly use default start level
    }

    private static File mockFile(String absolutePath) {
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn(absolutePath);
        return file;
    }

    private static List<String> splitAtComma(String string) {
        return Arrays.asList(string.split(","));
    }

}
