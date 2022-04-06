/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.model.PluginRef;

/**
 * Generates list of Maven dependencies from project OSGi/Eclipse dependencies
 * 
 * @deprecated Legacy implementation for the LocalTargetPlatformResolver; succeeded by
 *             {@link MavenDependencyInjector}
 */
@Deprecated
public class MavenDependencyCollector extends ArtifactDependencyVisitor {

    private MavenDependencyInjector injector;
    private final Logger logger;

    public MavenDependencyCollector(MavenProject project, BundleReader bundleReader, Logger logger) {
        this.injector = new MavenDependencyInjector(project, bundleReader, null, logger);
        this.logger = logger;
    }

    @Override
    public boolean visitFeature(FeatureDescription feature) {
        injector.addDependency(feature, Artifact.SCOPE_SYSTEM);
        return true; // keep visiting
    }

    @Override
    public void visitPlugin(PluginDescription plugin) {
        ReactorProject mavenProject = plugin.getMavenProject();
        if (mavenProject == null) {
            injector.addDependency(plugin, Artifact.SCOPE_SYSTEM);
        } else {
            injector.addDependency(plugin, Artifact.SCOPE_COMPILE);
        }
    }

    @Override
    public void missingPlugin(PluginRef ref, List<ArtifactDescriptor> walkback) {
        // we don't handle multi-environment target platforms well, so
        // missing environment specific bundles should not fail the build

        if (ref.getOs() == null && ref.getWs() == null && ref.getArch() == null) {
            super.missingPlugin(ref, walkback);
        } else {
            logger.warn("Missing environment specific bundle " + ref.toString());
        }
    }

}
