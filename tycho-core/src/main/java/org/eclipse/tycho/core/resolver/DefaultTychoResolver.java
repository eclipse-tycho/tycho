/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 532575
 *    Christoph Läubrich - Issue #460 - Delay classpath resolution to the compile phase 
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
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
        String threadMarker;
        if (logger.isDebugEnabled()) {
            threadMarker = "[" + Thread.currentThread().getName().replaceAll("^ForkJoinPool-(\\d+)-", "") + "] ";
        } else {
            threadMarker = "";
        }
        logger.debug(threadMarker + "Computing preliminary target platform for " + project);
        TargetPlatform preliminaryTargetPlatform = resolver.computePreliminaryTargetPlatform(session, project,
                reactorProjects);

        logger.info(threadMarker + "Resolving dependencies of " + project);
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(reactorProject);

        DependencyResolverConfiguration resolverConfiguration = configuration.getDependencyResolverConfiguration();

        DependencyArtifacts dependencyArtifacts = resolver.resolveDependencies(session, project,
                preliminaryTargetPlatform, reactorProjects, resolverConfiguration);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder(threadMarker);
            sb.append("Resolved target platform for ").append(project).append("\n");
            dependencyArtifacts.toDebugString(sb, "  ");
            logger.debug(sb.toString());
        }

        dr.setDependencyArtifacts(session, reactorProject, dependencyArtifacts);

        DependencyArtifacts testDependencyArtifacts = null;
        TychoProject tychoProjectType = projectTypes.get(project.getPackaging());
        if (tychoProjectType instanceof BundleProject) {
            List<ArtifactKey> testDependencies = ((BundleProject) tychoProjectType)
                    .getExtraTestRequirements(reactorProject);
            if (!testDependencies.isEmpty()) {
                logger.info(threadMarker + "Resolving test dependencies of " + project);
                DependencyResolverConfiguration testResolverConfiguration = new DependencyResolverConfiguration() {
                    @Override
                    public OptionalResolutionAction getOptionalResolutionAction() {
                        return resolverConfiguration.getOptionalResolutionAction();
                    }

                    @Override
                    public List<ArtifactKey> getExtraRequirements() {
                        ArrayList<ArtifactKey> res = new ArrayList<>(resolverConfiguration.getExtraRequirements());
                        res.addAll(testDependencies);
                        return res;
                    }
                };
                testDependencyArtifacts = resolver.resolveDependencies(session, project, preliminaryTargetPlatform,
                        reactorProjects, testResolverConfiguration);
            }
            dr.setTestDependencyArtifacts(session, reactorProject,
                    Objects.requireNonNullElse(testDependencyArtifacts, new DefaultDependencyArtifacts()));
        }

        resolver.injectDependenciesIntoMavenModel(project, dr, dependencyArtifacts, testDependencyArtifacts, logger);

        if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            StringBuilder sb = new StringBuilder(threadMarker);
            sb.append("Injected dependencies for ").append(project.toString()).append("\n");
            for (Dependency dependency : project.getDependencies()) {
                sb.append("  ").append(dependency.toString());
            }
            logger.debug(sb.toString());
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
}
