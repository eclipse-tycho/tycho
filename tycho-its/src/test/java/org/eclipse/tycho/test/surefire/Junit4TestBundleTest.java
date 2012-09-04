/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.EnvironmentUtil.isEclipse32Platform;
import static org.eclipse.tycho.test.util.SurefireUtil.testResultFile;
import static org.eclipse.tycho.test.util.TychoMatchers.exists;
import static org.junit.Assert.assertThat;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Junit4TestBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {

        if (isEclipse32Platform()) {
            // there is no JUnit 4 support in Eclipse 3.2
            return;
        }

        // a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
        Verifier verifier = getVerifier("surefire.junit4/bundle.test");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        assertThat(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit4Test"), exists());

        // ensure that JUnit 3 style tests also work -> related to bug 388909
        assertThat(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit3Test"), exists());
    }

}
