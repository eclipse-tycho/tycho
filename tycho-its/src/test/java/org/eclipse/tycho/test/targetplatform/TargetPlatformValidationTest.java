/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.targetplatform;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TargetPlatformValidationTest extends AbstractTychoIntegrationTest {

    @Test
    public void testValidationOK() throws Exception {
        Verifier verifier = getVerifier("/target-platform/validateOK", false);
        verifier.executeGoal("validate");
    }

    @Test
    public void testValidationKO() throws Exception {
        Verifier verifier = getVerifier("/target-platform/validateKO", false);
        try {
            verifier.executeGoal("validate");
            Assert.fail("Validation should have failed on unresolved target-platform");
        } catch (Exception ex) {
            // Normal behavior
        }
    }

    @Test
    public void testSuccessiveValidations() throws Exception {
        Verifier verifier = getVerifier("/target-platform/validate_OK_KO_OK_KO", false);
        try {
            verifier.executeGoal("validate");
            Assert.fail("Validation should have failed on unresolved target-platform");
        } catch (Exception ex) {
            // Normal behavior
        }
    }
}
