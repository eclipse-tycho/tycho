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
package org.eclipse.tycho.test.product;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JustJTest extends AbstractTychoIntegrationTest {
    @Test
    public void testJustJ() throws Exception {
        Verifier verifier = getVerifier("justj/multi", true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testBundleWithJustJ() throws Exception {
        Verifier verifier = getVerifier("justj/bundle", true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testProductWithJustJ() throws Exception {
        Verifier verifier = getVerifier("justj/product", true);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
