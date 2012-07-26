/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class SurefireParametersTest extends AbstractTychoIntegrationTest {

    @Test
    public void testTrimStackTrace_On() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit47/trimStackTrace");
        verifier.getSystemProperties().setProperty("trimStackTrace", Boolean.TRUE.toString());
        try {
            verifier.executeGoal("integration-test");
            Assert.fail("Test should have failed");
        } catch (Exception ex) {
            try {
                verifier.verifyTextInLog("stackMethod");
                Assert.fail("trimStackTrace=true => No stack expected");
            } catch (Exception ex2) {
                // Expected behavior
            }
        }
    }

    @Test
    public void testTrimStackTrace_Off() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit47/trimStackTrace");
        verifier.getSystemProperties().setProperty("trimStackTrace", Boolean.FALSE.toString());
        try {
            verifier.executeGoal("integration-test");
            Assert.fail("Test should have failed");
        } catch (Exception ex) {
            try {
                verifier.verifyTextInLog("stackMethod");
            } catch (Exception ex2) {
                Assert.fail("trimStackTrace=false => stack expected");
            }
        }
    }

}
