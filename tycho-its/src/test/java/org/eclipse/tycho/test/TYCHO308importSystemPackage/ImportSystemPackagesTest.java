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
package org.eclipse.tycho.test.TYCHO308importSystemPackage;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ImportSystemPackagesTest extends AbstractTychoIntegrationTest {
    @Test
    public void testLocalInstall() throws Exception {
        Verifier verifier = getVerifier("/TYCHO308importSystemPackage/local_install", false);
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testP2Repository() throws Exception {
        Verifier verifier = getVerifier("/TYCHO308importSystemPackage/p2_repository", false);
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }
}
