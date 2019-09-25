/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class EnableAssertionsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testEnableAssertions() throws Exception {
        Verifier verifier = getVerifier("surefire.enableAssertions");
        try {
            verifier.executeGoal("verify");
        } catch (VerificationException ve) {
            // expected
        }
        verifier.verifyTextInLog("There are test failures");
        verifier.verifyTextInLog("java.lang.AssertionError");
    }
}
