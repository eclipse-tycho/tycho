/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;

@Named(org.eclipse.tycho.ArtifactType.TYPE_P2_MAVEN_REPOSITORY)
@Singleton
public class P2SiteProject extends AbstractArtifactBasedProject {
    @Override
    protected ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        return new DefaultArtifactKey(org.eclipse.tycho.ArtifactType.TYPE_P2_MAVEN_REPOSITORY, project.getArtifactId(),
                getOsgiVersion(project));
    }

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        for (MavenProject other : session.getProjects()) {
            if (isRequirement(other)) {
                Dependency dependency = new Dependency();
                dependency.setGroupId(other.getGroupId());
                dependency.setArtifactId(other.getArtifactId());
                dependency.setVersion(other.getVersion());
                project.getModel().addDependency(dependency);
            }
        }
    }

    private boolean isRequirement(MavenProject other) {
        String packaging = other.getPackaging();
        return "jar".equalsIgnoreCase(packaging) || "bundle".equalsIgnoreCase(packaging)
                || ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || ArtifactType.TYPE_ECLIPSE_FEATURE.equals(packaging)
                || ArtifactType.TYPE_BUNDLE_FRAGMENT.equals(packaging)
                || ArtifactType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
    }

}
