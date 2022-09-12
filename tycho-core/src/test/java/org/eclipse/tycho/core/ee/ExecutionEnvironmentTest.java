/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc., and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.io.File;
import java.util.Optional;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class ExecutionEnvironmentTest extends AbstractTychoMojoTestCase {

    public void testTargetJRE() throws Exception {
        File basedir = getBasedir("projects/targetJRE");
        Optional<MavenProject> project = getSortedProjects(basedir).stream().filter(p -> p.getName().equals("bundle"))
                .findAny();
        assertEquals("JavaSE-1.7", TychoProjectUtils
                .getExecutionEnvironmentConfiguration(DefaultReactorProject.adapt(project.get())).getProfileName());
    }
}
