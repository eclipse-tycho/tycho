/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Rapicorp, Inc. - support <iu> syntax in category.xml (371983)
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

public class RepoRefLocationP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

    private static Verifier verifier;
    private static P2RepositoryTool p2Repo;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void executeBuild() throws Exception {
        System.setProperty("verifier.forkMode", "auto");
        verifier = new RepoRefLocationP2RepositoryIntegrationTest().getVerifier("/p2Repository.repositoryRef.location",
                false);
        verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
    }

    @Test
    public void testRefLocation() throws Exception {
        File repository = new File(verifier.getBasedir(), "target/repository");
        assertThat(new File(repository, "content.xml.xz"), isFile());
        assertThat(new File(repository, "artifacts.xml.xz"), isFile());
        assertThat(new File(repository, "p2.index"), isFile());
        assertThat(new File(repository, "category.xml"), isFile());
    }

}
