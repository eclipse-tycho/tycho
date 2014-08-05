/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.iu;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryWithIUTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIUInRepo() throws Exception {
        Verifier verifier = getVerifier("iu.repo", false);

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File artifactInRepo = new File(verifier.getBasedir(), "repository/target/repository/binary/anIU_1.0.0");
        Assert.assertTrue(artifactInRepo.exists());
    }
}
