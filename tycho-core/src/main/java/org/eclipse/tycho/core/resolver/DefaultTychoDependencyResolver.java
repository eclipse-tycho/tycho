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
package org.eclipse.tycho.core.resolver;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.resolver.DependencyVisitor;
import org.eclipse.tycho.resolver.TychoDependencyResolver;

@Component(role = TychoDependencyResolver.class)
public class DefaultTychoDependencyResolver implements TychoDependencyResolver {
    @Requirement
    private Logger logger;

    @Requirement
    private DefaultTargetPlatformConfigurationReader configurationReader;

    @Requirement
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get(project.getPackaging());
        if (dr == null) {
            return;
        }

        // generic Eclipse/OSGi metadata

        dr.setupProject(session, project);

        // p2 metadata

        Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.putAll(session.getSystemProperties()); // session wins
        properties.putAll(session.getUserProperties());
        project.setContextValue(TychoConstants.CTX_MERGED_PROPERTIES, properties);

        TargetPlatformConfiguration configuration = configurationReader
                .getTargetPlatformConfiguration(session, project);
        project.setContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration);

        TargetPlatformResolver resolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

        resolver.setupProjects(session, project, reactorProject);
    }

    public void resolveProject(MavenSession session, MavenProject project, List<ReactorProject> reactorProjects) {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get(project.getPackaging());
        if (dr == null) {
            return;
        }

        TargetPlatformResolver resolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

        logger.info("Resolving target platform for project " + project);
        DependencyArtifacts dependencyArtifacts = resolver.resolvePlatform(session, project, reactorProjects, null);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Resolved target platform for project ").append(project).append("\n");
            dependencyArtifacts.toDebugString(sb, "  ");
            logger.debug(sb.toString());
        }

        dr.setDependencyArtifacts(session, project, dependencyArtifacts);

        dr.resolveClassPath(session, project);

        resolver.injectDependenciesIntoMavenModel(project, dr, dependencyArtifacts, logger);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Injected dependencies for ").append(project.toString()).append("\n");
            for (Dependency dependency : project.getDependencies()) {
                sb.append("  ").append(dependency.toString());
            }
            logger.debug(sb.toString());
        }
    }

    public void traverse(MavenProject project, final DependencyVisitor visitor) {
        TychoProject tychoProject = projectTypes.get(project.getPackaging());
        if (tychoProject != null) {
            tychoProject.getDependencyWalker(project).walk(new ArtifactDependencyVisitor() {
                public void visitPlugin(org.eclipse.tycho.core.PluginDescription plugin) {
                    visitor.visit(plugin);
                };

                public boolean visitFeature(org.eclipse.tycho.core.FeatureDescription feature) {
                    return visitor.visit(feature);
                };
            });
        } else {
            // TODO do something!
        }
    }

}
