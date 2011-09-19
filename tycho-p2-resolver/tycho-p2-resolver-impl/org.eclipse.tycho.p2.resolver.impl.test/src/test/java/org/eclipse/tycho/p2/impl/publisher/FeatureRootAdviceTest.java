/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdviceTest {
    private static final String WINDOWS_SPEC_FOR_PROPERTIES_KEY = "win32.win32.x86";

    private static final Object WINDOWS_SPEC_FOR_ADVICE = "win32.win32.x86";

    private static final String LINUX_SPEC_FOR_PROPERTIES_KEY = "linux.gtk.x86";

    private static final String LINUX_SPEC_FOR_ADVICE = "gtk.linux.x86";

    private static final String RESOURCES_ROOTFILES_REL_PATH = "resources/rootfiles";

    public static final File RESOURCES_FEATURE_PROJ_REL_PATH = new File(RESOURCES_ROOTFILES_REL_PATH
            + "/feature-project");

    private static final String FEATURE_JAR_REL_PATH = RESOURCES_ROOTFILES_REL_PATH
            + "/feature-project/target/feature-0.0.1-SNAPSHOT.jar";

    private static final String GROUP_ID = "group";

    private static final String ARTIFACT_ID = "feature";

    private static final String VERSION = "0.0.1-SNAPSHOT";

    private static final String PACKAGING_TYPE = "eclipse-feature";

    private static final String DEFAULT_CONFIG_SPEC = "";

    private static final String DEFAULT_ARTIFACT_ID = "artifact.for.which.advice.applies";

    // files and directories used in build.properties
    private static final String ROOT_FILE_NAME = "rootFile.txt";

    private static final String ROOT_FILE2_NAME = "file1.txt";

    private static final String ROOT_FILE2_REL_PATH = "rootfiles/" + ROOT_FILE2_NAME;

    @Test
    public void testFeatureRootAdviceComputePath() throws Exception {
        IFeatureRootAdvice rootFileAdvice = FeatureRootAdvice.createRootFileAdvice(createDefaultArtifactMock());

        File file1 = new File(RESOURCES_FEATURE_PROJ_REL_PATH, ROOT_FILE_NAME).getCanonicalFile();
        IPath expectedPathFile1 = new Path(ROOT_FILE_NAME);
        IPath actualPathFile1 = rootFileAdvice.getRootFileComputer(DEFAULT_CONFIG_SPEC).computePath(file1);

        assertEquals(expectedPathFile1, actualPathFile1);

        File file2 = new File(RESOURCES_FEATURE_PROJ_REL_PATH, ROOT_FILE2_REL_PATH).getCanonicalFile();
        IPath expectedPathFile2 = new Path(ROOT_FILE2_NAME);
        IPath actualPathFile2 = rootFileAdvice.getRootFileComputer(DEFAULT_CONFIG_SPEC).computePath(file2);

        assertEquals(expectedPathFile2, actualPathFile2);
    }

    @Test
    public void testGetProjectBaseDir() throws Exception {
        ArtifactMock defaultArtifactMock = createDefaultArtifactMock();
        assertEquals(RESOURCES_FEATURE_PROJ_REL_PATH.getCanonicalFile(),
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

    @Test
    public void testParseBuildPropertiesNullChecks() {
        assertNull(FeatureRootAdvice.getRootFilesFromBuildProperties(null, null));

        assertNull(FeatureRootAdvice.getRootFilesFromBuildProperties(null, RESOURCES_FEATURE_PROJ_REL_PATH));

        assertNull(FeatureRootAdvice.getRootFilesFromBuildProperties(new Properties(), null));
    }

    @Test
    public void testParseBuildPropertiesRelativeFile() {
        Properties buildProperties = new Properties();
        buildProperties.put("root", "file:rootfiles/file1.txt");

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get(DEFAULT_CONFIG_SPEC);

        assertEquals(1, defaultRootFilesMap.size());

        IPath entryPath = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt"));
        assertFalse(entryPath.isAbsolute());

        assertEquals(new Path("file1.txt"), entryPath);
    }

    @Test
    public void testParseBuildPropertiesRelativeFiles() {
        Properties buildProperties = new Properties();
        buildProperties.put("root", "file:rootfiles/file1.txt,file:rootfiles/dir/file3.txt");

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get(DEFAULT_CONFIG_SPEC);
        assertNotNull(defaultRootFilesMap);
        assertEquals(2, defaultRootFilesMap.size());

        IPath entryPathFile1 = defaultRootFilesMap
                .get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt"));
        assertFalse(entryPathFile1.isAbsolute());
        assertEquals(new Path("file1.txt"), entryPathFile1);

        IPath entryPathFile3 = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH,
                "rootfiles/dir/file3.txt"));
        assertFalse(entryPathFile3.isAbsolute());

        assertEquals(new Path("file3.txt"), entryPathFile3);
    }

    @Test
    public void testParseBuildPropertiesRelativeFolder() {
        Properties buildProperties = new Properties();
        buildProperties.put("root", "rootfiles");

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get(DEFAULT_CONFIG_SPEC);

        assertEquals(4, defaultRootFilesMap.size());

        IPath entryPathFile1 = defaultRootFilesMap
                .get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt"));
        assertFalse(entryPathFile1.isAbsolute());
        assertEquals(new Path("file1.txt"), entryPathFile1);

        IPath entryPathFile2 = defaultRootFilesMap
                .get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file2.txt"));
        assertFalse(entryPathFile2.isAbsolute());
        assertEquals(new Path("file2.txt"), entryPathFile2);

        IPath entryPathDir = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/dir"));
        assertFalse(entryPathDir.isAbsolute());
        assertEquals(new Path("dir"), entryPathDir);

        IPath entryPathFile3 = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH,
                "rootfiles/dir/file3.txt"));
        assertFalse(entryPathFile3.isAbsolute());
        assertEquals(new Path("dir/file3.txt"), entryPathFile3);
    }

    @Test
    public void testParseBuildPropertiesAbsoluteFile() throws Exception {
        Properties buildProperties = new Properties();
        File file = new File(RESOURCES_FEATURE_PROJ_REL_PATH, "/rootfiles/file1.txt");
        buildProperties.put("root", "absolute:file:" + file.getAbsolutePath());

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get(DEFAULT_CONFIG_SPEC);
        assertEquals(1, defaultRootFilesMap.size());

        IPath entryPath = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt")
                .getCanonicalFile());
        assertFalse(entryPath.isAbsolute());

        assertEquals(new Path("file1.txt"), entryPath);
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithLinuxConfig() {
        Properties buildProperties = new Properties();
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file1.txt");

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        Map<File, IPath> defaultRootFilesMap = rootFilesFromBuildProperties.get(LINUX_SPEC_FOR_ADVICE);

        assertEquals(1, defaultRootFilesMap.size());

        IPath entryPath = defaultRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt"));
        assertFalse(entryPath.isAbsolute());

        assertEquals(new Path("file1.txt"), entryPath);
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithAndWithoutConfigs() {
        Properties buildProperties = new Properties();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file1.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file2.txt");
        buildProperties.put("root", "rootfiles/dir");

        Map<String, Map<File, IPath>> rootFilesFromBuildProperties = FeatureRootAdvice.getRootFilesFromBuildProperties(
                buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH);

        assertNotNull(rootFilesFromBuildProperties);

        // win config
        Map<File, IPath> winConfigRootFilesMap = rootFilesFromBuildProperties.get(WINDOWS_SPEC_FOR_ADVICE);

        assertEquals(1, winConfigRootFilesMap.size());

        IPath winEntryPath = winConfigRootFilesMap
                .get(new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt"));
        assertFalse(winEntryPath.isAbsolute());

        assertEquals(new Path("file1.txt"), winEntryPath);

        // linux config
        Map<File, IPath> linuxConfigRootFilesMap = rootFilesFromBuildProperties.get(LINUX_SPEC_FOR_ADVICE);

        assertEquals(1, linuxConfigRootFilesMap.size());

        IPath linuxEntryPath = linuxConfigRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH,
                "rootfiles/file2.txt"));
        assertFalse(linuxEntryPath.isAbsolute());

        assertEquals(new Path("file2.txt"), linuxEntryPath);

        // without config
        Map<File, IPath> defaultConfigRootFilesMap = rootFilesFromBuildProperties.get("");

        assertEquals(1, defaultConfigRootFilesMap.size());

        IPath entryPath = defaultConfigRootFilesMap.get(new File(RESOURCES_FEATURE_PROJ_REL_PATH,
                "rootfiles/dir/file3.txt"));
        assertFalse(entryPath.isAbsolute());

        assertEquals(new Path("file3.txt"), entryPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBuildPropertiesInvalidConfigs() {
        Properties invalidBuildProperties1 = new Properties();
        invalidBuildProperties1.put("root.invalid.config", "file:rootfiles/file1.txt");

        FeatureRootAdvice.getRootFilesFromBuildProperties(invalidBuildProperties1, RESOURCES_FEATURE_PROJ_REL_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBuildPropertiesInvalidConfigs2() {
        Properties invalidBuildProperties2 = new Properties();
        invalidBuildProperties2.put("root...", "file:rootfiles/file2.txt");

        FeatureRootAdvice.getRootFilesFromBuildProperties(invalidBuildProperties2, RESOURCES_FEATURE_PROJ_REL_PATH);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedFolderBuildProperties() {
        Properties unsupportedBuildProperties1 = new Properties();
        unsupportedBuildProperties1.put("root.folder.dir", "file:rootfiles/file1.txt");

        FeatureRootAdvice.getRootFilesFromBuildProperties(unsupportedBuildProperties1, RESOURCES_FEATURE_PROJ_REL_PATH);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedFolderBuildProperties2() {
        Properties unsupportedBuildProperties2 = new Properties();
        unsupportedBuildProperties2.put("root.win32.win32.x86.folder.dir", "file:rootfiles/file1.txt");

        FeatureRootAdvice.getRootFilesFromBuildProperties(unsupportedBuildProperties2, RESOURCES_FEATURE_PROJ_REL_PATH);
    }

    @Test
    public void testDescriptorIsNullIfNoRootFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        IFeatureRootAdvice advice = createAdvice(buildProperties);

        assertNull(advice.getDescriptor(DEFAULT_CONFIG_SPEC));
        assertNull(advice.getDescriptor(LINUX_SPEC_FOR_ADVICE));
    }

    @Test
    public void testDescriptorReturnsRootFile() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "file:rootfiles/file1.txt");
        IFeatureRootAdvice advice = createAdvice(buildProperties);

        File[] actualFiles = advice.getDescriptor(DEFAULT_CONFIG_SPEC).getFiles();
        File expectedFile = new File(RESOURCES_FEATURE_PROJ_REL_PATH, "rootfiles/file1.txt");
        assertEquals(Collections.singletonList(expectedFile), Arrays.asList(actualFiles));
    }

    @Test
    public void testNoPermisions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        List<String[]> actualPermissions = createAdviceAndGetPermissions(buildProperties, DEFAULT_CONFIG_SPEC);
        assertEquals(0, actualPermissions.size());
    }

    @Test
    public void testGlobalPermissions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", "file1.txt,dir/file3.txt");
        buildProperties.put("root.permissions.555", "file2.txt");

        List<String[]> actualPermissions = createAdviceAndGetPermissions(buildProperties, DEFAULT_CONFIG_SPEC);

        assertEquals(3, actualPermissions.size());
        assertPermissionEntry("dir/file3.txt", "755", actualPermissions.get(0));
        assertPermissionEntry("file1.txt", "755", actualPermissions.get(1));
        assertPermissionEntry("file2.txt", "555", actualPermissions.get(2));
    }

    @Test
    public void testSpecificPermissions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", "file1.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:file2.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".permissions.755", "file2.txt");
        IFeatureRootAdvice advice = createAdvice(buildProperties);

        List<String[]> globalPermissions = getSortedPermissions(advice, DEFAULT_CONFIG_SPEC);
        assertEquals(1, globalPermissions.size());

        List<String[]> specificPermissions = getSortedPermissions(advice, LINUX_SPEC_FOR_ADVICE);
        assertEquals(1, specificPermissions.size());
    }

    @Test
    public void testWhitespaceAroundSeparatorsInPermissions() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", " file1.txt ,  dir/file3.txt, \n\tfile2.txt ");
        List<String[]> list = createAdviceAndGetPermissions(buildProperties, DEFAULT_CONFIG_SPEC);

        assertEquals(3, list.size());
        assertPermissionEntry("dir/file3.txt", "755", list.get(0));
        assertPermissionEntry("file1.txt", "755", list.get(1));
        assertPermissionEntry("file2.txt", "755", list.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalPermissionsChmodMissing() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions", "file1.txt");
        createAdvice(buildProperties).getDescriptor(DEFAULT_CONFIG_SPEC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificPermissionsChmodMissing() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY + ".permissions", "file1.txt");
        createAdvice(buildProperties).getDescriptor(DEFAULT_CONFIG_SPEC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalPermissionsButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.permissions.644", "file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificPermissionsButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".permissions.644", "file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testPermissionsChmodInvalidValue() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.og-rwx", "file1.txt");
        createAdvice(buildProperties).getDescriptor(DEFAULT_CONFIG_SPEC);
    }

    //
    // symbolic links tests
    //
    @Test
    public void testNoLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String globalLinks = advice.getDescriptor(DEFAULT_CONFIG_SPEC).getLinks();
        assertEquals("", globalLinks);
    }

    @Test
    public void testGlobalLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link", "dir/file3.txt,alias1.txt,file1.txt,alias2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String actualLink = advice.getDescriptor(DEFAULT_CONFIG_SPEC).getLinks();
        assertEquals("dir/file3.txt,alias1.txt,file1.txt,alias2.txt", actualLink);
    }

    @Test
    public void testSpecificLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:file3.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".link", "file3.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String globalLink = advice.getDescriptor(DEFAULT_CONFIG_SPEC).getLinks();
        assertEquals("", globalLink);

        String specificLink = advice.getDescriptor(LINUX_SPEC_FOR_ADVICE).getLinks();
        assertEquals("file3.txt,alias.txt", specificLink);
    }

    @Test
    public void testWhitespaceAroundSeparatorsInLinks() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link",
                " file1.txt , alias1.txt ,  dir/file3.txt,alias2.txt , \n\tfile2.txt , alias3.txt \n\t");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String actualLinks = advice.getDescriptor(DEFAULT_CONFIG_SPEC).getLinks();
        assertEquals("file1.txt,alias1.txt,dir/file3.txt,alias2.txt,file2.txt,alias3.txt", actualLinks);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongRootfilesLinksKey() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        buildProperties.put("root.link.addedTooMuch", "file1.txt,alias.txt");
        createAdvice(buildProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalLinkButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.link", "file1.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificLinkButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY + ".link", "file1.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLinkValueNotInPairs() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link", "file1.txt,alias1.txt,file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    private static Properties createBuildPropertiesWithDefaultRootFiles() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "rootfiles");
        return buildProperties;
    }

    private static Properties createBuildPropertiesWithoutRootKeys() {
        Properties buildProperties = new Properties();
        buildProperties.put("some.unrelated.key", "123");
        return buildProperties;
    }

    private static List<String[]> createAdviceAndGetPermissions(Properties buildProperties, String configSpec) {
        IFeatureRootAdvice advice = createAdvice(buildProperties);
        return getSortedPermissions(advice, configSpec);
    }

    private static IFeatureRootAdvice createAdvice(Properties buildProperties) {
        return new FeatureRootAdvice(buildProperties, RESOURCES_FEATURE_PROJ_REL_PATH, DEFAULT_ARTIFACT_ID);
    }

    private static List<String[]> getSortedPermissions(IFeatureRootAdvice advice, String configSpec) {
        String[][] permissionsArray = advice.getDescriptor(configSpec).getPermissions();
        ArrayList<String[]> permissionsList = new ArrayList<String[]>();
        permissionsList.addAll(Arrays.asList(permissionsArray));
        Collections.sort(permissionsList, new PermissionEntryComparator());
        return permissionsList;
    }

    private static void assertPermissionEntry(String expectedFile, String expectedChmod, String[] descriptorPermission) {
        assertEquals(expectedChmod, descriptorPermission[0]);
        assertEquals(expectedFile, descriptorPermission[1]);
    }

    private static void callGetDescriptorsForAllConfigurations(IFeatureRootAdvice advice) {
        for (String configuration : advice.getConfigurations()) {
            advice.getDescriptor(configuration);
        }
    }

    private ArtifactMock createDefaultArtifactMock() throws IOException {
        return (new ArtifactMock(new File(FEATURE_JAR_REL_PATH).getCanonicalFile(), GROUP_ID, ARTIFACT_ID, VERSION,
                PACKAGING_TYPE));
    }

    private static class PermissionEntryComparator implements Comparator<String[]> {
        public int compare(String[] o1, String[] o2) {
            // compare files
            return o1[1].compareTo(o2[1]);
        }
    }

}
