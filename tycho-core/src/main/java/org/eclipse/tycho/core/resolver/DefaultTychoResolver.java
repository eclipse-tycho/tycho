/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 532575
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.resolver.DependencyVisitor;
import org.eclipse.tycho.resolver.TychoResolver;

@Component(role = TychoResolver.class)
public class DefaultTychoResolver implements TychoResolver {

    @Requirement
    private Logger logger;

    @Requirement
    private DefaultTargetPlatformConfigurationReader configurationReader;

    @Requirement
    private DefaultDependencyResolverFactory dependencyResolverLocator;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Requirement
    private ToolchainManager toolchainManager;

    public static final String TYCHO_ENV_OSGI_WS = "tycho.env.osgi.ws";
    public static final String TYCHO_ENV_OSGI_OS = "tycho.env.osgi.os";
    public static final String TYCHO_ENV_OSGI_ARCH = "tycho.env.osgi.arch";
    public static final String PROPERTY_PREFIX = "pom.model.property.";

    @Override
    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject) {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get(project.getPackaging());
        if (dr == null) {
            return;
        }

        // skip if setup was already done
        if (reactorProject.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES) != null) {
            return;
        }

        // generic Eclipse/OSGi metadata

        dr.setupProject(session, project);

        // p2 metadata

        Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.putAll(session.getSystemProperties()); // session wins
        properties.putAll(session.getUserProperties());
        reactorProject.setContextValue(TychoConstants.CTX_MERGED_PROPERTIES, properties);

        setTychoEnvironmentProperties(properties, project);
        setBuildProperties(project);

        TargetPlatformConfiguration configuration = configurationReader.getTargetPlatformConfiguration(session,
                project);
        reactorProject.setContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration);

        ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger,
                !configuration.isResolveWithEEConstraints(), toolchainManager, session);
        dr.readExecutionEnvironmentConfiguration(reactorProject, session, eeConfiguration);
        reactorProject.setContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION, eeConfiguration);

        DependencyResolver resolver = dependencyResolverLocator.lookupDependencyResolver(project);
        resolver.setupProjects(session, project, reactorProject);
    }

    @Override
    public void resolveProject(MavenSession session, MavenProject project, List<ReactorProject> reactorProjects) {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get(project.getPackaging());
        if (dr == null) {
            return;
        }

        DependencyResolver resolver = dependencyResolverLocator.lookupDependencyResolver(project);

        logger.info("Computing target platform for " + project);
        TargetPlatform preliminaryTargetPlatform = resolver.computePreliminaryTargetPlatform(session, project,
                reactorProjects);

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);

        DependencyResolverConfiguration resolverConfiguration = configuration.getDependencyResolverConfiguration();

        logger.info("Resolving dependencies of " + project);
        DependencyArtifacts dependencyArtifacts = resolver.resolveDependencies(session, project,
                preliminaryTargetPlatform, reactorProjects, resolverConfiguration);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Resolved target platform for ").append(project).append("\n");
            dependencyArtifacts.toDebugString(sb, "  ");
            logger.debug(sb.toString());
        }

        dr.setDependencyArtifacts(session, reactorProject, dependencyArtifacts);
        /*
         * TODO At the moment, we don't resolve test-specific deps, so we set it to common deps.
         * This will be refined later with addition of support of test-specific deps from
         * .classpath.
         */
        final DependencyArtifacts testDependencyArtifacts = dependencyArtifacts;
        dr.setTestDependencyArtifacts(session, reactorProject, testDependencyArtifacts);

        logger.info("Resolving class path of " + project);
        dr.resolveClassPath(session, project);

        resolver.injectDependenciesIntoMavenModel(project, dr, dependencyArtifacts, testDependencyArtifacts, logger);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Injected dependencies for ").append(project.toString()).append("\n");
            for (Dependency dependency : project.getDependencies()) {
                sb.append("  ").append(dependency.toString());
            }
            logger.debug(sb.toString());
        }
    }

    @Override
    public void traverse(MavenProject project, final DependencyVisitor visitor) {
        TychoProject tychoProject = projectTypes.get(project.getPackaging());
        if (tychoProject != null) {
            tychoProject.getDependencyWalker(DefaultReactorProject.adapt(project))
                    .walk(new ArtifactDependencyVisitor() {
                        @Override
                        public void visitPlugin(org.eclipse.tycho.core.PluginDescription plugin) {
                            visitor.visit(plugin);
                        }

                        @Override
                        public boolean visitFeature(org.eclipse.tycho.core.FeatureDescription feature) {
                            return visitor.visit(feature);
                        }
                    });
        } else {
            // TODO do something!
        }
    }

    protected void setTychoEnvironmentProperties(Properties properties, MavenProject project) {
        String arch = PlatformPropertiesUtils.getArch(properties);
        String os = PlatformPropertiesUtils.getOS(properties);
        String ws = PlatformPropertiesUtils.getWS(properties);
        project.getProperties().put(TYCHO_ENV_OSGI_WS, ws);
        project.getProperties().put(TYCHO_ENV_OSGI_OS, os);
        project.getProperties().put(TYCHO_ENV_OSGI_ARCH, arch);
    }

    protected void setBuildProperties(MavenProject project) {
        File pomfile = project.getFile();
        if (pomfile != null) {
            File buildPropertiesFile = new File(pomfile.getParentFile(), "build.properties");
            if (buildPropertiesFile.isFile() && buildPropertiesFile.length() > 0) {
                Properties buildProperties = new Properties();
                try {
                    try (FileInputStream stream = new FileInputStream(buildPropertiesFile)) {
                        buildProperties.load(stream);
                    }
                    Properties projectProperties = project.getProperties();
                    buildProperties.stringPropertyNames().forEach(key -> {
                        if (key.startsWith(PROPERTY_PREFIX)) {
                            projectProperties.setProperty(key.substring(PROPERTY_PREFIX.length()),
                                    buildProperties.getProperty(key));
                        }
                    });
                } catch (IOException e) {
                    logger.warn("reading build.properties from project " + project.getName() + " failed", e);
                }
            }
        }
    }

}
