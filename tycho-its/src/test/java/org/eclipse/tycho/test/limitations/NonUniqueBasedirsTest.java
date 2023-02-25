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
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.limitations;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class NonUniqueBasedirsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testNonUniqueBasedirFailure() throws Exception {
        Verifier verifier = getVerifier("limitations.uniqueBaseDirs", false);
        try {
            verifier.executeGoal("clean");
            Assert.fail("build failure expected");
        } catch (VerificationException e) {
            // expected
        }

        // expect a clear error message -> requested in bug 366967
        verifier.verifyTextInLog("Multiple modules within the same basedir are not supported");
    }

}
