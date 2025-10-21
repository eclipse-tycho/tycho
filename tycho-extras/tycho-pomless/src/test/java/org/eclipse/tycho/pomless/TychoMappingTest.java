/*******************************************************************************
 * Copyright (c) 2015, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to changed API
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.maven.polyglot.PolyglotModelManager;

public class TychoMappingTest {

    private PlexusContainer container;
    private PolyglotModelManager polyglotModelManager;

    @Before
    public void setUp() throws Exception {
        ContainerConfiguration config = new DefaultContainerConfiguration();
        config.setAutoWiring(true);
        config.setClassPathScanning(PlexusConstants.SCANNING_ON);
        container = new DefaultPlexusContainer(config);
        polyglotModelManager = container.lookup(PolyglotModelManager.class);
    }

    @After
    public void tearDown() {
        if (container != null) {
            container.dispose();
        }
    }

    @Test
    public void testLocateBuildProperties() throws Exception {
        File pom = polyglotModelManager.findPom(new File(getMappingTestDir(), "simple"));
        assertNotNull(pom);
        assertEquals(TychoBundleMapping.META_INF_DIRECTORY, pom.getName());
    }

    @Test
    public void testPriority() throws Exception {
        File pom = polyglotModelManager.findPom(new File(getMappingTestDir(), "precedence"));
        assertNotNull(pom);
        assertEquals("pom.xml must win over build.properties", "pom.xml", pom.getName());
    }

    private File getMappingTestDir() {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File("").getAbsolutePath();
        }
        return new File(basedir, "src/test/resources/mapping/");
    }

}
