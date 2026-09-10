/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * A blank/empty value in a shared {@code bndRunProperties} configuration (e.g. a placeholder
 * meant to be overridden by a specific module) must not cause the {@code bnd-test} goal to fail
 * with a NullPointerException.
 */
public class BndRunPropertiesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testBlankBndRunPropertiesValueIsIgnored() throws Exception {
        Verifier verifier = getVerifier("surefire.bndRunProperties");
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
