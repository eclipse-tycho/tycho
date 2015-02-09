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
package org.eclipse.tycho.test.p2Repository;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryWithIUTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIUInRepo() throws Exception {
        Verifier verifier = getVerifier("p2Repository.iu", false);

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File artifactInRepo = new File(verifier.getBasedir(),
                "repository/target/repository/features/prr.example.feature_1.2.0.jar");
        Assert.assertTrue(artifactInRepo.exists());
    }
}
