/*******************************************************************************
 * Copyright (c) 2015, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to changed API
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;

import org.codehaus.plexus.PlexusTestCase;
import org.sonatype.maven.polyglot.PolyglotModelManager;

public class TychoMappingTest extends PlexusTestCase {

    private PolyglotModelManager polyglotModelManager;

    @Override
    protected void setUp() throws Exception {
        polyglotModelManager = lookup(PolyglotModelManager.class);
    }

    public void testLocateBuildProperties() throws Exception {
        File pom = polyglotModelManager.findPom(new File(getMappingTestDir(), "simple"));
        assertNotNull(pom);
        assertEquals(TychoBundleMapping.META_INF_DIRECTORY, pom.getName());
    }

    public void testPriority() throws Exception {
        File pom = polyglotModelManager.findPom(new File(getMappingTestDir(), "precedence"));
        assertNotNull(pom);
        assertEquals("pom.xml must win over build.properties", "pom.xml", pom.getName());
    }

    private File getMappingTestDir() {
        return new File(getBasedir(), "src/test/resources/mapping/");
    }

}
