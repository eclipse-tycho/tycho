/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.IOUtil;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class PublishFeaturesAndBundlesMojoTest extends AbstractTychoMojoTestCase {

    public void testPublisher() throws Exception {
        File basedir = getBasedir("publisher/testProject");
        List<MavenProject> projects = getSortedProjects(basedir, null);
        MavenProject project = projects.get(0);

        initLegacySupport(projects, project);

        // simulate that content to be published has already been extracted to the target folder
        File sourceRepositoryDir = new File(project.getFile().getParent(), "target/sourceRepository").getAbsoluteFile();
        generateContentToBePublished(sourceRepositoryDir);

        File publishedContentDir = new File(project.getFile().getParent(), "target/repository").getAbsoluteFile();

        // call publisher mojo
        Mojo publishMojo = lookupMojo("publish-features-and-bundles", project.getFile());
        setVariableValueToObject(publishMojo, "project", project);
        setVariableValueToObject(publishMojo, "sourceLocation", sourceRepositoryDir.toString());
        setVariableValueToObject(publishMojo, "artifactRepositoryLocation", publishedContentDir.toString());
        setVariableValueToObject(publishMojo, "metadataRepositoryLocation", publishedContentDir.toString());
        setVariableValueToObject(publishMojo, "publishArtifacts", Boolean.TRUE);

        publishMojo.execute();

        assertPublishedIU(publishedContentDir, "org.eclipse.tycho.extras.testdata");
        assertPublishedArtifact(publishedContentDir, "org.eclipse.tycho.extras.testdata", "1.0.0");
    }

    private static void assertPublishedArtifact(File publishedContentDir, String bundleID, String version) {
        String pluginArtifactNamePrefix = bundleID + "_" + version; // without qualifier
        for (File bundle : new File(publishedContentDir, "plugins").listFiles()) {
            if (bundle.getName().startsWith(pluginArtifactNamePrefix))
                return;
        }

        Assert.fail("Published artifact not found: " + pluginArtifactNamePrefix);
    }

    private static void assertPublishedIU(File publishedContentDir, String iuID) throws IOException {
        XMLParser parser = new XMLParser();
        Document document = parser.parse(new XMLIOSource(new File(publishedContentDir, "content.xml")));
        Element unitElement = document.getChild("repository/units");
        List<Element> children = unitElement.getChildren("unit");
        for (Element element : children) {
            if (iuID.equals(element.getAttribute("id").getValue())) {
                return;
            }
        }
        Assert.fail("IU not found: " + iuID);
    }

    private void initLegacySupport(List<MavenProject> projects, MavenProject currentProject) throws Exception {
        MavenSession session = newMavenSession(currentProject, projects);
        System.out.println(session.getLocalRepository());
        LegacySupport buildContext = lookup(LegacySupport.class);
        buildContext.setSession(session);
    }

    private void generateContentToBePublished(File repositoryFolder) throws IOException {
        String bundleFileName = "testdata-1.0.0-SNAPSHOT.jar";
        URL source = getClassLoader().getResource(bundleFileName);
        FileUtils.copyURLToFile(source, new File(repositoryFolder, "plugins/" + bundleFileName));
    }

    // use the normal local Maven repository (called by newMavenSession)
    @Override
    protected ArtifactRepository getLocalRepository() throws Exception {
        RepositorySystem repoSystem = lookup(RepositorySystem.class);
        File path = getLocalMavenRepository().getCanonicalFile();
        ArtifactRepository r = repoSystem.createLocalRepository(path);
        return r;
    }

    private File getLocalMavenRepository() {
        /*
         * The build (more specifically, the maven-properties-plugin) writes the local Maven
         * repository location to a file. Here, we read this file. (Approach copied from tycho-its.)
         */
        Properties buildProperties = new Properties();
        InputStream is = this.getClassLoader().getResourceAsStream("baseTest.properties");
        try {
            buildProperties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.close(is);
        }
        return new File(buildProperties.getProperty("local-repo"));
    }
}
