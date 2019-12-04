/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class BundleStartInSurefireTest extends AbstractTychoIntegrationTest {

    // requested in TYCHO-373
    @Test
    public void implicitDSAutostart() throws Exception {
        Verifier verifier = getVerifier("surefire.bundleStart/implicit/ds.test");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void explicitBundleStartLevel() throws Exception {
        // bundle and test with .qualifier versions -> regression test for TYCHO-170
        Verifier verifier = getVerifier("surefire.bundleStart/explicit");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
