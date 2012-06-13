/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.jarcomparator.tests;

import java.io.FileInputStream;
import java.io.InputStream;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.zipcomparator.internal.ClassfileComparator;
import org.eclipse.tycho.zipcomparator.internal.ContentsComparator;
import org.eclipse.tycho.zipcomparator.internal.ManifestComparator;
import org.eclipse.tycho.zipcomparator.internal.PropertiesComparator;
import org.junit.Assert;

public class ContentsComparatorTest extends PlexusTestCase {
    public void testManifest() throws Exception {
        Assert.assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST.MF"));
        Assert.assertTrue(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST2.MF"));
        Assert.assertFalse(isContentEqual(ManifestComparator.TYPE, "src/test/resources/manifest/MANIFEST.MF",
                "src/test/resources/manifest/MANIFEST3.MF"));
    }

    public void testClassfile() throws Exception {
        Assert.assertTrue(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class"));
        Assert.assertFalse(isContentEqual(ClassfileComparator.TYPE,
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass.class",
                "target/test-classes/org/eclipse/tycho/jarcomparator/testdata/JavaClass$1.class"));
    }

    public void testProperties() throws Exception {
        Assert.assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props.properties"));
        Assert.assertTrue(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props2.properties"));
        Assert.assertFalse(isContentEqual(PropertiesComparator.TYPE, "src/test/resources/properties/props.properties",
                "src/test/resources/properties/props3.properties"));
    }

    private boolean isContentEqual(String type, String baseline, String reactor) throws Exception {
        ContentsComparator comparator = lookup(ContentsComparator.class, type);
        InputStream is = new FileInputStream(baseline);
        try {
            InputStream is2 = new FileInputStream(reactor);
            try {
                return comparator.getDelta(is, is2) == null;
            } finally {
                IOUtil.close(is2);
            }
        } finally {
            IOUtil.close(is);
        }
    }
}
