/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.junit.jupiter.api.Test;

@PlexusTest
public class ProjectMetadataReaderTest {

    @Inject
    private ProjectMetadataReader reader;

    @Test
    public void test_moduleElementWithExplicitPomXml() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml");
        assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir, true);
        assertEquals(1, reader.getProjects().size());
    }

    @Test
    public void test_customPomXmlFileName() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml_expected");
        assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir, true);
        assertEquals(1, reader.getProjects().size());
    }

    @Test
    public void test_missingBasedir() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/missing");
        assertFalse(basedir.exists()); // sanity check
        reader.addBasedir(basedir, true);
        assertEquals(0, reader.getProjects().size());
    }
}
