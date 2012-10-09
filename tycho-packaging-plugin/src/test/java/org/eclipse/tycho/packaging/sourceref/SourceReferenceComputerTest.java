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

import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.packaging.SourceReferences;

public class SourceReferenceComputerTest extends PlexusTestCase {

    private SourceReferenceComputer sourceRefComputer;
    private Manifest manifest;

    @Override
    protected void setUp() throws Exception {
        sourceRefComputer = lookup(SourceReferenceComputer.class);
        manifest = new Manifest();
    }

    public void testAddSourceReferenceDummyProvider() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(true, null), createProjectStub());
        assertEquals("scm:dummy:aDummySCMURL;path=\"dummy/path\"", getSourceRefsHeaderValue());
    }

    public void testAddSourceReferenceCustomValue() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(true, "scm:myvalue"),
                createProjectStub());
        assertEquals("scm:myvalue", getSourceRefsHeaderValue());
    }

    public void testAddSourceReferenceNoGenerate() throws Exception {
        sourceRefComputer.addSourceReferenceHeader(manifest, createSourceRefConfig(false, null), createProjectStub());
        assertNull(getSourceRefsHeaderValue());
    }

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
