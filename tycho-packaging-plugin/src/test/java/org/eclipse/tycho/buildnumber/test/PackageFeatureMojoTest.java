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
package org.eclipse.tycho.buildnumber.test;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.packaging.PackageFeatureMojo;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class PackageFeatureMojoTest extends AbstractTychoMojoTestCase {
    public void testFeatureXmlGeneration() throws Exception {
        File basedir = getBasedir("projects/featureXmlGeneration");
        File platform = new File("src/test/resources/eclipse");
        List<MavenProject> projects = getSortedProjects(basedir, platform);

        MavenProject project = getProject(projects, "featureXml.feature");

        PackageFeatureMojo mojo = (PackageFeatureMojo) lookupMojo("package-feature", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "session", newMavenSession(project, projects));

        mojo.execute();

        Feature feature = Feature.read(new File("target/projects/featureXmlGeneration/feature/target/feature.xml"));

        assertEquals("4.8.1.v20100302", feature.getPlugins().get(0).getVersion());
    }
}
