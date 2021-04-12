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

import java.io.FileInputStream;
import java.io.InputStream;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.zipcomparator.internal.ClassfileComparator;
import org.eclipse.tycho.zipcomparator.internal.ContentsComparator;
import org.eclipse.tycho.zipcomparator.internal.ManifestComparator;
import org.eclipse.tycho.zipcomparator.internal.PropertiesComparator;

public class ContentsComparatorTest extends PlexusTestCase {
    public void testManifest() throws Exception {
        assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST.MF"));
        assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST2.MF"));
        assertFalse(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST3.MF"));
    }

    public void testClassfile() throws Exception {
        assertTrue(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class"));
        assertFalse(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass$1.class"));
    }

    public void testProperties() throws Exception {
        assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props.properties"));
        assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props2.properties"));
        assertFalse(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props3.properties"));
    }

    public void testWithMalformedClasses() throws Exception {
        assertTrue(isContentEqual(ClassfileComparator.TYPE, "src/test/resources/classfiles/MalformedClass1.clazz",
                "src/test/resources/classfiles/MalformedClass1.clazz"));
        assertFalse(isContentEqual(ClassfileComparator.TYPE, "src/test/resources/classfiles/MalformedClass1.clazz",
                "src/test/resources/classfiles/MalformedClass2.clazz"));
    }

    private boolean isContentEqual(String type, String baseline, String reactor) throws Exception {
        ContentsComparator comparator = lookup(ContentsComparator.class, type);
        try (InputStream is = new FileInputStream(baseline)) {
            try (InputStream is2 = new FileInputStream(reactor)) {
                return comparator.getDelta(is, is2, null) == null;
            }
        }
    }
}
