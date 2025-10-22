/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #460 - Delay classpath resolution to the compile phase 
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.p2resolver.PomReactorProjectFacade;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.osgi.framework.Filter;

public abstract class AbstractTychoProject implements TychoProject {

    private static final String CTX_OSGI_BUNDLE_BASENAME = TychoConstants.CTX_BASENAME + "/tychoProject";
    private static final String CTX_MAVEN_SESSION = CTX_OSGI_BUNDLE_BASENAME + "/mavenSession";
    private static final String CTX_MAVEN_PROJECT = CTX_OSGI_BUNDLE_BASENAME + "/mavenProject";
    private static final String CTX_INITIAL_MAVEN_DEPENDENCIES = CTX_OSGI_BUNDLE_BASENAME + "/initialDependencies";

    @Inject
    protected MavenDependenciesResolver projectDependenciesResolver;
    @Inject
    protected LegacySupport legacySupport;

    @Inject
    protected TychoProjectManager projectManager;

    @Inject
    protected Logger logger;

    @Inject
    @javax.inject.Named("p2")
    protected DependencyResolver dependencyResolver;

    @Override
    public DependencyArtifacts getDependencyArtifacts(MavenProject project) {
        return getDependencyArtifacts(DefaultReactorProject.adapt(project));
    }

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject reactorProject) {
        return reactorProject.computeContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS, () -> {
            if (logger != null) {
                logger.info("Resolving dependencies of " + reactorProject);
            }
            MavenSession mavenSession = getMavenSession(reactorProject);
            MavenProject mavenProject = getMavenProject(reactorProject);
            TargetPlatform preliminaryTargetPlatform = dependencyResolver.getPreliminaryTargetPlatform(mavenSession,
                    mavenProject);
            TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(mavenProject);
            DependencyResolverConfiguration resolverConfiguration = configuration.getDependencyResolverConfiguration();
            DependencyArtifacts dependencyArtifacts = dependencyResolver.resolveDependencies(mavenSession, mavenProject,
                    preliminaryTargetPlatform, resolverConfiguration, configuration.getEnvironments());
            if (logger != null) {
                if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(mavenSession, mavenProject)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Resolved target platform for ").append(reactorProject).append("\n");
                    dependencyArtifacts.toDebugString(sb, "  ");
                    logger.debug(sb.toString());
                }
            }
            return dependencyArtifacts;
        });

    }

    @Override
    public DependencyArtifacts getTestDependencyArtifacts(ReactorProject project) {
        return (DependencyArtifacts) project.getContextValue(TychoConstants.CTX_TEST_DEPENDENCY_ARTIFACTS);
    }

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project, TargetEnvironment environment) {
        DependencyArtifacts platform = getDependencyArtifacts(project);

        if (environment != null && platform instanceof MultiEnvironmentDependencyArtifacts multiEnvArtifacts) {
            platform = multiEnvArtifacts.getPlatform(environment);
            if (platform == null) {
                throw new IllegalStateException("Unsupported runtime environment " + environment.toString()
                        + " for project " + project.toString());
            }
        }

        return platform;
    }

    public void setupProject(MavenSession session, MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        reactorProject.setContextValue(CTX_MAVEN_SESSION, session);
        reactorProject.setContextValue(CTX_MAVEN_PROJECT, project);
        reactorProject.setContextValue(CTX_INITIAL_MAVEN_DEPENDENCIES,
                List.copyOf(collectInitial(project, new HashMap<>()).values()));
    }

    private Map<String, Dependency> collectInitial(MavenProject project, Map<String, Dependency> map) {
        for (Dependency dependency : project.getDependencies()) {
            map.putIfAbsent(dependency.getManagementKey(), dependency);
        }
        MavenProject parent = project.getParent();
        if (parent != null) {
            return collectInitial(parent, map);
        }
        return map;
    }

    protected TargetEnvironment[] getEnvironments(ReactorProject project, TargetEnvironment environment) {
        if (environment != null) {
            return new TargetEnvironment[] { environment };
        }

        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        if (configuration.isImplicitTargetEnvironment()) {
            return null; // any
        }

        // all specified
        List<TargetEnvironment> environments = configuration.getEnvironments();
        return environments.toArray(new TargetEnvironment[environments.size()]);
    }

    @Override
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project) {
        return null;
    }

    @Override
    public Filter getTargetEnvironmentFilter(MavenProject project) {
        return null;
    }

    public void readExecutionEnvironmentConfiguration(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        readExecutionEnvironmentConfiguration(projectManager.getTargetPlatformConfiguration(project), sink);
    }

    public static void readExecutionEnvironmentConfiguration(TargetPlatformConfiguration targetPlatformConfiguration,
            ExecutionEnvironmentConfiguration executionEnvironmentConfiguration) {

        String configuredForcedProfile = targetPlatformConfiguration.getExecutionEnvironment();
        if (configuredForcedProfile != null) {
            executionEnvironmentConfiguration.overrideProfileConfiguration(configuredForcedProfile,
                    "target-platform-configuration <executionEnvironment>");
        } else {
            targetPlatformConfiguration.getTargets().stream() //
                    .map(TargetDefinition::getTargetEE) //
                    .filter(Objects::nonNull) //
                    .findFirst() //
                    .ifPresent(profile -> executionEnvironmentConfiguration.overrideProfileConfiguration(profile,
                            "first targetJRE from referenced target-definition files"));
        }

        String configuredDefaultProfile = targetPlatformConfiguration.getExecutionEnvironmentDefault();
        if (configuredDefaultProfile != null) {
            executionEnvironmentConfiguration.setProfileConfiguration(configuredDefaultProfile,
                    "target-platform-configuration <executionEnvironmentDefault>");
        }
    }

    @Override
    public Collection<Artifact> getInitialArtifacts(ReactorProject reactorProject, Collection<String> scopes) {
        Object contextValue = reactorProject.getContextValue(CTX_INITIAL_MAVEN_DEPENDENCIES);
        if (contextValue instanceof Collection<?>) {
            @SuppressWarnings("unchecked")
            Collection<Dependency> dependencies = (Collection<Dependency>) contextValue;
            if (dependencies.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, ReactorProject> reactorProjectMap = getReactorProjectMap(reactorProject);
            Stream<Artifact> projectArtifacts = streamProjectArtifacts(reactorProject, dependencies, scopes)
                    .map(artifact -> {
                        String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(),
                                artifact.getBaseVersion());
                        ReactorProject artifactReactorProject = reactorProjectMap.get(key);
                        if (artifactReactorProject != null) {
                            MavenProject mavenProject = artifactReactorProject.adapt(MavenProject.class);
                            if (mavenProject != null) {
                                return mavenProject.getArtifact();
                            }
                        }
                        return artifact;

                    });
            return projectArtifacts.toList();
        }
        return Collections.emptyList();
    }

    private Stream<Artifact> streamProjectArtifacts(ReactorProject project, Collection<Dependency> dependencies,
            Collection<String> scopes) {
        MavenProject mavenProject = getMavenProject(project);
        MavenSession mavenSession = getMavenSession(project);
        try {
            return projectDependenciesResolver.resolve(mavenProject, dependencies, scopes, mavenSession).stream();
        } catch (DependencyCollectionException e) {
            return Stream.empty();
        } catch (DependencyResolutionException e) {
            return Stream.empty();
        }
    }

    @Override
    public Map<Artifact, IArtifactFacade> getArtifactFacades(ReactorProject reactorProject,
            Collection<Artifact> artifacts) {
        Map<String, ReactorProject> reactorProjectMap = getReactorProjectMap(reactorProject);
        Map<Artifact, IArtifactFacade> resultMap = new HashMap<>();
        for (Artifact artifact : artifacts) {
            String key = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
            if (reactorProjectMap.containsKey(key)) {
                PomReactorProjectFacade reactorFacade = new PomReactorProjectFacade(artifact,
                        reactorProjectMap.get(key));
                resultMap.put(artifact, reactorFacade);
            } else {
                MavenArtifactFacade externalFacade = new MavenArtifactFacade(artifact);
                resultMap.put(artifact, externalFacade);
            }
        }
        return resultMap;
    }

    private Map<String, ReactorProject> getReactorProjectMap(ReactorProject reactorProject) {
        MavenSession session = getMavenSession(reactorProject);
        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        Map<String, ReactorProject> reactorProjectMap = new HashMap<>();
        for (ReactorProject p : reactorProjects) {
            reactorProjectMap.put(ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion()), p);
        }
        return reactorProjectMap;
    }

    protected static String getKey(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion() + ":"
                + dependency.getType() + ":" + Objects.requireNonNullElse(dependency.getClassifier(), "");
    }

    protected static String getKey(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":"
                + artifact.getType() + ":" + Objects.requireNonNullElse(artifact.getClassifier(), "");
    }

    protected MavenSession getMavenSession(ReactorProject reactorProject) {
        MavenSession mavenSession = (MavenSession) reactorProject.getContextValue(CTX_MAVEN_SESSION);
        if (mavenSession == null) {
            return Objects.requireNonNull(legacySupport.getSession(),
                    "Project not setup correctly, neither context nor adaption works here!");
        }
        return mavenSession;
    }

    protected static MavenProject getMavenProject(ReactorProject reactorProject) {
        MavenProject contextValue = (MavenProject) reactorProject.getContextValue(CTX_MAVEN_PROJECT);
        if (contextValue == null) {
            MavenProject adapt = reactorProject.adapt(MavenProject.class);
            return Objects.requireNonNull(adapt,
                    "Project not setup correctly, neither context nor adaption works here!");
        }
        return contextValue;
    }

}
