/*******************************************************************************
 * Copyright (c) 2012, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.packaging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.downloadstats.DownloadStatsTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Assume;
import org.junit.Test;

// tests the pack200 support (bug 377357)
public class RepositoryPackedArtifactsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Assume.assumeTrue("Pack200 stuff ignored for Java 14+", Runtime.version().feature() < 14);
        Verifier verifier = getVerifier("/packaging.pack200", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        assertDirectory(new File(basedir, "product.pack200/target/repository/plugins/"), "bundle_1.0.0.123abc.jar",
                "bundle_1.0.0.123abc.jar.pack.gz", "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar",
                "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar.pack.gz");

        assertDirectory(new File(basedir, "product.nopack200/target/repository/plugins/"), "bundle_1.0.0.123abc.jar",
                "org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");

        // TODO verify metadata contains packed artifacts
    }

    @Test
    public void testDownloadStatsAlsoAttachedToPack200() throws Exception {
        Assume.assumeTrue("Pack200 stuff ignored for Java 14+", Runtime.version().feature() < 14);
        Verifier verifier = getVerifier("/packaging.pack200", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.getSystemProperties().put("tycho.generateDownloadStatsProperty", "true");
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        JarFile artifactsJar = new JarFile(
                new File(verifier.getBasedir(), "product.pack200/target/repository/artifacts.jar"));
        assertEquals(2, DownloadStatsTest.captureDownloadStatsFromArtifactsJar(artifactsJar,
                artifactElement -> artifactElement.getAttributeValue("id").equals("bundle")).size());
    }

    private void assertDirectory(File dir, String... expectedFiles) {
        Set<String> actualFiles = new HashSet<>(Arrays.asList(dir.list()));
        assertEquals(new HashSet<>(Arrays.asList(expectedFiles)), actualFiles);
    }
}
