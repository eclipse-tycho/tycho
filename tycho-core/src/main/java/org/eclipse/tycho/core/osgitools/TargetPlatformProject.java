/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION)
public class TargetPlatformProject extends AbstractTychoProject {

    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project) {
        return new ArtifactDependencyWalker() {

            @Override
            public void walk(ArtifactDependencyVisitor visitor) {

            }

            @Override
            public void traverseFeature(File location, Feature feature, ArtifactDependencyVisitor visitor) {

            }
        };
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        return new DefaultArtifactKey("target", project.getArtifactId(), project.getVersion());
    }

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject reactorProject) {
        return reactorProject.computeContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS, () -> {
            DefaultDependencyArtifacts artifacts = new DefaultDependencyArtifacts(reactorProject);
            MavenSession mavenSession = getMavenSession(reactorProject);
            MavenProject mavenProject = getMavenProject(reactorProject);
            TargetPlatform targetPlatform = dependencyResolver.getPreliminaryTargetPlatform(mavenSession, mavenProject);
            if (targetPlatform instanceof P2TargetPlatform p2) {
                Set<IInstallableUnit> installableUnits = p2.getInstallableUnits();
                for (IInstallableUnit iu : installableUnits) {
                    for (IArtifactKey key : iu.getArtifacts()) {
                        ArtifactKey artifactKey = ArtifactTypeHelper.toTychoArtifactKey(iu, key);
                        artifacts.addArtifactFile(artifactKey, () -> targetPlatform.getArtifactLocation(artifactKey),
                                List.of(iu));
                    }
                }
            }
            return artifacts;
        });
    }

}
