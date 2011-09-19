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
package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO45Test extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        String tychoVersion = getTychoVersion();

        Verifier verifier = getVerifier("TYCHO45");

        // generate poms
        verifier.getCliOptions().add("-DgroupId=tycho45");
        verifier.getCliOptions().add("-DtestSuite=tests.suite");
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-init.txt");
        verifier.executeGoal("org.eclipse.tycho:tycho-pomgenerator-plugin:" + tychoVersion + ":generate-poms");
        verifier.verifyErrorFreeLog();

        // run the build
        verifier.getCliOptions().add("-DtestClass=tests.suite.AllTests");
        verifier.setLogFileName("log-test.txt");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }
}
