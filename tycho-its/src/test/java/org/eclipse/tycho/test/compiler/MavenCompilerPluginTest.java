/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class MavenCompilerPluginTest extends AbstractTychoIntegrationTest {

    @Test
    public void testJDTCompilerId() throws Exception {
        Verifier verifier = getVerifier("compiler.mavenCompilerPlugin", false);
        try {
            verifier.executeGoal("compile");
            Assert.fail();
        } catch (VerificationException e) {
            // expected
            verifier.verifyTextInLog("field Foo.unused is not used");
        }
    }

}
