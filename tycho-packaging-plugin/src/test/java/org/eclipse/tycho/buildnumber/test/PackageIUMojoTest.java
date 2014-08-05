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
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
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

        PackageIUMojo mojo = (PackageIUMojo) lookupMojo("package-iu", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);

        mojo.execute();

        ZipFile zip = new ZipFile(new File(basedir, "target/iuWithPayload-1.0.0.zip"));
        try {
            ZipEntry entry = zip.getEntry("file.txt");
            assertNotNull("Missing file in artifact", entry);
        } finally {
            zip.close();
        }
    }

    public void testArtifactWithoutPayload() throws Exception {
        File basedir = getBasedir("projects/iuWithoutPayload");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "iuWithoutPayload");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageIUMojo mojo = (PackageIUMojo) lookupMojo("package-iu", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);

        mojo.execute();

        IU iu = IU.loadIU(new File(basedir, "target"));
        Element artifact = iu.getSelfArtifact();
        assertNull(artifact);

        ZipFile zip = new ZipFile(new File(basedir, "target/iuWithoutPayload-1.0.0.zip"));
        try {
            ZipEntry entry = zip.getEntry("emptyArtifact.xml");
            assertNotNull("emptyArtifact marker file expected", entry);
        } finally {
            zip.close();
        }
    }

    public void testInjectArtifactReference() throws Exception {
        File basedir = getBasedir("projects/iuWithPayloadButNoArtifactReference");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "iuWithPayloadButNoArtifactReference");
        MavenSession session = newMavenSession(project, projects);
        // set build qualifier
        lookupMojoWithDefaultConfiguration(project, session, "build-qualifier").execute();

        PackageIUMojo mojo = (PackageIUMojo) lookupMojo("package-iu", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", session);

        mojo.execute();

        IU iu = IU.loadIU(new File(basedir, "target"));
        Element artifact = iu.getSelfArtifact();
        assertNotNull(artifact);
        assertEquals("binary", artifact.getAttributeValue("classifier"));
        assertEquals("iuWithPayloadButNoArtifactReference", artifact.getAttributeValue("id"));
        assertEquals("1.0.0", artifact.getAttributeValue("version"));
    }
}
