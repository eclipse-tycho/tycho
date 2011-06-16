/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        v01.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_342.toString());
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();

        // build02, some dependencies come from local, some from remote repositories
        Verifier v02 = getVerifier("TYCHO0209tychoRepositoryRoundtrip/build02", false);
        v02.getSystemProperties().setProperty("p2.repo", P2Repositories.ECLIPSE_342.toString());
        v02.executeGoal("install");
        v02.verifyErrorFreeLog();
        File site = new File(v02.getBasedir(), "build02.site01/target/site");
        Assert.assertEquals(2, new File(site, "features").listFiles().length);
        Assert.assertEquals(3, new File(site, "plugins").listFiles().length);

        // build03, all dependencies come from local repository
        Verifier v03 = getVerifier("TYCHO0209tychoRepositoryRoundtrip/build03", false);
        v03.executeGoal("install");
        v03.verifyErrorFreeLog();
    }

}
