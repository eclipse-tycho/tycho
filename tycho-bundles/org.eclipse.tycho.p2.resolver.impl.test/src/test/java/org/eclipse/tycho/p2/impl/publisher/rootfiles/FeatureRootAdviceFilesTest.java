/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Bachmann electronic GmbH - adding support for root.folder and root.<config>.folder
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.FEATURE_PROJECT_TEST_RESOURCE_ROOT;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.GLOBAL_SPEC;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.LINUX_SPEC_FOR_ADVICE;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.LINUX_SPEC_FOR_PROPERTIES_KEY;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.WINDOWS_SPEC_FOR_ADVICE;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.WINDOWS_SPEC_FOR_PROPERTIES_KEY;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createAdvice;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createBuildPropertiesWithDefaultRootFiles;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createBuildPropertiesWithoutRootKeys;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.p2.publisher.rootfiles.FileToPathMap;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdviceFilesTest {

    @Test
    public void testParseBuildPropertiesRelativeFile() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "file:rootfiles/file1.txt");

        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(1, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFiles() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "file:rootfiles/file1.txt,file:rootfiles/dir/file3.txt");

        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(2, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
        assertRootFileEntry(filesMap, "rootfiles/dir/file3.txt", "file3.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFolder() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "rootfiles");

        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(3, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
        assertRootFileEntry(filesMap, "rootfiles/file2.txt", "file2.txt");
        assertRootFileEntry(filesMap, "rootfiles/dir/file3.txt", "dir/file3.txt");
    }

    @Test
    public void testParseBuildPropertiesAbsoluteFile() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "absolute:file:" + getResourceFile("rootfiles/file1.txt"));

        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(1, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithLinuxConfig() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file1.txt");

        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, LINUX_SPEC_FOR_ADVICE);

        assertEquals(1, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithAndWithoutConfigs() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file1.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file2.txt");
        buildProperties.put("root", "rootfiles/dir");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        FileToPathMap winFilesMap = getSourceToDestinationMap(advice, WINDOWS_SPEC_FOR_ADVICE);
        assertEquals(1, winFilesMap.size());
        assertRootFileEntry(winFilesMap, "rootfiles/file1.txt", "file1.txt");

        FileToPathMap linuxFilesMap = getSourceToDestinationMap(advice, LINUX_SPEC_FOR_ADVICE);
        assertEquals(1, linuxFilesMap.size());
        assertRootFileEntry(linuxFilesMap, "rootfiles/file2.txt", "file2.txt");

        FileToPathMap globalFilesMap = getSourceToDestinationMap(advice, GLOBAL_SPEC);
        assertEquals(1, globalFilesMap.size());
        assertRootFileEntry(globalFilesMap, "rootfiles/dir/file3.txt", "file3.txt");
    }

    @Test
    public void testWhitespacesAroundSeparatorsOfFiles() {
        Properties properties = createBuildPropertiesWithDefaultRootFiles();
        properties.put("root", "rootfiles/file1.txt ,\n\trootfiles/file2.txt");

        FileToPathMap filesMap = createAdviceAndGetFilesMap(properties, GLOBAL_SPEC);

        assertEquals(2, filesMap.keySet().size());
    }

    @Test
    public void testRootFilesWithFolders() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.folder.foo/bar", "file:rootfiles/file1.txt");
        FileToPathMap filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);
        assertEquals(1, filesMap.keySet().size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "foo/bar/file1.txt");
    }

    @Test
    public void testRootFilesWithFoldersAndConfig() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.folder.foo", "file:rootfiles/dir/file3.txt");
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY + ".folder.windir", "file:rootfiles/file1.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".folder.linuxdir", "file:rootfiles/file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        FileToPathMap winFilesMap = getSourceToDestinationMap(advice, WINDOWS_SPEC_FOR_ADVICE);
        assertEquals(1, winFilesMap.size());
        assertRootFileEntry(winFilesMap, "rootfiles/file1.txt", "windir/file1.txt");

        FileToPathMap linuxFilesMap = getSourceToDestinationMap(advice, LINUX_SPEC_FOR_ADVICE);
        assertEquals(1, linuxFilesMap.size());
        assertRootFileEntry(linuxFilesMap, "rootfiles/file2.txt", "linuxdir/file2.txt");

        FileToPathMap globalFilesMap = getSourceToDestinationMap(advice, GLOBAL_SPEC);
        assertEquals(1, globalFilesMap.size());
        assertRootFileEntry(globalFilesMap, "rootfiles/dir/file3.txt", "foo/file3.txt");
    }

    private static FileToPathMap createAdviceAndGetFilesMap(Properties buildProperties, String configSpec) {
        IFeatureRootAdvice advice = createAdvice(buildProperties);
        FileToPathMap defaultRootFilesMap = getSourceToDestinationMap(advice, configSpec);
        return defaultRootFilesMap;
    }

    private static FileToPathMap getSourceToDestinationMap(IFeatureRootAdvice advice, String configSpec) {
        File[] filesInSources = advice.getDescriptor(configSpec).getFiles();
        IPathComputer rootFileComputer = advice.getRootFileComputer(configSpec);

        FileToPathMap filesMap = new FileToPathMap();
        for (File fileInSources : filesInSources) {
            IPath pathInInstallation = rootFileComputer.computePath(fileInSources);
            filesMap.put(fileInSources, pathInInstallation);
        }
        return filesMap;
    }

    private static void assertRootFileEntry(FileToPathMap winFilesMap, String expectedPathInSources,
            String expectedPathInInstallation) {
        IPath actualPathInInstallation = winFilesMap.get(getResourceFile(expectedPathInSources));
        assertFalse("File not included as root file: " + expectedPathInSources, actualPathInInstallation == null);
        assertFalse(actualPathInInstallation.isAbsolute());
        assertEquals(new Path(expectedPathInInstallation), actualPathInInstallation);
    }

    private static File getResourceFile(String pathInTestResources) {
        return new File(FEATURE_PROJECT_TEST_RESOURCE_ROOT, pathInTestResources);
    }

}
