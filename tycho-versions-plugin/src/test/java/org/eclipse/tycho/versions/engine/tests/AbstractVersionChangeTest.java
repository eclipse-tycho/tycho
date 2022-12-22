/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype Inc. and others.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

// TODO reference helpers via static import instead of misusing inheritance for this 
public abstract class AbstractVersionChangeTest extends AbstractMojoTestCase {
    protected static void assertPom(File basedir) throws IOException {
        assertFileContent(new File(basedir, "pom.xml"));
    }

    protected static void assertBundleManifest(File basedir) throws IOException {
        assertFileContent(new File(basedir, "META-INF/MANIFEST.MF"));
    }

    protected static void assertFeatureXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "feature.xml"));
    }

    protected static void assertCategoryXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "category.xml"));
    }

    protected static void assertProductFile(File basedir, String name) throws IOException {
        assertFileContent(new File(basedir, name));
    }

    protected static void assertFileContent(File actual) throws IOException {
        File expected = new File(actual.getParentFile(), actual.getName() + "_expected");
        assertEquals(toAsciiString(expected), toAsciiString(actual));
    }

    protected static void assertP2IuXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "p2iu.xml"));
    }

    private static String toAsciiString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String str;
            while ((str = r.readLine()) != null) {
                sb.append(str).append('\n');
            }
        }
        return sb.toString();
    }

}
