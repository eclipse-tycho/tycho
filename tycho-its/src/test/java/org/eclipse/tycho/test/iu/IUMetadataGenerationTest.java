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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.junit.Assert;
import org.junit.Test;

public class IUMetadataGenerationTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIUWithArtifact() throws Exception {
        Verifier verifier = getVerifier("iu.artifact", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        File repoProject = new File(verifier.getBasedir(), "repository");
        P2RepositoryTool repo = P2RepositoryTool.forEclipseRepositoryModule(repoProject);
        IU finalIU = repo.getUniqueIU("iuA");

        //Here we check that the final IU contained in the repo has the right shape
        assertThat(finalIU.getProvidedCapabilities(), hasItem("org.eclipse.equinox.p2.iu/iuA/1.0.0"));
        assertMavenProperties(finalIU, "iu.artifact", "iuA", finalIU.getVersion());
        assertThat(finalIU.getArtifacts(), hasItem("binary/iuA/1.0.0"));

        //check that the artifact is here
        Assert.assertTrue(new File(repoProject, "target/repository/binary/iuA_1.0.0").exists());
    }

    private void assertMavenProperties(IU iu, String groupId, String artifactId, String iuVersion) throws Exception {
        List<String> properties = iu.getProperties();
        assertThat(properties, hasItem("maven-groupId=" + groupId));
        assertThat(properties, hasItem("maven-artifactId=" + artifactId));
        assertThat(properties, hasItem("maven-version=" + iuVersion));
    }

    @Test
    public void testIUWithoutArtifact() throws Exception {
        Verifier verifier = getVerifier("iu.withoutArtifact", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();

        P2RepositoryTool repo = P2RepositoryTool
                .forEclipseRepositoryModule(new File(verifier.getBasedir(), "repository"));
        IU finalIU = repo.getUniqueIU("iuA");

        assertThat(finalIU.getProvidedCapabilities(), hasItem("org.eclipse.equinox.p2.iu/iuA/1.0.0"));
        assertThat(finalIU.getArtifacts().size(), is(0));
    }
}
