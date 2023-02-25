/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Manumitting Technologies Inc - adapated for trimStackTrace (bug 535881)
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TrimStackTrace extends AbstractTychoIntegrationTest {

    @Test
    public void testTrimStackTraceFalse() throws Exception {
        Verifier verifier = getVerifier("surefire.trimstacktrace");
        try {
            verifier.executeGoal("integration-test");
            Assert.fail();
        } catch (VerificationException e) {
            // expected
            verifier.verifyTextInLog("org.junit.Assert.fail");
        }

    }

}
