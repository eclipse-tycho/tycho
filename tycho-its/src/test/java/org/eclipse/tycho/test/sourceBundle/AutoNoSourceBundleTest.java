/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.sourceBundle;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class AutoNoSourceBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        // the point of the test is to make sure Tycho does NOT allow references to missing source bundles (see bug 367637)

        Verifier verifier = getVerifier("/sourceBundle.autoSkip", false);
        try {
            verifier.executeGoal("verify");
            Assert.fail("Reference to a missing source bundle did not fail the build");
        } catch (VerificationException expected) {
            verifier.verifyTextInLog("feature.feature.group 1.0.0.qualifier requires 'org.eclipse.equinox.p2.iu; bundle.source 0.0.0' but it could not be found");
        }
    }

}
