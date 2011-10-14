/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.test;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class TychoBuildOrderComponentTest extends AbstractTychoMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testModuleOrderOfBundleFeatureAndSite() throws Exception {
        List<MavenProject> projects = getSortedProjects(getBasedir("projects/moduleorder"), null);

        assertEquals(5, projects.size());
        int ix = 1;
        assertEquals("moduleorder.p002", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p001", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p004", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p003", projects.get(ix++).getArtifactId());
    }

    public void testModuleOrderOfProduct() throws Exception {
        List<MavenProject> projects = getSortedProjects(getBasedir("projects/moduleorder.product"), null);

        assertEquals(3, projects.size());

        int ix = 1;
        assertEquals("moduleorder.p002", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p005", projects.get(ix++).getArtifactId());
    }

    public void testModuleOrderOfModuleWithMultipleMarkers() throws Exception {
        List<MavenProject> projects = getSortedProjects(getBasedir("projects/moduleorder.combinedModule"), null);

        assertEquals(4, projects.size());

        int ix = 1;
        assertEquals("moduleorder.p002", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p006", projects.get(ix++).getArtifactId());
        assertEquals("moduleorder.p007", projects.get(ix++).getArtifactId());
    }
}
