/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
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
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.helper.PluginRealmHelper;
import org.eclipse.tycho.model.project.EclipseProject;
import org.eclipse.tycho.targetplatform.TargetDefinition;

import aQute.bnd.osgi.Processor;

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

    @Requirement
    PluginRealmHelper pluginRealmHelper;

    @Requirement
    MavenBundleResolver mavenBundleResolver;

    @Requirement
    TargetPlatformService targetPlatformService;

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

    public void readExecutionEnvironmentConfiguration(ReactorProject project, ExecutionEnvironmentConfiguration sink) {
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
        return reactorProject.computeContextValue(CTX_TARGET_PLATFORM_CONFIGURATION,
                () -> configurationReader.getTargetPlatformConfiguration(getMavenSession(), project));
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
        if (artifact instanceof ProjectArtifact projectArtifact) {
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
        return eclipseProjectCache.computeIfAbsent(projectFile, file -> {
            if (file.isFile()) {
                try {
                    return Optional.of(EclipseProject.parse(file.toPath()));
                } catch (IOException e) {
                    logger.warn("Can't parse project file " + file, e);
                }
            }
            return Optional.empty();
        });
    }

    private MavenSession getMavenSession() {
        MavenSession session = legacySupport.getSession();
        return session != null ? session : mavenSession;
    }

    public Optional<Processor> getBndTychoProject(MavenProject project) {
        Optional<TychoProject> tychoProject = getTychoProject(project);
        if (tychoProject.isEmpty()) {
            return Optional.empty();
        }
        File bndFile = new File(project.getBasedir(), TychoConstants.PDE_BND);
        if (bndFile.exists()) {
            Processor processor = new Processor();
            processor.setProperties(bndFile);
            return Optional.of(processor);
        }
        return Optional.empty();
    }

    /**
     * Determine the list of dependencies for a given project as a collection of path items
     * 
     * @param project
     *            the project to use to determine the dependencies
     * @return a Collection of pathes describing the project dependencies
     * @throws Exception
     */
    public Collection<Path> getProjectDependencies(MavenProject project) throws Exception {
        Set<Path> dependencySet = new HashSet<>();
        TychoProject tychoProject = getTychoProject(project).get();
        List<ArtifactDescriptor> dependencies = tychoProject
                .getDependencyArtifacts(DefaultReactorProject.adapt(project)).getArtifacts();
        for (ArtifactDescriptor descriptor : dependencies) {
            File location = descriptor.fetchArtifact().get();
            if (location.equals(project.getBasedir())) {
                continue;
            }
            ReactorProject reactorProject = descriptor.getMavenProject();
            if (reactorProject == null) {
                writeLocation(location, dependencySet);
            } else {
                writeLocation(reactorProject.getArtifact(descriptor.getClassifier()), dependencySet);
            }
        }
        if (tychoProject instanceof OsgiBundleProject bundleProject) {
            MavenSession session = getMavenSession();
            pluginRealmHelper.visitPluginExtensions(project, session, ClasspathContributor.class, cpc -> {
                List<ClasspathEntry> list = cpc.getAdditionalClasspathEntries(project, Artifact.SCOPE_COMPILE);
                if (list != null && !list.isEmpty()) {
                    for (ClasspathEntry entry : list) {
                        for (File locations : entry.getLocations()) {
                            writeLocation(locations, dependencySet);
                        }
                    }
                }
            });
            // This is a hack because "org.eclipse.osgi.services" exports the annotation
            // package and might then be resolved by Tycho as a dependency, but then PDE
            // can't find the annotations here, so we always add this as a dependency
            // manually here, once "org.eclipse.osgi.services" is gone we can remove this
            // again!
            Optional<ResolvedArtifactKey> bundle = mavenBundleResolver.resolveMavenBundle(project, session, "org.osgi",
                    "org.osgi.service.component.annotations", "1.3.0");
            bundle.ifPresent(key -> {
                writeLocation(key.getLocation(), dependencySet);
            });
        }
        return dependencySet;
    }

    private void writeLocation(File location, Collection<Path> consumer) {
        if (location == null) {
            return;
        }
        consumer.add(location.getAbsoluteFile().toPath());
    }

    public Optional<TargetPlatform> getTargetPlatform(MavenProject project) {
        return targetPlatformService.getTargetPlatform(DefaultReactorProject.adapt(project));

    }

}
