/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0432configurableFailIfNoTests;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class FailIfNoTestsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testNoTestsNoFailure() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0432configurableFailIfNoTests");
        verifier.getSystemProperties().setProperty("failIfNoTests", "false");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testNoTestsFailureDefaultCase() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0432configurableFailIfNoTests");
        try {
            verifier.executeGoal("integration-test");
            Assert.fail();
        } catch (VerificationException e) {
            // expected
        }
        verifier.verifyTextInLog("No tests found");
    }

}
