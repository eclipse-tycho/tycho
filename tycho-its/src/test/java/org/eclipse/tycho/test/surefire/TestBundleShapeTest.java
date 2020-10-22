/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestBundleShapeTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        // TYCHO-460
        Verifier verifier = getVerifier("surefire.bundleResources");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }
}
