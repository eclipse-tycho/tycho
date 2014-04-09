/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class EnvVarTest extends AbstractTychoIntegrationTest {

    @Test
    public void testEnvironmentVariablesInheritance() throws Exception {
        Verifier verifier = getVerifier("surefire.envVars");
        Map<String, String> env = new HashMap<String, String>();
        env.put("KEY_1", "value_1");
        verifier.executeGoal("integration-test", env);
        // project contains a test doing the assertions
        verifier.verifyErrorFreeLog();
    }
}
