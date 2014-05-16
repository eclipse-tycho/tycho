/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.fail;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class RequireJREPackagesImportTest extends AbstractTychoIntegrationTest {

    @Test
    public void testStrictImportJREPackages() throws Exception {
        Verifier verifier = getVerifier("compiler.requireJREPackageImports", false);
        try {
            verifier.executeGoal("compile");
            fail();
        } catch (VerificationException e) {
            // expected
            verifier.verifyTextInLog("[ERROR] Access restriction: The type Context is not accessible due to restriction on classpath entry");
        }
    }
}
