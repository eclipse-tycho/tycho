/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.eeProfile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FragmentsAttachedTest extends AbstractTychoIntegrationTest {

    @Test
    public void testSWTFragmentsAttached() throws Exception {
        Verifier verifier = getVerifier("eeProfile.resolution.fragments", false);

        verifier.executeGoal("verify");

        // with bug 413116, compile would fail
        verifier.verifyErrorFreeLog();
    }

}
