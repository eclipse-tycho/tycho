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
package org.eclipse.tycho.extras.custombundle;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator.OptionalResolutionAction;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

/**
 * This component is invoked during Tycho dependency resolution and provides P2 metadata that
 * describes artifacts that will be created by custom-bundle goal.
 */
@Component(role = P2MetadataProvider.class, hint = "org.eclipse.tycho.extras.custombundle.CustomBundleP2MetadataProvider")
public class CustomBundleP2MetadataProvider implements P2MetadataProvider, Initializable {
    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator generator;

    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) project
                .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
        OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

        if (TargetPlatformConfiguration.OPTIONAL_RESOLUTION_IGNORE.equals(configuration.getOptionalResolutionAction())) {
            optionalAction = OptionalResolutionAction.IGNORE;
        }

        Plugin plugin = project.getPlugin("org.eclipse.tycho.extras:tycho-custom-bundle-plugin");
        if (plugin != null) {
            // it is possible to configure manifest location at <plugin> level, but it does not make sense to do so
            for (PluginExecution execution : plugin.getExecutions()) {
                File location = getBundleLocation(execution);
                String classifier = getClassifier(execution);
                if (location != null && classifier != null) {
                    IArtifactFacade artifact = new AttachedArtifact(project, location, classifier);
                    IDependencyMetadata metadata = generator.generateMetadata(artifact, null, optionalAction);
                    reactorProject.setDependencyMetadata(classifier, false, metadata.getMetadata());
                }
            }
        }
    }

    private String getClassifier(PluginExecution execution) {
        Xpp3Dom cfg = (Xpp3Dom) execution.getConfiguration();
        if (cfg == null) {
            return null;
        }
        Xpp3Dom classifierDom = cfg.getChild("classifier");
        if (classifierDom == null) {
            return null;
        }
        return classifierDom.getValue();
    }

    private File getBundleLocation(PluginExecution execution) {
        Xpp3Dom cfg = (Xpp3Dom) execution.getConfiguration();
        if (cfg == null) {
            return null;
        }
        Xpp3Dom locationDom = cfg.getChild("bundleLocation");
        if (locationDom == null) {
            return null;
        }
        return new File(locationDom.getValue());
    }

    public void initialize() throws InitializationException {
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
    }

}
