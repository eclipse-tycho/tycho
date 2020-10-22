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

import java.io.File;

import org.junit.Assert;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

public class ProjectMetadataReaderTest extends PlexusTestCase {

    private ProjectMetadataReader reader;

    @Override
    public void setUp() throws Exception {
        reader = lookup(ProjectMetadataReader.class);
    }

    public void test_moduleElementWithExplicitPomXml() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml");
        Assert.assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(1, reader.getProjects().size());
    }

    public void test_customPomXmlFileName() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml_expected");
        Assert.assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(1, reader.getProjects().size());
    }

    public void test_missingBasedir() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/missing");
        Assert.assertFalse(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(0, reader.getProjects().size());
    }
}
