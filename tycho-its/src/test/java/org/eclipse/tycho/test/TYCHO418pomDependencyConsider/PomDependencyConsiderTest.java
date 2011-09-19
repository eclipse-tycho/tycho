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
package org.eclipse.tycho.test.TYCHO418pomDependencyConsider;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class PomDependencyConsiderTest extends AbstractTychoIntegrationTest {
    @Test
    public void testPomDependenciesConsider() throws Exception {
        Verifier verifier = getVerifier("/TYCHO418pomDependencyConsider/artifact");
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        verifier = getVerifier("/TYCHO418pomDependencyConsider", false);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());

        Assert.assertTrue(new File(basedir,
                "site/target/site/plugins/TYCHO418pomDependencyConsider.artifact_0.0.1.SNAPSHOT.jar").canRead());
    }

}
