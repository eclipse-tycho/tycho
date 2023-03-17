/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.maven.SessionScoped;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.targetplatform.TargetDefinition;

@Component(role = TychoProjectManager.class)
@SessionScoped
public class TychoProjectManager {

    static final String CTX_TARGET_PLATFORM_CONFIGURATION = "TychoProjectManager/targetPlatformConfiguration";

    @Requirement(role = TychoProject.class)
    Map<String, TychoProject> projectTypes;

    @Requirement
    BundleReader bundleReader;

    @Requirement
    DefaultTargetPlatformConfigurationReader configurationReader;

    @Requirement
    LegacySupport legacySupport;

    @Requirement
    Logger logger;

    @Requirement
    ToolchainManager toolchainManager;

    private final Map<File, Optional<EclipseProject>> eclipseProjectCache = new ConcurrentHashMap<>();

    private final MavenSession mavenSession;

    @Inject
    public TychoProjectManager(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    public ExecutionEnvironmentConfiguration getExecutionEnvironmentConfiguration(MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        return reactorProject.computeContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION, () -> {
            TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(project);
            ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger,
                    !configuration.isResolveWithEEConstraints(), toolchainManager, mavenSession);
            TychoProject tychoProject = getTychoProject(project).orElse(null);
            if (tychoProject instanceof AbstractTychoProject atp) {
                atp.readExecutionEnvironmentConfiguration(reactorProject, mavenSession, eeConfiguration);
            } else {
                AbstractTychoProject.readExecutionEnvironmentConfiguration(configuration, eeConfiguration);
            }
            return eeConfiguration;
        });
    }

    public void readExecutionEnvironmentConfiguration(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        TargetPlatformConfiguration tpConfiguration = getTargetPlatformConfiguration(project);

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

    public TargetPlatformConfiguration getTargetPlatformConfiguration(MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        return reactorProject.computeContextValue(CTX_TARGET_PLATFORM_CONFIGURATION, () -> {
            TargetPlatformConfiguration configuration = configurationReader
                    .getTargetPlatformConfiguration(getMavenSession(), project);
            return configuration;
        });
    }

    public TargetPlatformConfiguration getTargetPlatformConfiguration(ReactorProject project) {

        return getTargetPlatformConfiguration(project.adapt(MavenProject.class));
    }

    public Optional<TychoProject> getTychoProject(MavenProject project) {
        if (project == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(projectTypes.get(project.getPackaging()));
    }

    public Optional<TychoProject> getTychoProject(ReactorProject project) {
        if (project == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(projectTypes.get(project.getPackaging()));
    }

    public Optional<ArtifactKey> getArtifactKey(MavenProject project) {
        return getArtifactKey(DefaultReactorProject.adapt(project));
    }

    public Optional<ArtifactKey> getArtifactKey(ReactorProject project) {
        return getTychoProject(project).map(tp -> tp.getArtifactKey(project));
    }

    public ArtifactKey getArtifactKey(Artifact artifact) {
        if (artifact instanceof ProjectArtifact) {
            ProjectArtifact projectArtifact = (ProjectArtifact) artifact;
            Optional<ArtifactKey> key = getArtifactKey(projectArtifact.getProject());
            if (key.isPresent()) {
                return key.get();
            }
        }
        try {
            OsgiManifest loadManifest = bundleReader.loadManifest(artifact.getFile());
            return loadManifest.toArtifactKey();
        } catch (OsgiManifestParserException e) {
            // not an bundle then...
        }
        return new DefaultArtifactKey("maven", artifact.getGroupId() + ":" + artifact.getArtifactId(),
                artifact.getVersion());
    }

    public Optional<EclipseProject> getEclipseProject(MavenProject project) {
        File projectFile = new File(project.getBasedir(), ".project");
        if (projectFile.isFile()) {
            return eclipseProjectCache.computeIfAbsent(projectFile, file -> {
                try {
                    return Optional.of(EclipseProject.parse(projectFile.toPath()));
                } catch (IOException e) {
                    logger.warn("Can't parse project file " + projectFile + ": " + e);
                    return Optional.empty();
                }
            });
        }
        return Optional.empty();
    }

    private MavenSession getMavenSession() {
        MavenSession session = legacySupport.getSession();
        if (session != null) {
            return session;
        }
        return mavenSession;
    }

}
