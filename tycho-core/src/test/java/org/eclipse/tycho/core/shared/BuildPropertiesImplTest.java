/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

public class BuildPropertiesImplTest {

    /**
     * Simulate Properties with different key/entry iteration order in different JDKs.
     */
    private static class SortedProperties extends Properties {

        private boolean reverse;

        public SortedProperties(boolean reverseSortOrder) {
            super();
            this.reverse = reverseSortOrder;
        }

        @Override
        public Set<Object> keySet() {
            List<Object> sortedList = new ArrayList<>(super.keySet());
            sortedList.sort(null);
            if (reverse) {
                Collections.reverse(sortedList);
            }
            return new LinkedHashSet<>(sortedList);
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            List<Map.Entry<Object, Object>> sortedList = new ArrayList<>(super.entrySet());
            Collections.sort(sortedList, (o1, o2) -> ((String) o2.getKey()).compareTo((String) o1.getKey()));
            if (reverse) {
                Collections.reverse(sortedList);
            }
            return new LinkedHashSet<>(sortedList);
        }
    }

    @Test
    public void testSupportedKeys() throws IOException {
        BuildPropertiesImpl buildProperties = new BuildPropertiesImpl(
                readProperties(new File("src/test/resources/buildproperties/testbuild.properties")));
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
    public void testKeyOrderIsStable() throws Exception {
        Properties sortedProperties = new SortedProperties(false);
        sortedProperties.setProperty("source.a.jar", "source-a1/,source-a2/");
        sortedProperties.setProperty("source.b.jar", "source-b1/,source-b2/");
        sortedProperties.setProperty("source.c.jar", "source-c1/,source-c2/");

        Properties reverseSortedProperties = new SortedProperties(true);
        reverseSortedProperties.putAll(sortedProperties);

        BuildPropertiesImpl buildProperties1 = new BuildPropertiesImpl(sortedProperties);
        BuildPropertiesImpl buildProperties2 = new BuildPropertiesImpl(reverseSortedProperties);
        List<String> sourceFolderKeys1 = new ArrayList<>(buildProperties1.getJarToSourceFolderMap().keySet());
        List<String> sourceFolderKeys2 = new ArrayList<>(buildProperties2.getJarToSourceFolderMap().keySet());
        assertEquals("keyset iteration order must be stable.", sourceFolderKeys1, sourceFolderKeys2);
    }

    @Test
    public void testNoBuildPropertiesFileFound() throws Exception {
        BuildPropertiesImpl buildProperties = new BuildPropertiesImpl(new Properties());
        assertNotNull(buildProperties);
    }

    private static Properties readProperties(File propsFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(propsFile)) {
            properties.load(is);
        }
        return properties;
    }

}
