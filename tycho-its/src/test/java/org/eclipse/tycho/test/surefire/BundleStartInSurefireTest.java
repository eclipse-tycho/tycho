/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.shared.verifier.Verifier;
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
