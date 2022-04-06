/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

// TODO 353889 make ordering work - the deprecated LocalDependencyResolver doesn't order p2-installable-unit modules
public class P2IUTest extends AbstractTychoMojoTestCase {

    public void testIUDependencies() throws Exception {
        File basedir = getBasedir("iuBuildOrder/justIUs");
        List<MavenProject> projects = getSortedProjects(basedir);
        assertEquals("parent", projects.get(0).getArtifactId());
//        assertEquals("iuA", projects.get(1).getArtifactId());
//        assertEquals("iuB", projects.get(2).getArtifactId());
    }

    public void testFeatureToIUDependency() throws Exception {
        File basedir = getBasedir("iuBuildOrder/featureToIU");
        List<MavenProject> projects = getSortedProjects(basedir);
        assertEquals("parent", projects.get(0).getArtifactId());
//        assertEquals("anIU", projects.get(1).getArtifactId());
//        assertEquals("featureWithIUDeps", projects.get(2).getArtifactId());
    }

    public void testIUToFeature() throws Exception {
        File basedir = getBasedir("iuBuildOrder/iuToFeature");
        List<MavenProject> projects = getSortedProjects(basedir);
        assertEquals("parent", projects.get(0).getArtifactId());
//        assertEquals("aFeature", projects.get(1).getArtifactId());
//        assertEquals("anIU", projects.get(2).getArtifactId());
    }

}
