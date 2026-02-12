/*******************************************************************************
 * Copyright (c) 2011, 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ExtraAssertions {
    public static void assertPom(File basedir) throws IOException {
        assertFileContent(new File(basedir, "pom.xml"));
    }

    public static void assertBundleManifest(File basedir) throws IOException {
        assertFileContent(new File(basedir, "META-INF/MANIFEST.MF"));
    }

    public static void assertFeatureXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "feature.xml"));
    }

    public static void assertCategoryXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "category.xml"));
    }

    public static void assertProductFile(File basedir, String name) throws IOException {
        assertFileContent(new File(basedir, name));
    }

    public static void assertFileContent(File actual) throws IOException {
        File expected = new File(actual.getParentFile(), actual.getName() + "_expected");
        assertEquals(toAsciiString(expected), toAsciiString(actual));
    }

    public static void assertP2IuXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "p2iu.xml"));
    }

    private static String toAsciiString(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }

}
