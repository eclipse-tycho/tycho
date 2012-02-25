/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdviceTest {
    static final String GLOBAL_SPEC = "";

    static final String WINDOWS_SPEC_FOR_PROPERTIES_KEY = "win32.win32.x86";

    static final String WINDOWS_SPEC_FOR_ADVICE = "win32.win32.x86";

    static final String LINUX_SPEC_FOR_PROPERTIES_KEY = "linux.gtk.x86";

    static final String LINUX_SPEC_FOR_ADVICE = "gtk.linux.x86";

    private static final String RESOURCES_ROOTFILES_REL_PATH = "resources/rootfiles";

    public static final File FEATURE_PROJECT_TEST_RESOURCE_ROOT = new File(RESOURCES_ROOTFILES_REL_PATH
            + "/feature-project");

    private static final String FEATURE_JAR_REL_PATH = RESOURCES_ROOTFILES_REL_PATH
            + "/feature-project/target/feature-0.0.1-SNAPSHOT.jar";

    private static final String GROUP_ID = "group";

    private static final String ARTIFACT_ID = "feature";

    private static final String VERSION = "0.0.1-SNAPSHOT";

    private static final String PACKAGING_TYPE = "eclipse-feature";

    private static final String DEFAULT_ARTIFACT_ID = "artifact.for.which.advice.applies";

    // files and directories used in build.properties
    private static final String ROOT_FILE_NAME = "rootFile.txt";

    private static final String ROOT_FILE2_NAME = "file1.txt";

    private static final String ROOT_FILE2_REL_PATH = "rootfiles/" + ROOT_FILE2_NAME;

    static Properties createBuildPropertiesWithDefaultRootFiles() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "rootfiles");
        return buildProperties;
    }

    static Properties createBuildPropertiesWithoutRootKeys() {
        Properties buildProperties = new Properties();
        buildProperties.put("some.unrelated.key", "123");
        return buildProperties;
    }

    static IFeatureRootAdvice createAdvice(Properties buildProperties) {
        return new FeatureRootAdvice(new BuildPropertiesImpl(buildProperties), FEATURE_PROJECT_TEST_RESOURCE_ROOT,
                DEFAULT_ARTIFACT_ID);
    }

    static void callGetDescriptorsForAllConfigurations(IFeatureRootAdvice advice) {
        for (String configuration : advice.getConfigurations()) {
            advice.getDescriptor(configuration);
        }
    }

    @Test
    public void testFeatureRootAdviceComputePath() throws Exception {
        IFeatureRootAdvice rootFileAdvice = FeatureRootAdvice.createRootFileAdvice(createDefaultArtifactMock(),
                new BuildPropertiesParserForTesting());

        File file1 = new File(FEATURE_PROJECT_TEST_RESOURCE_ROOT, ROOT_FILE_NAME).getCanonicalFile();
        IPath expectedPathFile1 = new Path(ROOT_FILE_NAME);
        IPath actualPathFile1 = rootFileAdvice.getRootFileComputer(GLOBAL_SPEC).computePath(file1);

        assertEquals(expectedPathFile1, actualPathFile1);

        File file2 = new File(FEATURE_PROJECT_TEST_RESOURCE_ROOT, ROOT_FILE2_REL_PATH).getCanonicalFile();
        IPath expectedPathFile2 = new Path(ROOT_FILE2_NAME);
        IPath actualPathFile2 = rootFileAdvice.getRootFileComputer(GLOBAL_SPEC).computePath(file2);

        assertEquals(expectedPathFile2, actualPathFile2);
    }

    @Test
    public void testGetProjectBaseDir() throws Exception {
        ArtifactMock defaultArtifactMock = createDefaultArtifactMock();
        assertEquals(FEATURE_PROJECT_TEST_RESOURCE_ROOT.getCanonicalFile(),
                FeatureRootAdvice.getProjectBaseDir(defaultArtifactMock).getCanonicalFile());

        // null checks
        ArtifactMock wrongTypeArtifactMock = new ArtifactMock(new File(FEATURE_JAR_REL_PATH).getCanonicalFile(),
                GROUP_ID, ARTIFACT_ID, VERSION, "eclipse-plugin");
        assertNull(FeatureRootAdvice.getProjectBaseDir(wrongTypeArtifactMock));

        ArtifactMock invalidLocationArtifactMock = new ArtifactMock(new File(
                "resources/rootfiles/feature-project/target/feature.jar").getCanonicalFile(), GROUP_ID, ARTIFACT_ID,
                VERSION, PACKAGING_TYPE);
        assertNull(FeatureRootAdvice.getProjectBaseDir(invalidLocationArtifactMock));

        ArtifactMock invalidRelativeLocationArtifactMock = new ArtifactMock(new File(FEATURE_JAR_REL_PATH), GROUP_ID,
                ARTIFACT_ID, VERSION, PACKAGING_TYPE);
        assertNull(FeatureRootAdvice.getProjectBaseDir(invalidRelativeLocationArtifactMock));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBuildPropertiesInvalidConfigs() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.invalid.key", "file:rootfiles/file1.txt");

        createAdvice(buildProperties);
    }

    @Ignore("No check that config specs are valid")
    @Test(expected = IllegalArgumentException.class)
    public void testParseBuildPropertiesInvalidConfigs2() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.ws.os.arch", "file:rootfiles/file1.txt");

        createAdvice(buildProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBuildPropertiesWithTrailingDots() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root..", "file:rootfiles/file1.txt");

        createAdvice(buildProperties);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedFolderBuildProperties() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.folder.dir", "file:rootfiles/file1.txt");

        createAdvice(buildProperties);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedFolderBuildProperties2() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.win32.win32.x86.folder.dir", "file:rootfiles/file1.txt");

        createAdvice(buildProperties);
    }

    @Test
    public void testDescriptorIsNullIfNoRootFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        IFeatureRootAdvice advice = createAdvice(buildProperties);

        assertNull(advice.getDescriptor(GLOBAL_SPEC));
        assertNull(advice.getDescriptor(LINUX_SPEC_FOR_ADVICE));
    }

    private ArtifactMock createDefaultArtifactMock() throws IOException {
        return (new ArtifactMock(new File(FEATURE_JAR_REL_PATH).getCanonicalFile(), GROUP_ID, ARTIFACT_ID, VERSION,
                PACKAGING_TYPE));
    }
}
