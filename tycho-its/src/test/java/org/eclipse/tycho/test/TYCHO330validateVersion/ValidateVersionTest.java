/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO330validateVersion;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
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
