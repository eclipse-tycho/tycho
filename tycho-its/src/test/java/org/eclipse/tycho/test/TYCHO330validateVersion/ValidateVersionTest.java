/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO330validateVersion;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class ValidateVersionTest extends AbstractTychoIntegrationTest {

    @Test
    public void testPlugin() throws Exception {
        Verifier verifier = getVerifier("/TYCHO330validateVersion/bundle", false);
        try {
            verifier.executeGoal("verify");
            Assert.fail();
        } catch (VerificationException e) {
            // good enough for now
        }

    }

    @Test
    public void testFeature() throws Exception {
        Verifier verifier = getVerifier("/TYCHO330validateVersion/feature", false);
        try {
            verifier.executeGoal("verify");
            Assert.fail();
        } catch (VerificationException e) {
            // good enough for now
        }
    }
}
