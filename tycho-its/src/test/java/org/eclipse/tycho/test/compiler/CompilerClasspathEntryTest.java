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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompilerClasspathEntryTest extends AbstractTychoIntegrationTest {

    @Test
    public void testJUnit4Container() throws Exception {
        Verifier verifier = getVerifier("compiler.junitcontainer/junit4-in-bundle", true);
        verifier.executeGoal("test");
        verifier.verifyErrorFreeLog();
    }

    public void testLibEntry() throws Exception {
        Verifier verifier = getVerifier("compiler.libentry/my.bundle", false);
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
    }
}
