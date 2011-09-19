/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0404pomDependencyConsiderExtraClasspath;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomDependencyConsiderExtraClasspathTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0404pomDependencyConsiderExtraClasspath", false);
        FileUtils.deleteDirectory(new File(verifier.localRepo, "TYCHO0404pomDependencyConsiderExtraClasspath"));
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

}
