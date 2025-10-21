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

import javax.inject.Inject;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.testing.PlexusExtension;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.testing.PlexusTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.maven.polyglot.PolyglotModelManager;

@PlexusTest
public class TychoMappingTest implements PlexusTestConfiguration {

    @Inject
    private PolyglotModelManager polyglotModelManager;

    @Override
    public void customizeConfiguration(ContainerConfiguration configuration) {
        configuration.setAutoWiring(true);
        configuration.setClassPathScanning(PlexusConstants.SCANNING_ON);
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
        return new File(PlexusExtension.getBasedir(), "src/test/resources/mapping/");
    }

}
