/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test;

import org.apache.maven.shared.verifier.Verifier;
import org.junit.Test;

public class MissingPluginVersionsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testMissingPluginVersion() throws Exception {
        Verifier verifier = getVerifier("missingPluginVersions", false);
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
    }
}
