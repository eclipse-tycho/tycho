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

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExtraTestApplicationArgumentsTest extends AbstractTychoIntegrationTest {
    @Test
    public void exportProduct() throws Exception {
        // project that passes extra arguments to the test application -> requested in TYCHO-290
        Verifier verifier = getVerifier("surefire.appArgs");
        verifier.executeGoal("integration-test");

        // project contains a test doing the assertions
        verifier.verifyErrorFreeLog();
    }
}
