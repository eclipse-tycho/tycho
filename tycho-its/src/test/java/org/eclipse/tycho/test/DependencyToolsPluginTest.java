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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.version.TychoVersion;
import org.junit.Test;

public class DependencyToolsPluginTest extends AbstractTychoIntegrationTest {

    @Test
    public void testUsage() throws Exception {
        Verifier verifier = getVerifier("dependency-tools.usage", false, true);
        // First build the project to ensure dependencies are resolved
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        
        // Now execute the usage goal via CLI
        verifier.executeGoal("org.eclipse.tycho.extras:tycho-dependency-tools-plugin:" 
                + TychoVersion.getTychoVersion() + ":usage");
        verifier.verifyErrorFreeLog();
        
        // Verify the log contains expected output
        verifier.verifyTextInLog("Scan reactor for dependencies...");
        verifier.verifyTextInLog("DEPENDENCIES USAGE REPORT");
        verifier.verifyTextInLog("Target:");
        
        // Verify that unit status is shown
        verifier.verifyTextInLog("USED");
    }
}
