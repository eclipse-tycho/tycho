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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TargetPlatformPackagingTest extends AbstractTychoIntegrationTest {

    @Test
    public void testPackaging_ValidationOK() throws Exception {
        Verifier verifier = getVerifier("/target-platform/packagingOK", false);
        verifier.executeGoal("validate");
    }

    @Test
    public void testPackaging_ValidationKO() throws Exception {
        Verifier verifier = getVerifier("/target-platform/packagingKO", false);
        try {
            verifier.executeGoal("validate");
            Assert.fail("Validation should have failed on unresolved target-platform");
        } catch (Exception ex) {
            // Normal behavior: Expected failing build because of validation
        }
    }

    @Test
    public void testPackaging_ValidationSkipped() throws Exception {
        Verifier verifier = getVerifier("/target-platform/packagingKO", false);
        Map<String, String> args = new HashMap<String, String>();
        args.put("tycho.target-platform-plugin.validation.skip", Boolean.TRUE.toString());
        verifier.executeGoal("validate");
        // Since TP validation is skipped, everything should perform well
    }
}
