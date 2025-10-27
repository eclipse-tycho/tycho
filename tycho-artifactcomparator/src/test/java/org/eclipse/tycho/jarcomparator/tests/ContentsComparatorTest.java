/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.jarcomparator.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.eclipse.tycho.zipcomparator.internal.ClassfileComparator;
import org.eclipse.tycho.zipcomparator.internal.ContentsComparator;
import org.eclipse.tycho.zipcomparator.internal.ManifestComparator;
import org.eclipse.tycho.zipcomparator.internal.PropertiesComparator;
import org.junit.jupiter.api.Test;

@PlexusTest
public class ContentsComparatorTest {

    @Inject
    private PlexusContainer container;

    @Test
    public void testManifest() throws Exception {
        assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST.MF"));
        assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST2.MF"));
        assertFalse(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST3.MF"));
    }

    @Test
    public void testClassfile() throws Exception {
        assertTrue(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class"));
        assertFalse(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass$1.class"));
    }

    @Test
    public void testProperties() throws Exception {
        assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props.properties"));
        assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props2.properties"));
        assertFalse(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props3.properties"));
    }

    @Test
    public void testWithMalformedClasses() throws Exception {
        assertTrue(isContentEqual(ClassfileComparator.TYPE, "src/test/resources/classfiles/MalformedClass1.clazz",
                "src/test/resources/classfiles/MalformedClass1.clazz"));
        assertFalse(isContentEqual(ClassfileComparator.TYPE, "src/test/resources/classfiles/MalformedClass1.clazz",
                "src/test/resources/classfiles/MalformedClass2.clazz"));
    }

    @Test
    public void testManifestWithJavaVersion() throws Exception {
        // Test that manifests with different Java-Version headers are considered equal (ignored)
        assertTrue(isContentEqual(ManifestComparator.TYPE,
                "src/test/resources/manifest/MANIFEST-JavaVersion1.MF",
                "src/test/resources/manifest/MANIFEST-JavaVersion2.MF"));
    }

    private boolean isContentEqual(String type, String baseline, String reactor) throws Exception {
        ContentsComparator comparator = container.lookup(ContentsComparator.class, type);
        try (InputStream is = new FileInputStream(baseline)) {
            try (InputStream is2 = new FileInputStream(reactor)) {
                return comparator.getDelta(new ComparatorInputStream(is), new ComparatorInputStream(is2), null) == null;
            }
        }
    }
}
