/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

    private static final String INCLUDED_PLUGIN_ID = "org.eclipse.osgi.source";
    private static final String INCLUDED_PLUGIN_VERSION = "3.4.3.R34x_v20081215-1030";

    private static final String CUSTOM_FINAL_NAME = "testrepo-myqualifier";

    private static Verifier verifier;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void executeBuild() throws Exception {
        verifier = new BasicP2RepositoryIntegrationTest().getVerifier("/p2Repository", false);
        verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void test381377BundleInclusion() {
        P2RepositoryTool p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));

        // check that (separately!) included bundle is there
        assertThat(p2Repo.getBundleArtifact(INCLUDED_PLUGIN_ID, INCLUDED_PLUGIN_VERSION), isFile());
    }

    @Test
    public void test347416CustomFinalName() throws Exception {
        File repositoryArchive = new File(verifier.getBasedir(), "target/" + CUSTOM_FINAL_NAME + ".zip");
        assertThat(repositoryArchive, isFile());
    }
}
