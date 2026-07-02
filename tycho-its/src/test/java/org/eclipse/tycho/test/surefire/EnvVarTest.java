/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class EnvVarTest extends AbstractTychoIntegrationTest {

    @Test
    public void testEnvironmentVariablesInheritance() throws Exception {
        Verifier verifier = getVerifier("surefire.envVars");
        Map<String, String> env = new HashMap<>();
        env.put("KEY_1", "value_1");
        verifier.executeGoal("integration-test", env);
        // project contains a test doing the assertions
        verifier.verifyErrorFreeLog();
    }
}
