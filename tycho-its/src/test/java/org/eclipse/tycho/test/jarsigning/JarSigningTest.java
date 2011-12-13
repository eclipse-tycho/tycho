/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.jarsigning;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JarSigningTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSigning() throws Exception {
        Verifier verifier = getVerifier("jar-signing", false);

        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("verified successfully");
    }
}
