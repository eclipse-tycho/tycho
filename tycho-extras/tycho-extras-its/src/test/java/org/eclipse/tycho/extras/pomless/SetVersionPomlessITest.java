/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronics GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bachmann electronics GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pomless;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.extras.its.AbstractTychoExtrasIntegrationTest;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.versions.pom.PomFile;
import org.junit.Assert;
import org.junit.Test;

public class SetVersionPomlessITest extends AbstractTychoExtrasIntegrationTest {

    @Test
    public void testPomlessBuildExtension() throws Exception {
        Verifier verifier = getVerifier("testsetversionpomless", false);
        String newVersion = "2.0.0";
        verifier.addCliArgument("-DnewVersion=" + newVersion);
        verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + getTychoVersion() + ":set-version");
        verifier.verifyErrorFreeLog();
        File baseDir = new File(verifier.getBasedir());

        PomFile rootPom = PomFile.read(new File(baseDir, "pom.xml"), false);
        Assert.assertEquals(newVersion, rootPom.getVersion());

        DefaultBundleReader reader = new DefaultBundleReader();
        OsgiManifest bundleManifest = reader.loadManifest(new File(baseDir, "bundle1/META-INF/MANIFEST.MF"));
        Assert.assertEquals(newVersion, bundleManifest.getBundleVersion());

        OsgiManifest testBundleManifest = reader.loadManifest(new File(baseDir, "bundle1.tests/META-INF/MANIFEST.MF"));
        Assert.assertEquals(newVersion, testBundleManifest.getBundleVersion());

        Feature feature = Feature.read(new File(baseDir, "feature/feature.xml"));
        Assert.assertEquals(newVersion, feature.getVersion());
    }

}
