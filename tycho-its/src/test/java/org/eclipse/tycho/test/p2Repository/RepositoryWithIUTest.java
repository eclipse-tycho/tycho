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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class RepositoryWithIUTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIUInRepo() throws Exception {
        Verifier verifier = getVerifier("p2Repository.iu", false);
        verifier.getSystemProperties().setProperty("test-data-repo", P2Repositories.ECLIPSE_KEPLER.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(testProjectRoot, "repository"));
        IU iu = p2Repo.getUniqueIU("Test Category");
        assertThat(iu.getRequiredIds(), hasItem("sample-product"));
    }

    @Test
    public void testIUMatchSyntaxInRepo() throws Exception {
        Verifier verifier = getVerifier("p2Repository.iu.match", false);
        verifier.getSystemProperties().setProperty("test-data-repo",  P2Repositories.ECLIPSE_KEPLER.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        String testProjectRoot = verifier.getBasedir();
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(testProjectRoot, "repository"));
        IU iu = p2Repo.getUniqueIU("Test Category");
        assertThat(iu.getRequiredIds(), hasItem("sample-product"));
    }
}
