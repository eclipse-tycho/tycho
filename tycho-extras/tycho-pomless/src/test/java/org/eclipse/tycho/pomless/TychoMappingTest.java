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

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.sonatype.maven.polyglot.PolyglotModelManager;

@PlexusTest
public class TychoMappingTest {

    @Inject
    private PolyglotModelManager polyglotModelManager;

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
        assertEquals("pom.xml", pom.getName(), "pom.xml must win over build.properties");
    }

    private File getMappingTestDir() {
        return new File(getBasedir(), "src/test/resources/mapping/");
    }

}
