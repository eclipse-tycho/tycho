/*******************************************************************************
 * Copyright (c) 2015 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bachmann electronic GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Arrays;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

public class TychoMavenLifecycleParticipantTest {

    @Test
    public void validateConsistentTychoVersionWithSameVersion() throws MavenExecutionException {
        TychoMavenLifecycleParticipant tycho = new TychoMavenLifecycleParticipant(new SilentLog());
        MavenProject project = createProject();
        addTychoPlugin(project, "tycho-packaging-plugin", "0.22.0");
        addTychoPlugin(project, "tycho-versions-plugin", "0.22.0");

        tycho.validateConsistentTychoVersion(Arrays.asList(project));
    }

    @Test
    public void validateConsistentTychoVersionWithNullAsVersion() throws MavenExecutionException {
        TychoMavenLifecycleParticipant tycho = new TychoMavenLifecycleParticipant(new SilentLog());
        MavenProject project = createProject();
        addTychoPlugin(project, "tycho-packaging-plugin", null);
        addTychoPlugin(project, "tycho-versions-plugin", "0.23.0");

        tycho.validateConsistentTychoVersion(Arrays.asList(project));
    }

    @Test(expected = MavenExecutionException.class)
    public void validateConsistentTychoVersionWithDifferentVersionsInSameProject() throws MavenExecutionException {
        TychoMavenLifecycleParticipant tycho = new TychoMavenLifecycleParticipant(new SilentLog());
        MavenProject project = createProject();
        addTychoPlugin(project, "tycho-packaging-plugin", "0.22.0");
        addTychoPlugin(project, "tycho-versions-plugin", "0.23.0");

        tycho.validateConsistentTychoVersion(Arrays.asList(project));
    }

    @Test(expected = MavenExecutionException.class)
    public void validateConsistentTychoVersionWithDifferentVersionsInDifferentProjects() throws MavenExecutionException {
        TychoMavenLifecycleParticipant tycho = new TychoMavenLifecycleParticipant(new SilentLog());
        MavenProject project1 = createProject();
        addTychoPlugin(project1, "tycho-packaging-plugin", "0.22.0");
        MavenProject project2 = createProject();
        addTychoPlugin(project2, "tycho-versions-plugin", "0.23.0");

        tycho.validateConsistentTychoVersion(Arrays.asList(project1, project2));
    }

    private MavenProject createProject() {
        MavenProject project = new MavenProject();
        project.setBuild(new Build());
        return project;
    }

    private void addTychoPlugin(MavenProject project, String artifactId, String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.eclipse.tycho");
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        project.getBuild().addPlugin(plugin);
    }

}
