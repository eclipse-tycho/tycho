/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.model.Feature;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_FEATURE)
public class EclipseFeatureProject extends AbstractArtifactBasedProject {
    @Override
    protected ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        final File location = project.getBasedir();
        final Feature feature = Feature.loadFeature(location);
        return new AbstractArtifactDependencyWalker(getDependencyArtifacts(project, environment), getEnvironments(
                project, environment)) {
            @Override
            public void walk(ArtifactDependencyVisitor visitor) {
                traverseFeature(location, feature, visitor);
            }
        };
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        Feature feature = Feature.loadFeature(project.getBasedir());
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_FEATURE, feature.getId(), feature.getVersion());
    }

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        // validate feature.xml
        Feature.loadFeature(project.getBasedir());
    }

}
