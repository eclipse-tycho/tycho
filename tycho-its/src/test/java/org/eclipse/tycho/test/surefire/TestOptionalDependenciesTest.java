/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class TestOptionalDependenciesTest extends AbstractTychoIntegrationTest {

    // tests that optional dependencies can be disabled in the test runtime in case they are conflicting (cf. bug 351842)
    @Test
    public void testIgnoreMutuallyExclusiveOptionalDependenciesForTestRuntimeComputation() throws Exception {
        Verifier verifier = getVerifier("/surefire.optionalDependencies.ignore", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.getCliOptions().add("-De352-repo=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    } // see also OptionalDependenciesTest.testOptionallyRequiredBundleCanBeIgnored()

    // tests that optionalDependencies configuration only affects the current project (bug 367701)
    @Test
    public void reactorIndirectOptionalDependencies() throws Exception {
        Verifier verifier = getVerifier("/surefire.optionalDependencies.reactor", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

}
