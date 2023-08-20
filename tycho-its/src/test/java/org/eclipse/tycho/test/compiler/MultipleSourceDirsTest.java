/*******************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class MultipleSourceDirsTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("compliler.multipleSourceDirs", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        verifyTextNotInLog(verifier, "'build.plugins.plugin.version' for org.codehaus.mojo:build-helper-maven-plugin is missing");
    }
}
