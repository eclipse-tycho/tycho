/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.facade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class BuildPropertiesImplTest {

    @Test
    public void testSupportedKeys() {
        BuildPropertiesImpl buildProperties = new BuildPropertiesImpl(new File("resources/testbuild.properties"));
        assertEquals("1.3", buildProperties.getJavacSource());
        assertEquals("1.1", buildProperties.getJavacTarget());
        assertEquals("JavaSE-1.6", buildProperties.getJreCompilationProfile());
        assertEquals(Arrays.asList("folder/", "file.txt"), buildProperties.getBinIncludes());
        assertEquals(Arrays.asList("excluded_folder/", "excluded_file.txt"), buildProperties.getBinExcludes());
        assertEquals(Arrays.asList("src_folder/", "src_file.txt"), buildProperties.getSourceIncludes());
        assertEquals(Arrays.asList("excluded_src_folder/", "excluded_src_file.txt"),
                buildProperties.getSourceExcludes());
        assertEquals(Collections.singletonList("platform_URL"), buildProperties.getJarsExtraClasspath());
        assertEquals(Arrays.asList("foo.jar", "bar.jar"), buildProperties.getJarsCompileOrder());
        assertEquals(Collections.singletonMap(".", Arrays.asList("extra.jar")),
                buildProperties.getJarToExtraClasspathMap());
        assertEquals(Collections.singletonMap(".", "ISO-8859-1"), buildProperties.getJarToJavacDefaultEncodingMap());
        assertEquals("20120101000000", buildProperties.getForceContextQualifier());
        assertEquals(Collections.singletonMap(".", Arrays.asList("foo/", "bar/")),
                buildProperties.getJarToSourceFolderMap());
        assertEquals(Collections.singletonMap(".", "bin/"), buildProperties.getJarToOutputFolderMap());
        Map<String, String> rootEntries = buildProperties.getRootEntries();
        assertEquals(2, rootEntries.size());
        assertEquals("rootFolder/", rootEntries.get("root"));
        assertEquals("winRootFolder/", rootEntries.get("root.win32.win32.x86"));
        assertFalse(buildProperties.isRootFilesUseDefaultExcludes());
    }

    @Test
    public void testNoBuildPropertiesFileFound() throws Exception {
        BuildPropertiesImpl buildProperties = new BuildPropertiesImpl(new File("DOES_NOT_EXIST"));
        assertNotNull(buildProperties);
    }

}
