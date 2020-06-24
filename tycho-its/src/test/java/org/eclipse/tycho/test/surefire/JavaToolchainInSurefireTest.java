/*******************************************************************************
 * Copyright (c) 2011 SAP AG. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JavaToolchainInSurefireTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("surefire.toolchains");
        File toolchains = new File(verifier.getBasedir() + "/toolchains.xml");
        verifier.getCliOptions().add("--toolchains " + toolchains.getCanonicalPath());
        verifier.executeGoal("integration-test");
        verifier.verifyTextInLog("Toolchain in tycho-surefire-plugin: JDK[fake-jdk-home]");
    }
}
