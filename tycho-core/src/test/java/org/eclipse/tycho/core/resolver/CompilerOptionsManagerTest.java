/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class CompilerOptionsManagerTest extends AbstractTychoMojoTestCase {

    private CompilerOptionsManager subject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        subject = lookup(CompilerOptionsManager.class);
    }

    public void testReadEmptyCompilerOptions() throws Exception {
        File basedir = getBasedir("projects/compilerOptions/empty");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        CompilerOptions result = subject.getCompilerOptions(projects.get(0));

        assertThat(0, is(result.getExtraRequirements().size()));
    }

    public void testReadCompilerOptions() throws Exception {
        File basedir = getBasedir("projects/compilerOptions/all");
        List<MavenProject> projects = getSortedProjects(basedir, null);

        CompilerOptions result = subject.getCompilerOptions(projects.get(0));

        assertThat(1, is(result.getExtraRequirements().size()));
        Dependency extraRequirement = result.getExtraRequirements().get(0);
        assertThat(extraRequirement.getType(), is("eclipse-plugin"));
        assertThat(extraRequirement.getArtifactId(), is("org.eclipse.osgi"));
    }

}
