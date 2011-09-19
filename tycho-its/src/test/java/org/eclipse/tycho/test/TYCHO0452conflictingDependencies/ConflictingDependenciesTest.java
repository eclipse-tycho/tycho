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
package org.eclipse.tycho.test.TYCHO0452conflictingDependencies;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ConflictingDependenciesTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/TYCHO0452conflictingDependencies", false);
        verifier.getCliOptions().add("-De342-p2.repo=" + toURI(new File("repositories/e342")));
        verifier.getCliOptions().add("-De352-p2.repo=" + toURI(new File("repositories/e352")));
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        assertFileExists(basedir, "site/target/site/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar");
        assertFileExists(basedir, "site/target/site/plugins/org.eclipse.osgi_3.5.2.R35x_v20100126.jar");
    }

}
