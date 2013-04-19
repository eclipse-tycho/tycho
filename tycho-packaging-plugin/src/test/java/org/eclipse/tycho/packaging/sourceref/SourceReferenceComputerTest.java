/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging.sourceref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.packaging.SourceReferences;
import org.junit.Before;
import org.junit.Test;

public class SourceReferenceComputerTest {

    private SourceReferenceComputer sourceRefComputer;
    private Manifest manifest;

    @Before
    public void setUp() throws Exception {
        // TODO investigate why plexus lookup using PlexusTestCase no longer works 
        sourceRefComputer = new SourceReferenceComputer();
        sourceRefComputer.providerMap = new HashMap<String, SourceReferencesProvider>();
        sourceRefComputer.providerMap.put("dummy", new DummySourceReferencesProvider());
        manifest = new Manifest();
    }

    @Test
    public void testAddSourceReferenceDummyProvider() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(true, null), createProjectStub());
        assertEquals("scm:dummy:aDummySCMURL;path=\"dummy/path\"", getSourceRefsHeaderValue());
    }

    @Test
    public void testAddSourceReferenceCustomValue() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(true, "scm:myvalue"),
                createProjectStub());
        assertEquals("scm:myvalue", getSourceRefsHeaderValue());
    }

    @Test
    public void testAddSourceReferenceNoGenerate() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(false, null), createProjectStub());
        assertNull(getSourceRefsHeaderValue());
    }

    @Test
    public void testAddSourceReferenceNoProvider() {
        try {
            sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(true, null),
                    createProjectStub("scm:unknown:foo"));
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    private String getSourceRefsHeaderValue() {
        return manifest.getMainAttributes().getValue("Eclipse-SourceReferences");
    }

    private static SourceReferences createSourceRefConfig(boolean generate, String customValue) {
        SourceReferences sourceReferences = new SourceReferences();
        sourceReferences.setGenerate(generate);
        sourceReferences.setCustomValue(customValue);
        return sourceReferences;
    }

    private MavenProject createProjectStub() {
        return createProjectStub("scm:dummy:aDummySCMURL");
    }

    private MavenProject createProjectStub(String scmUrl) {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("tycho.scmUrl", scmUrl);
        return project;
    }
}
