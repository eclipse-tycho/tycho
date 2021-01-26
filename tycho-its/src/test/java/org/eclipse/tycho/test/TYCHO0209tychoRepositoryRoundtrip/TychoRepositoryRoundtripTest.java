/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO0209tychoRepositoryRoundtrip;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Assert;
import org.junit.Test;

public class TychoRepositoryRoundtripTest extends AbstractTychoIntegrationTest {

    @Test
    public void testLocalMavenRepository() throws Exception {
        // build01
        Verifier v01 = getVerifier("TYCHO0209tychoRepositoryRoundtrip/build01", false);
        v01.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_LATEST.toString());
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();

        final boolean ignoreLocallyInstalledArtifacts = false;
        // build02, some dependencies come from local, some from remote repositories
        Verifier v02 = getVerifier("TYCHO0209tychoRepositoryRoundtrip/build02", false, ignoreLocallyInstalledArtifacts);
        v02.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_LATEST.toString());
        v02.executeGoal("install");
        v02.verifyErrorFreeLog();
        v02.verifyTextInLog(
                "[WARNING] The following locally built units have been used to resolve project dependencies:");
        v02.verifyTextInLog("[WARNING]   org.codehaus.tycho.tychoits.tycho0209.build01.bundle01/0.0.1.");
        File site = new File(v02.getBasedir(), "build02.site01/target/site");
        Assert.assertEquals(2, new File(site, "features").listFiles().length);
        Assert.assertEquals(3, new File(site, "plugins").listFiles().length);

        // build03, all dependencies come from local repository
        Verifier v03 = getVerifier("TYCHO0209tychoRepositoryRoundtrip/build03", false, ignoreLocallyInstalledArtifacts);
        v03.executeGoal("install");
        v03.verifyErrorFreeLog();
    }

}
