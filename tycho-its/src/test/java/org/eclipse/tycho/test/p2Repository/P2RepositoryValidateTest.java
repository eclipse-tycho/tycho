/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class P2RepositoryValidateTest extends AbstractTychoIntegrationTest {

    @Test
    public void testValidate() throws Exception {
        Verifier verifier = getVerifier("p2Repository.unresolvableIU", false);
        verifier.getSystemProperties().put("test-data-repo", P2Repositories.ECLIPSE_352.toString());
        try {
            verifier.executeGoal("validate");
            fail("Expected build failure");
        } catch (VerificationException ex) {
            // expected
        }
        verifier.verifyTextInLog("non.existing.iu");
    }

}
