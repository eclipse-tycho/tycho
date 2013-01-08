/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.junit.Test;

public class MissingPluginVersionsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testMissingPluginVersion() throws Exception {
        Verifier verifier = getVerifier("missingPluginVersions");
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
    }
}
