/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.limitations;

import static org.junit.Assert.fail;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MixedTychoVersionsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSeveralTychoVersionsConfigured() throws Exception {
        Verifier verifier = getVerifier("limitations.tychoVersions", false);
        try {
            verifier.executeGoal("compile");
            fail();
        } catch (VerificationException e) {
            // expected
            verifier.verifyTextInLog("[ERROR] Several versions of Tycho plugins are configured [0.13.0, 0.14.0, "
                    + TychoVersion.getTychoVersion() + "]:");
        }
    }

}
