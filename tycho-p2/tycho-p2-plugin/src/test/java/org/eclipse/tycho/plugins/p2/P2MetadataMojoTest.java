/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
package org.eclipse.tycho.plugins.p2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class P2MetadataMojoTest {
    private static final File MAIN_ARTIFACT = new File("bin.jar");

    private static final File SOURCE_ARTIFACT = new File("source.jar");

    private static final File OTHER_ARTIFACT = new File("some/other.xml");

    File testFile;

    @BeforeEach
    public void init() throws Exception {
        testFile = File.createTempFile(this.getClass().getName(), ".properties");
        testFile.delete();
    }

    @AfterEach
    public void cleanUp() {
        testFile.delete();
    }

    @Test
    public void testWriteArtifactLocations() throws Exception {
        Map<String, File> artifactLocations = new HashMap<>();
        artifactLocations.put(null, MAIN_ARTIFACT);
        artifactLocations.put("source", SOURCE_ARTIFACT);
        artifactLocations.put("other-classifier", OTHER_ARTIFACT);

        P2MetadataMojo.writeArtifactLocations(testFile, artifactLocations);

        Properties result = loadProperties(testFile);
        assertEquals(3, result.size());
        assertEquals(MAIN_ARTIFACT.getCanonicalFile(), getFileEntry("artifact.main", result));
        assertEquals(SOURCE_ARTIFACT.getCanonicalFile(), getFileEntry("artifact.attached.source", result));
        assertEquals(OTHER_ARTIFACT.getCanonicalFile(), getFileEntry("artifact.attached.other-classifier", result));
    }

    @Test
    public void testWriteOnlyAttachedArtifactLocation() throws Exception {
        Map<String, File> artifactLocations = new HashMap<>();
        artifactLocations.put("other-classifier", OTHER_ARTIFACT);

        P2MetadataMojo.writeArtifactLocations(testFile, artifactLocations);

        Properties result = loadProperties(testFile);
        assertEquals(1, result.size());
        assertEquals(OTHER_ARTIFACT.getCanonicalFile(), getFileEntry("artifact.attached.other-classifier", result));
    }

    private File getFileEntry(String key, Properties result) throws IOException {
        String value = (String) result.get(key);
        if (value == null)
            return new File("");
        return new File(value).getCanonicalFile();
    }

    private static Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream propertiesStream = new FileInputStream(propertiesFile)) {
            properties.load(propertiesStream);
        }
        return properties;
    }
}
