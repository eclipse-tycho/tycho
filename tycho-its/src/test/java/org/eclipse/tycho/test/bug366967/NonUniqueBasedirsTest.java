/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug366967;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class NonUniqueBasedirsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testNonUniqueBasedirFailure() throws Exception {
        Verifier verifier = getVerifier("/nonUniqueModuleBaseDir", false);
        try {
            verifier.executeGoal("clean");
            Assert.fail("build failure expected");
        } catch (VerificationException e) {
            // expected
        }
        verifier.verifyTextInLog("Multiple modules within the same basedir are not supported");
    }

}
