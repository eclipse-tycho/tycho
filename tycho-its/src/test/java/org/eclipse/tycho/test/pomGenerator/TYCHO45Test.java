/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.pomGenerator;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO45Test extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        String tychoVersion = getTychoVersion();

        Verifier verifier = getVerifier("pomGenerator.testSuite");

        // generate poms
        verifier.getSystemProperties().setProperty("groupId", "tycho45");
        verifier.getSystemProperties().setProperty("failIfNoTests", "false");
        verifier.getSystemProperties().setProperty("repoURL", "https://download.eclipse.org/releases/oxygen/");
        verifier.getSystemProperties().setProperty("repoName", "oxygen");
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-init.txt");
        verifier.executeGoal("org.eclipse.tycho:tycho-pomgenerator-plugin:" + tychoVersion + ":generate-poms");
        verifier.verifyErrorFreeLog();

        // run the build
        verifier.getSystemProperties().setProperty("testClass", "tests.suite.AllTests");
        verifier.setLogFileName("log-test.txt");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }
}
