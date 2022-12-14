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

import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProjectMetadataReaderTest extends TychoPlexusTestCase {

    private ProjectMetadataReader reader;

    @Before
    public void setUp() throws Exception {
        reader = lookup(ProjectMetadataReader.class);
    }

    @Test
    public void test_moduleElementWithExplicitPomXml() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml");
        Assert.assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(1, reader.getProjects().size());
    }

    @Test
    public void test_customPomXmlFileName() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/pom.xml_expected");
        Assert.assertTrue(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(1, reader.getProjects().size());
    }

    @Test
    public void test_missingBasedir() throws Exception {
        File basedir = new File("src/test/resources/projects/simple/missing");
        Assert.assertFalse(basedir.exists()); // sanity check
        reader.addBasedir(basedir);
        Assert.assertEquals(0, reader.getProjects().size());
    }
}
