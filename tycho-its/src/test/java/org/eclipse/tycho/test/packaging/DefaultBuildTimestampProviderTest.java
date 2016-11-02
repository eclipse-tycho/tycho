/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.packaging;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.maven.it.Verifier;
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

        String mavenBuildTimestamp = readFileToString(
                new File(baseDir, "mavenPlugin/target/classes/buildtimestamp.txt")).toString();

        // Tycho ITs are running with maven 3.0. In that version of maven, the
        // build timestamp uses the local time zone, Tycho is using UTC, so we
        // convert here the maven timestamp to UCT before comparing
        // see: https://issues.apache.org/jira/browse/MNG-5452
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date buildTimestamp = format.parse(mavenBuildTimestamp);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        String plugin1Manifest = readFileToString(new File(baseDir, "plugin01/target/MANIFEST.MF")).toString();
        String plugin2Manifest = readFileToString(new File(baseDir, "plugin02/target/MANIFEST.MF")).toString();
        Assert.assertTrue("Expected" + mavenBuildTimestamp + "\nbut was\n" + plugin1Manifest,
                plugin1Manifest.contains("Bundle-Version: 1.0.0." + format.format(buildTimestamp)));
        Assert.assertTrue(plugin2Manifest.contains("Bundle-Version: 1.0.0." + format.format(buildTimestamp)));
    }

}
