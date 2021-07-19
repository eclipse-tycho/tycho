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
 *    Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ProductMixedVersionsTest extends AbstractTychoIntegrationTest {
    @Test
    public void testMixedPluginVersions() throws Exception {
        Verifier verifier = getVerifier("product.differentVersions", false);
        verifier.getSystemProperties().setProperty("platform-url", P2Repositories.ECLIPSE_LATEST.toString());
        verifier.executeGoals(Arrays.asList("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }
}
