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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.targetplatform.TargetDefinition;

public abstract class AbstractTychoProject extends AbstractLogEnabled implements TychoProject {

    private static final String CTX_OSGI_BUNDLE_BASENAME = TychoConstants.CTX_BASENAME + "/tychoProject";
    private static final String CTX_MAVEN_SESSION = CTX_OSGI_BUNDLE_BASENAME + "/mavenSession";
    private static final String CTX_MAVEN_PROJECT = CTX_OSGI_BUNDLE_BASENAME + "/mavenProject";
    private static final String CTX_INITIAL_MAVEN_DEPENDENCIES = CTX_OSGI_BUNDLE_BASENAME + "/initialDependencies";

    @Requirement
    ProjectDependenciesResolver projectDependenciesResolver;

    @Override
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project) {
        return TychoProjectUtils.getDependencyArtifacts(project);
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

    public void setDependencyArtifacts(MavenSession session, ReactorProject project,
            DependencyArtifacts dependencyArtifacts) {
        project.setContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS, dependencyArtifacts);
    }

    public void setTestDependencyArtifacts(MavenSession session, ReactorProject project,
            DependencyArtifacts dependencyArtifacts) {
        project.setContextValue(TychoConstants.CTX_TEST_DEPENDENCY_ARTIFACTS, dependencyArtifacts);
    }

    public void setupProject(MavenSession session, MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        reactorProject.setContextValue(CTX_MAVEN_SESSION, session);
        reactorProject.setContextValue(CTX_MAVEN_PROJECT, project);
        reactorProject.setContextValue(CTX_INITIAL_MAVEN_DEPENDENCIES, List.copyOf(project.getDependencies()));
    }

    protected TargetEnvironment[] getEnvironments(ReactorProject project, TargetEnvironment environment) {
        if (environment != null) {
            return new TargetEnvironment[] { environment };
        }

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
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

    public void readExecutionEnvironmentConfiguration(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        TargetPlatformConfiguration tpConfiguration = TychoProjectUtils.getTargetPlatformConfiguration(project);

        String configuredForcedProfile = tpConfiguration.getExecutionEnvironment();
        if (configuredForcedProfile != null) {
            sink.overrideProfileConfiguration(configuredForcedProfile,
                    "target-platform-configuration <executionEnvironment>");
        } else {
            tpConfiguration.getTargets().stream() //
                    .map(TargetDefinition::getTargetEE) //
                    .filter(Objects::nonNull) //
                    .findFirst() //
                    .ifPresent(profile -> sink.overrideProfileConfiguration(profile,
                            "first targetJRE from referenced target-definition files"));
        }

        String configuredDefaultProfile = tpConfiguration.getExecutionEnvironmentDefault();
        if (configuredDefaultProfile != null) {
            sink.setProfileConfiguration(configuredDefaultProfile,
                    "target-platform-configuration <executionEnvironmentDefault>");
        }
    }

    @Override
    public Map<Dependency, Artifact> getInitialArtifactMap(ReactorProject reactorProject) {
        Object contextValue = reactorProject.getContextValue(CTX_INITIAL_MAVEN_DEPENDENCIES);
        if (contextValue instanceof Collection<?>) {
            @SuppressWarnings("unchecked")
            Collection<Dependency> dependencies = (Collection<Dependency>) contextValue;
            if (dependencies.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Dependency> initialDependencies = dependencies.stream()
                    .collect(Collectors.toMap(d -> getKey(d), Function.identity(), (a, b) -> a));
            Map<Dependency, Artifact> map = new HashMap<>();
            Collection<Artifact> artifacts = getProjectArtifacts(reactorProject);
            for (Artifact artifact : artifacts) {
                Dependency dependency = initialDependencies.get(getKey(artifact));
                if (dependency != null) {
                    map.put(dependency, artifact);
                }
            }
            return map;
        }
        return Collections.emptyMap();
    }

    private Collection<Artifact> getProjectArtifacts(ReactorProject project) {
        MavenProject mavenProject = getMavenProject(project);
        Set<Artifact> artifacts = mavenProject.getArtifacts();
        if (artifacts.isEmpty()) {
            MavenSession mavenSession = getMavenSession(project);
            try {
                return projectDependenciesResolver.resolve(mavenProject, List.of(Artifact.SCOPE_COMPILE), mavenSession);
            } catch (AbstractArtifactResolutionException e) {
                //can't do anything then...
                return Collections.emptyList();
            }
        }
        return artifacts;
    }

    protected static String getKey(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion() + ":"
                + dependency.getType() + ":" + Objects.requireNonNullElse(dependency.getClassifier(), "");
    }

    protected static String getKey(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":"
                + artifact.getType() + ":" + Objects.requireNonNullElse(artifact.getClassifier(), "");
    }

    protected static MavenSession getMavenSession(ReactorProject reactorProject) {
        return Objects.requireNonNull((MavenSession) reactorProject.getContextValue(CTX_MAVEN_SESSION),
                "Project not setup correctly");
    }

    protected static MavenProject getMavenProject(ReactorProject reactorProject) {
        return Objects.requireNonNull((MavenProject) reactorProject.getContextValue(CTX_MAVEN_PROJECT),
                "Project not setup correctly");
    }

}
