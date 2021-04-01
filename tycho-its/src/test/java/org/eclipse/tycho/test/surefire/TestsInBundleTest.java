/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestsInBundleTest extends AbstractTychoIntegrationTest {

    @Test
    public void testCompile() throws Exception {

        // a eclipse-plugin with test classes
        Verifier verifier = getVerifier("surefire.combinedtests/bundle.test");

        verifier.executeGoal("test-compile");
        verifier.verifyErrorFreeLog();

        assertTrue("compiled class file do not exists",
                new File(verifier.getBasedir(), "target/classes/bundle/test/Counter.class").exists());
        assertTrue("compiled test-class file do not exists",
                new File(verifier.getBasedir(), "target/test-classes/bundle/test/AdderTest.class").exists());
    }

}
