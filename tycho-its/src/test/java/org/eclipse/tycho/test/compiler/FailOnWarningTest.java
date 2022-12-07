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

package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.fail;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FailOnWarningTest extends AbstractTychoIntegrationTest {

    @Test
    public void testCompilerFailOnWarning() throws Exception {
        Verifier verifier = getVerifier("compiler.failOnWarning", false);
        try {
            verifier.executeGoal("compile");
            fail();
        } catch (VerificationException e) {
            // expected 
            verifier.verifyTextInLog("The value of the local variable a is not used");
            verifier.verifyTextInLog("1 problem (1 warning)");
            verifier.verifyTextInLog("error: warnings found and -failOnWarning specified");
        }
    }
}
