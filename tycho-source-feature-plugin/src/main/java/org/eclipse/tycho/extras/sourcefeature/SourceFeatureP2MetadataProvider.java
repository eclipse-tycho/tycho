/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator.OptionalResolutionAction;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class, hint = "org.eclipse.tycho.extras.sourcefeature.SourceFeatureP2MetadataProvider")
public class SourceFeatureP2MetadataProvider implements P2MetadataProvider, Initializable {
    @Requirement
    private Logger log;

    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator generator;

    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        File template = new File(project.getBasedir(), SourceFeatureMojo.FEATURE_TEMPLATE_DIR);

        if (!ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || !template.isDirectory()) {
            return;
        }

        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

        if (TargetPlatformConfiguration.OPTIONAL_RESOLUTION_IGNORE.equals(configuration.getOptionalResolutionAction())) {
            optionalAction = OptionalResolutionAction.IGNORE;
        }

        Plugin plugin = project.getPlugin("org.eclipse.tycho.extras:tycho-source-feature-plugin");
        if (plugin != null) {
            try {
                Feature sourceFeature = SourceFeatureMojo.getSourceFeature(project);

                String classifier = SourceFeatureMojo.SOURCES_FEATURE_CLASSIFIER;
                File sourceFeatureBasedir = new File(project.getBuild().getDirectory(), classifier);
                sourceFeatureBasedir.mkdirs();

                Feature.write(sourceFeature, new File(sourceFeatureBasedir, Feature.FEATURE_XML));

                IArtifactFacade artifact = new AttachedArtifact(project, sourceFeatureBasedir, classifier);
                IDependencyMetadata metadata = generator.generateMetadata(artifact, null, optionalAction);
                reactorProject.setDependencyMetadata(classifier, true, metadata.getMetadata());
            } catch (IOException e) {
                log.error("Could not create sources feature.xml", e);
            }
        }
    }

    public void initialize() throws InitializationException {
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
    }

}
