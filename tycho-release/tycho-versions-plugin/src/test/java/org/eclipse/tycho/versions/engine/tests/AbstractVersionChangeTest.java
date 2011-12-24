/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.codehaus.plexus.PlexusTestCase;

public abstract class AbstractVersionChangeTest extends PlexusTestCase {
    protected void assertPom(File basedir) throws IOException {
        assertFileContent(new File(basedir, "pom.xml"));
    }

    protected void assertBundleManifest(File basedir) throws IOException {
        assertFileContent(new File(basedir, "META-INF/MANIFEST.MF"));
    }

    protected void assertFeatureXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "feature.xml"));
    }

    protected void assertSiteXml(File basedir) throws IOException {
        assertFileContent(new File(basedir, "site.xml"));
    }

    protected void assertProductFile(File basedir, String name) throws IOException {
        assertFileContent(new File(basedir, name));
    }

    protected void assertFileContent(File actual) throws IOException {
        File expected = new File(actual.getParentFile(), actual.getName() + "_expected");
        assertEquals(toAsciiString(expected), toAsciiString(actual));
    }

    private String toAsciiString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        try {
            String str;
            while ((str = r.readLine()) != null) {
                sb.append(str).append('\n');
            }
        } finally {
            r.close();
        }
        return sb.toString();
    }

}
