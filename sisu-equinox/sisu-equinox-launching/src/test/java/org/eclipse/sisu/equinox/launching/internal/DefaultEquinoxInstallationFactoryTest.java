/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

import static org.easymock.EasyMock.createNiceMock;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultEquinoxInstallationFactoryTest {

    private Map<ArtifactKey, File> bundles;
    private Map<String, BundleStartLevel> startLevel;
    private DefaultEquinoxInstallationFactory factory;

    @Before
    public void setup() {
        bundles = new HashMap<ArtifactKey, File>();
        ArtifactKey key1 = new DefaultArtifactKey("eclipse-plugin", "foo.bar.artifact1", "1.0");
        ArtifactKey key2 = new DefaultArtifactKey("eclipse-plugin", "foo.bar.artifact2", "1.0");
        bundles.put(key1, new File("artifact1File"));
        bundles.put(key2, new File("artifact2File"));
        startLevel = new HashMap<String, BundleStartLevel>();
        BundleStartLevel startLevel1 = new BundleStartLevel("foo.bar.artifact1", 6, true);
        startLevel.put("foo.bar.artifact1", startLevel1);
        Logger log = createNiceMock(Logger.class);
        factory = new DefaultEquinoxInstallationFactory(log);
    }

    @Test
    public void testBundleStartLevelsWithDefaultStartLevel() throws IOException {
        BundleStartLevel defaultLevel = new BundleStartLevel(null, 7, true);
        String osgiBundles = factory.toOsgiBundles(bundles, startLevel, defaultLevel);
        Assert.assertTrue(osgiBundles.contains("artifact1File@6:start")); // artifact1 must have its start level
        Assert.assertTrue(osgiBundles.contains("artifact2File@7:start")); // default must be used
    }

    @Test
    public void testBundleStartLevelsWithoutDefaultStartLevel() throws IOException {
        String osgiBundles = factory.toOsgiBundles(bundles, startLevel, null);
        Assert.assertTrue(osgiBundles.contains("artifact1File@6:start")); // artifact1 must have its start level
        Assert.assertTrue(osgiBundles.endsWith("artifact2File")); // no start level/autostart must be set
    }
}
