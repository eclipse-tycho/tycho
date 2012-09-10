/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
