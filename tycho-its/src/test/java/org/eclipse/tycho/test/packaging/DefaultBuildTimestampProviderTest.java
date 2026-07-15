/*******************************************************************************
 * Copyright (c) 2016, 2021 Bachmann electronic GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.packaging;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test uses a project that contains 3 plugins (2 eclipse-plugins and one maven plugin). The
 * maven plugin contains a text file where the maven.build.buildtimestamp is written to. It compares
 * the content of that file with the qualifier of the Bundle-Version in the eclipse plugins manifest
 * files.
 *
 */
public class DefaultBuildTimestampProviderTest extends AbstractTychoIntegrationTest {

    @Test
    public void testDefaulBuildTimestampIsTheMavenBuildTimestamp() throws Exception {
        Verifier verifier = getVerifier("/packaging.buildtimestamp", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        File baseDir = new File(verifier.getBasedir());

        String mavenBuildTimestamp = Files
                .readString(Paths.get(baseDir.getAbsolutePath(), "mavenPlugin/target/classes/buildtimestamp.txt"));

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date buildTimestamp = format.parse(mavenBuildTimestamp);
        String plugin1Manifest = Files.readString(Paths.get(baseDir.getAbsolutePath(), "plugin01/target/MANIFEST.MF"));
        String plugin2Manifest = Files.readString(Paths.get(baseDir.getAbsolutePath(), "plugin02/target/MANIFEST.MF"));
        String expectedBundleVersion = "Bundle-Version: 1.0.0." + format.format(buildTimestamp);
        Assert.assertTrue(
                "Expected Bundle-Version in MANIFEST: '" + expectedBundleVersion + "'\nbut was\n" + plugin1Manifest,
                plugin1Manifest.contains(expectedBundleVersion));
        Assert.assertTrue(plugin2Manifest.contains(expectedBundleVersion));
    }

}
