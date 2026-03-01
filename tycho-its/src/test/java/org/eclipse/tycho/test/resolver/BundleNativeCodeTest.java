/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class BundleNativeCodeTest extends AbstractTychoIntegrationTest {

    @Test
    public void testBundleNativeCode() throws Exception {
        Verifier verifier = getVerifier("/resolver.nativeCode");
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
    }
}
