/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug367637_autoNoSourceBundle;

import java.util.Arrays;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class AutoNoSourceBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        // the point of the test is to make sure Tycho does NOT allow references to missing source bundles

        Verifier verifier = getVerifier("/367637_autoNoSourceBundle", false);
        try {
            verifier.executeGoals(Arrays.asList("clean", "install"));
            Assert.fail("Reference to a missing source bundle did not fail the build");
        } catch (VerificationException expected) {
            verifier.verifyTextInLog("Unable to satisfy dependency from feature.feature.group 1.0.0.qualifier to bundle.source 0.0.0");
        }
    }

}
