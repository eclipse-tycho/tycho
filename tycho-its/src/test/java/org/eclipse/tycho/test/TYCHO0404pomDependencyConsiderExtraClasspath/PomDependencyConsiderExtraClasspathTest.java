/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0404pomDependencyConsiderExtraClasspath;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomDependencyConsiderExtraClasspathTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0404pomDependencyConsiderExtraClasspath", false);
        FileUtils.deleteDirectory(
                new File(verifier.getLocalRepository(), "TYCHO0404pomDependencyConsiderExtraClasspath"));
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
