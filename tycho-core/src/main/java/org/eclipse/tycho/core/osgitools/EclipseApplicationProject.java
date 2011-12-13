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
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.model.ProductConfiguration;

@Component(role = TychoProject.class, hint = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_APPLICATION)
public class EclipseApplicationProject extends AbstractArtifactBasedProject {
    @Override
    protected ArtifactDependencyWalker newDependencyWalker(MavenProject project, TargetEnvironment environment) {
        final ProductConfiguration product = loadProduct(DefaultReactorProject.adapt(project));
        return new AbstractArtifactDependencyWalker(getDependencyArtifacts(project, environment), getEnvironments(project,
                environment)) {
            public void walk(ArtifactDependencyVisitor visitor) {
                traverseProduct(product, visitor);
            }
        };
    }

    protected ProductConfiguration loadProduct(final ReactorProject project) {
        File file = new File(project.getBasedir(), project.getArtifactId() + ".product");
        try {
            return ProductConfiguration.read(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read product configuration file " + file.getAbsolutePath(), e);
        }
    }

    public ArtifactKey getArtifactKey(ReactorProject project) {
        ProductConfiguration product = loadProduct(project);
        String id = product.getId() != null ? product.getId() : project.getArtifactId();
        String version = product.getVersion() != null ? product.getVersion() : getOsgiVersion(project);

        return new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_APPLICATION, id, version);
    }
}
