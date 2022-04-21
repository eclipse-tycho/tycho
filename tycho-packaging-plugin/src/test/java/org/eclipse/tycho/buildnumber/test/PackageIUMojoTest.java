/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildnumber.test;

import static org.eclipse.tycho.test.util.ArchiveContentUtil.getFilesInZip;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.model.IU;
import org.eclipse.tycho.packaging.PackageIUMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

import de.pdark.decentxml.Element;

public class PackageIUMojoTest extends AbstractTychoMojoTestCase {

    public void testThatArtifactPayloadIsCorrect() throws Exception {
        File basedir = getBasedir("projects/iuWithPayload/");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "iuWithPayload");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageIUMojo mojo = (PackageIUMojo) lookupConfiguredMojo(project, "package-iu");
        setVariableValueToObject(mojo, "artifactContentFolder",
                new File(basedir, "src/main/resources").getAbsolutePath());
        mojo.execute();

		assertTrue(getFilesInZip(new File(basedir, "target/iuWithPayload-1.0.0.zip")).contains("file.txt"));
    }

    public void testArtifactWithoutPayload() throws Exception {
        File basedir = getBasedir("projects/iuWithoutPayload");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "iuWithoutPayload");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageIUMojo mojo = (PackageIUMojo) lookupConfiguredMojo(project, "package-iu");
        mojo.execute();

        IU iu = IU.loadIU(new File(basedir, "target"));
        Element artifact = iu.getSelfArtifact();
		assertNull(artifact);

		assertTrue(new File(basedir, "target/iuWithoutPayload-1.0.0.zip").exists());
    }

    public void testInjectArtifactReference() throws Exception {
        File basedir = getBasedir("projects/iuWithPayloadButNoArtifactReference");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "iuWithPayloadButNoArtifactReference");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageIUMojo mojo = (PackageIUMojo) lookupConfiguredMojo(project, "package-iu");
        setVariableValueToObject(mojo, "artifactContentFolder",
                new File(basedir, "src/main/resources").getAbsolutePath());
        mojo.execute();

        IU iu = IU.loadIU(new File(basedir, "target"));
        Element artifact = iu.getSelfArtifact();
        assertNotNull(artifact);
		assertEquals("binary", artifact.getAttributeValue("classifier"));
		assertEquals("iuWithPayloadButNoArtifactReference", artifact.getAttributeValue("id"));
		assertEquals("1.0.0", artifact.getAttributeValue("version"));
    }
}
