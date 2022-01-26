/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jan Sievers - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.utils.io.FileUtils;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;

public class MirrorMojoTest extends AbstractTychoMojoTestCase {

    private File mirrorDestinationDir;
    private Mojo mirrorMojo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File basedir = getBasedir("mirroring/testProject");
        List<MavenProject> projects = getSortedProjects(basedir);
        MavenProject project = projects.get(0);
        initLegacySupport(projects, project);
        mirrorDestinationDir = new File(project.getFile().getParent(), "target/repository").getCanonicalFile();
        FileUtils.deleteDirectory(mirrorDestinationDir);
        mirrorMojo = lookupMojo("mirror", project.getFile());
        setVariableValueToObject(mirrorMojo, "destination", mirrorDestinationDir);
        setVariableValueToObject(mirrorMojo, "project", project);
    }

    @Override
    protected void tearDown() throws Exception {
        // this is needed because the DefaultEquinoxEmbedder plexus component 
        // is not disposed
        EclipseStarter.shutdown();
        System.clearProperty("org.osgi.framework.vendor");
    }

    public void testMirrorFromOldStyleUpdatesite() throws Exception {
        File sourceRepository = new File("src/test/resources/mirroring/sourceUpdatesite").getCanonicalFile();
        setVariableValueToObject(mirrorMojo, "source",
                Collections.singletonList(new Repository(sourceRepository.toURI())));
        mirrorMojo.execute();
        assertTrue(mirrorDestinationDir.isDirectory());
        assertEquals(1, new File(mirrorDestinationDir, "plugins").listFiles().length);
        assertMirroredBundle(mirrorDestinationDir, "testbundle", "1.0.0");
        assertMirroredFeature(mirrorDestinationDir, "testfeature", "1.0.0");
    }

    public void testMirrorSpecificIUFromP2Repo() throws Exception {
        File sourceRepository = new File("src/test/resources/mirroring/sourceP2Repo").getCanonicalFile();
        setVariableValueToObject(mirrorMojo, "source",
                Collections.singletonList(new Repository(sourceRepository.toURI())));
        Iu testBundleIu = new Iu();
        testBundleIu.id = "test.bundle1";
        setVariableValueToObject(mirrorMojo, "ius", Collections.singletonList(testBundleIu));
        mirrorMojo.execute();
        assertTrue(mirrorDestinationDir.isDirectory());
        assertEquals(1, new File(mirrorDestinationDir, "plugins").listFiles().length);
        assertMirroredBundle(mirrorDestinationDir, "test.bundle1", "1.0.0.201108100850");
    }

    public void testMirrorProduct() throws Exception {
        File sourceRepository = new File("src/test/resources/mirroring/sourceP2RepoWithProduct").getCanonicalFile();
        setVariableValueToObject(mirrorMojo, "source",
                Collections.singletonList(new Repository(sourceRepository.toURI())));
        Iu testBundleIu = new Iu();
        testBundleIu.id = "dummy";
        setVariableValueToObject(mirrorMojo, "ius", Collections.singletonList(testBundleIu));
        mirrorMojo.execute();
        assertTrue(mirrorDestinationDir.isDirectory());
        assertTrue(Arrays.stream(new File(mirrorDestinationDir, "binary").listFiles()).map(File::getName)
                .anyMatch(name -> name.startsWith("dummy")));
    }

    public void testMirrorWithPlatformFilter() throws Exception {
        File sourceRepository = new File("src/test/resources/mirroring/sourceP2Repo").getCanonicalFile();
        setVariableValueToObject(mirrorMojo, "source",
                Collections.singletonList(new Repository(sourceRepository.toURI())));
        Iu featureIU = new Iu();
        featureIU.id = "test.feature.feature.group";
        setVariableValueToObject(mirrorMojo, "ius", Collections.singletonList(featureIU));
        Map<String, String> filter = new HashMap<>();
        filter.put("osgi.os", "linux");
        filter.put("osgi.ws", "gtk");
        filter.put("osgi.arch", "x86_64");
        setVariableValueToObject(mirrorMojo, "filter", filter);
        mirrorMojo.execute();
        assertTrue(mirrorDestinationDir.isDirectory());
        // win32 fragment must not mirrored because platform filter does not match
        assertEquals(2, new File(mirrorDestinationDir, "plugins").listFiles().length);
        assertMirroredBundle(mirrorDestinationDir, "test.bundle1", "1.0.0.201108100850");
        assertMirroredBundle(mirrorDestinationDir, "test.bundle2", "1.0.0.201108100850");
    }

    private static void assertMirroredBundle(File publishedContentDir, String bundleID, String version) {
        assertMirroredArtifact(publishedContentDir, bundleID, version, "plugins");
    }

    private static void assertMirroredFeature(File publishedContentDir, String featureID, String version) {
        assertMirroredArtifact(publishedContentDir, featureID, version, "features");
    }

    private static void assertMirroredArtifact(File publishedContentDir, String id, String version, String folder) {
        String pluginArtifactNamePrefix = id + "_" + version; // without qualifier
        for (File bundle : new File(publishedContentDir, folder).listFiles()) {
            if (bundle.getName().startsWith(pluginArtifactNamePrefix))
                return;
        }

        Assert.fail("Published artifact not found: " + pluginArtifactNamePrefix);
    }

    private void initLegacySupport(List<MavenProject> projects, MavenProject currentProject) throws Exception {
        MavenSession session = newMavenSession(currentProject, projects);
        LegacySupport buildContext = lookup(LegacySupport.class);
        buildContext.setSession(session);
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
        try (InputStream is = this.getClassLoader().getResourceAsStream("baseTest.properties")) {
            buildProperties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new File(buildProperties.getProperty("local-repo"));
    }
}
