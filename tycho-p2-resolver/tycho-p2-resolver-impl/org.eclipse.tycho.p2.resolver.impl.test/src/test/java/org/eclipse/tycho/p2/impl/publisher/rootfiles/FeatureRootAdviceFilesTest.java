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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdviceFilesTest {

    @Test
    public void testParseBuildPropertiesRelativeFile() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "file:rootfiles/file1.txt");

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(1, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFiles() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "file:rootfiles/file1.txt,file:rootfiles/dir/file3.txt");

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(2, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
        assertRootFileEntry(filesMap, "rootfiles/dir/file3.txt", "file3.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFolder() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "rootfiles");

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(4, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
        assertRootFileEntry(filesMap, "rootfiles/file2.txt", "file2.txt");
        assertRootFileEntry(filesMap, "rootfiles/dir", "dir");
        assertRootFileEntry(filesMap, "rootfiles/dir/file3.txt", "dir/file3.txt"); // TODO this is redundant, including dir implies this
    }

    @Test
    public void testParseBuildPropertiesAbsoluteFile() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root", "absolute:file:" + absoluteResourceFile("rootfiles/file1.txt"));

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(buildProperties, GLOBAL_SPEC);

        assertEquals(1, filesMap.size());
        assertRootFileEntry(filesMap, "rootfiles/file1.txt", "file1.txt");
    }

    @Test
    public void testParseBuildPropertiesRelativeFileWithLinuxConfig() {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file1.txt");

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(buildProperties, LINUX_SPEC_FOR_ADVICE);

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

        Map<File, IPath> winFilesMap = getSourceToDestinationMap(advice, WINDOWS_SPEC_FOR_ADVICE);
        assertEquals(1, winFilesMap.size());
        assertRootFileEntry(winFilesMap, "rootfiles/file1.txt", "file1.txt");

        Map<File, IPath> linuxFilesMap = getSourceToDestinationMap(advice, LINUX_SPEC_FOR_ADVICE);
        assertEquals(1, linuxFilesMap.size());
        assertRootFileEntry(linuxFilesMap, "rootfiles/file2.txt", "file2.txt");

        Map<File, IPath> globalFilesMap = getSourceToDestinationMap(advice, GLOBAL_SPEC);
        assertEquals(1, globalFilesMap.size());
        assertRootFileEntry(globalFilesMap, "rootfiles/dir/file3.txt", "file3.txt");
    }

    @Test
    public void testWhitespacesAroundSeparatorsOfFiles() {
        Properties properties = createBuildPropertiesWithDefaultRootFiles();
        properties.put("root", "rootfiles/file1.txt ,\n\trootfiles/file2.txt");

        Map<File, IPath> filesMap = createAdviceAndGetFilesMap(properties, GLOBAL_SPEC);

        assertEquals(2, filesMap.size());
    }

    private static Map<File, IPath> createAdviceAndGetFilesMap(Properties buildProperties, String configSpec) {
        IFeatureRootAdvice advice = createAdvice(buildProperties);
        Map<File, IPath> defaultRootFilesMap = getSourceToDestinationMap(advice, configSpec);
        return defaultRootFilesMap;
    }

    private static Map<File, IPath> getSourceToDestinationMap(IFeatureRootAdvice advice, String configSpec) {
        File[] filesInSources = advice.getDescriptor(configSpec).getFiles();
        IPathComputer rootFileComputer = advice.getRootFileComputer(configSpec);

        Map<File, IPath> filesMap = new HashMap<File, IPath>();
        for (File fileInSources : filesInSources) {
            IPath pathInInstallation = rootFileComputer.computePath(fileInSources);
            filesMap.put(fileInSources, pathInInstallation);
        }
        return filesMap;
    }

    private static void assertRootFileEntry(Map<File, IPath> filesMap, String expectedPathInSources,
            String expectedPathInInstallation) {
        IPath actualPathInInstallation = filesMap.get(absoluteResourceFile(expectedPathInSources));
        assertFalse("File not included as root file: " + expectedPathInSources, actualPathInInstallation == null);
        assertFalse(actualPathInInstallation.isAbsolute());
        assertEquals(new Path(expectedPathInInstallation), actualPathInInstallation);
    }

    private static File absoluteResourceFile(String pathInTestResources) {
        return new File(FEATURE_PROJECT_TEST_RESOURCE_ROOT, pathInTestResources).getAbsoluteFile();
    }

}
