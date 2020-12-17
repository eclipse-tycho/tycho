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

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.model.ProductConfiguration;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_APPLICATION)
@Deprecated
public class EclipseApplicationProject extends AbstractArtifactBasedProject {
    @Override
    protected ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        final ProductConfiguration product = loadProduct(project);
        return new AbstractArtifactDependencyWalker(getDependencyArtifacts(project, environment),
                getEnvironments(project, environment)) {
            @Override
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

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        ProductConfiguration product = loadProduct(project);
        String id = product.getId() != null ? product.getId() : project.getArtifactId();
        String version = product.getVersion() != null ? product.getVersion() : getOsgiVersion(project);

        // TODO this is an invalid type constant for an ArtifactKey
        return new DefaultArtifactKey(PackagingType.TYPE_ECLIPSE_APPLICATION, id, version);
    }
}
