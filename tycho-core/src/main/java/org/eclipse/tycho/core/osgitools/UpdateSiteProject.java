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
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.model.UpdateSite;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_UPDATE_SITE)
@Deprecated
public class UpdateSiteProject extends AbstractArtifactBasedProject {

    @Override
    protected ArtifactDependencyWalker newDependencyWalker(MavenProject project, TargetEnvironment environment) {
        final UpdateSite site = loadSite(project);
        return new AbstractArtifactDependencyWalker(getDependencyArtifacts(project, environment),
                getEnvironments(project, environment)) {
            @Override
            public void walk(ArtifactDependencyVisitor visitor) {
                traverseUpdateSite(site, visitor);
            }
        };
    }

    private UpdateSite loadSite(MavenProject project) {
        File file = new File(project.getBasedir(), UpdateSite.SITE_XML);
        try {
            return UpdateSite.read(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read site.xml " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        // TODO this is an invalid type constant for an ArtifactKey
        return new DefaultArtifactKey(PackagingType.TYPE_ECLIPSE_UPDATE_SITE, project.getArtifactId(),
                getOsgiVersion(project));
    }
}
