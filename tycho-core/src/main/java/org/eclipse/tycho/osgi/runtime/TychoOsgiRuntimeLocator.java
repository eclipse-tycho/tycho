/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator;
import org.eclipse.tycho.dev.DevWorkspaceResolver;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

/**
 * Implementation of {@link org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator} for Tycho's
 * OSGi runtime.
 */
@Component(role = EquinoxRuntimeLocator.class)
public class TychoOsgiRuntimeLocator implements EquinoxRuntimeLocator {
    /**
     * List of packages exported by the shared bundles. The shared bundles are loaded by the Maven
     * class loader (see transitive dependencies of <code>tycho-maven-plugin</code>) but their
     * classes are also exposed to the implementation bundles in in Tycho's OSGi runtime (see
     * {@link TychoOsgiRuntimeArtifacts#getRuntimeArtifacts()}) via the system packages extra
     * option.
     */
    private static final String[] SYSTEM_PACKAGES_EXTRA = { "org.eclipse.tycho", // 
            "org.eclipse.tycho.artifacts", //
            "org.eclipse.tycho.core.ee.shared", //
            "org.eclipse.tycho.core.shared", //
            "org.eclipse.tycho.core.resolver.shared", //
            "org.eclipse.tycho.locking.facade", //
            "org.eclipse.tycho.p2.metadata", //
            "org.eclipse.tycho.p2.repository", //
            "org.eclipse.tycho.p2.resolver.facade", //
            "org.eclipse.tycho.p2.target.facade", //
            "org.eclipse.tycho.p2.tools", //
            "org.eclipse.tycho.p2.tools.director.shared", //
            "org.eclipse.tycho.p2.tools.publisher.facade", //
            "org.eclipse.tycho.p2.tools.mirroring.facade", //
            "org.eclipse.tycho.p2.tools.verifier.facade", //
            "org.eclipse.tycho.repository.registry.facade",//
            "org.eclipse.tycho.p2.tools.baseline.facade" };

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private LegacySupport buildContext;

    @Requirement(hint = "zip")
    private UnArchiver unArchiver;

    @Requirement
    private FileLockService fileLockService;

    @Requirement
    private Map<String, TychoOsgiRuntimeArtifacts> runtimeArtifacts;

    @Requirement
    private DevWorkspaceResolver workspaceState;

    @Override
    public void locateRuntime(EquinoxRuntimeDescription description, boolean forked) throws MavenExecutionException {
        WorkspaceTychoOsgiRuntimeLocator workspaceLocator = WorkspaceTychoOsgiRuntimeLocator
                .getResolver(this.workspaceState);

        MavenSession session = buildContext.getSession();

        TychoOsgiRuntimeArtifacts framework = runtimeArtifacts.get(TychoOsgiRuntimeArtifacts.HINT_FRAMEWORK);
        if (framework != null) {
            addRuntimeArtifacts(workspaceLocator, description, session, framework);
        }
        if (forked) {
            TychoOsgiRuntimeArtifacts shared = runtimeArtifacts.get(TychoOsgiRuntimeArtifacts.HINT_SHARED);
            if (framework != null) {
                addRuntimeArtifacts(workspaceLocator, description, session, shared);
            }
        } else {
            for (String systemPackage : SYSTEM_PACKAGES_EXTRA) {
                description.addExtraSystemPackage(systemPackage);
            }
        }
        addAdditionalRuntimeArtifacts(workspaceLocator, session, description);
        if (workspaceLocator != null) {
            workspaceLocator.addPlatformProperties(description);
        }
    }

    public void addAdditionalRuntimeArtifacts(WorkspaceTychoOsgiRuntimeLocator workspaceLocator, MavenSession session,
            EquinoxRuntimeDescription description) throws MavenExecutionException {
        for (Map.Entry<String, TychoOsgiRuntimeArtifacts> entry : runtimeArtifacts.entrySet()) {
            if (TychoOsgiRuntimeArtifacts.HINT_FRAMEWORK.equals(entry.getKey())) {
                continue;
            }
            if (TychoOsgiRuntimeArtifacts.HINT_SHARED.equals(entry.getKey())) {
                continue;
            }
            addRuntimeArtifacts(workspaceLocator, description, session, entry.getValue());
        }
    }

    private void addRuntimeArtifacts(WorkspaceTychoOsgiRuntimeLocator workspaceLocator,
            EquinoxRuntimeDescription description, MavenSession session, TychoOsgiRuntimeArtifacts framework)
            throws MavenExecutionException {
        for (Dependency dependency : framework.getRuntimeArtifacts()) {
            if (workspaceLocator != null) {
                Dependency dependencyPom = new Dependency();
                dependencyPom.setType("pom");
                dependencyPom.setGroupId(dependency.getGroupId());
                dependencyPom.setArtifactId(dependency.getArtifactId());
                dependencyPom.setVersion(dependency.getVersion());
                Artifact pom = resolveDependency(session, dependencyPom);

                boolean resolved;
                if ("zip".equals(dependency.getType())) {
                    resolved = workspaceLocator.addProduct(description, pom);
                } else {
                    resolved = workspaceLocator.addBundle(description, pom);
                }

                if (resolved) {
                    continue;
                }

                // fallback to regular resolution logic if requested dependency is not found in the workspace
            }
            addRuntimeArtifact(description, session, dependency);
        }
    }

    private void addRuntimeArtifact(EquinoxRuntimeDescription description, MavenSession session, Dependency dependency)
            throws MavenExecutionException {
        Artifact artifact = resolveDependency(session, dependency);

        if ("zip".equals(dependency.getType())) {
            File artifactFile = new File(session.getLocalRepository().getBasedir(), session.getLocalRepository()
                    .pathOf(artifact));
            File eclipseDir = new File(artifactFile.getParentFile(), "eclipse");

            FileLocker locker = fileLockService.getFileLocker(artifactFile);
            locker.lock();
            try {
                if (!eclipseDir.exists() || artifact.isSnapshot()) {
                    logger.debug("Extracting Tycho's OSGi runtime");

                    if (artifact.getFile().lastModified() > eclipseDir.lastModified()) {
                        logger.debug("Unpacking Tycho's OSGi runtime to " + eclipseDir);
                        try {
                            FileUtils.deleteDirectory(eclipseDir);
                        } catch (IOException e) {
                            logger.warn("Failed to delete Tycho's OSGi runtime " + eclipseDir + ": " + e.getMessage());
                        }
                        eclipseDir.mkdirs();
                        unArchiver.setSourceFile(artifact.getFile());
                        unArchiver.setDestDirectory(eclipseDir);
                        try {
                            unArchiver.extract();
                        } catch (ArchiverException e) {
                            throw new MavenExecutionException("Failed to unpack Tycho's OSGi runtime: "
                                    + e.getMessage(), e);
                        }

                        eclipseDir.setLastModified(artifact.getFile().lastModified());
                    }
                }
            } finally {
                locker.release();
            }
            description.addInstallation(eclipseDir);
        } else {
            description.addBundle(artifact.getFile());
        }
    }

    public Artifact resolveDependency(MavenSession session, Dependency dependency) throws MavenExecutionException {
        Artifact artifact = repositorySystem.createArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getVersion(), dependency.getType());

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(getPluginRepositories(session));
        request.setCache(session.getRepositoryCache());
        request.setOffline(session.isOffline());
        request.setProxies(session.getSettings().getProxies());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());

        ArtifactResolutionResult result = repositorySystem.resolve(request);

        try {
            resolutionErrorHandler.throwErrors(request, result);
        } catch (ArtifactResolutionException e) {
            throw new MavenExecutionException("Could not resolve artifact for Tycho's OSGi runtime", e);
        }

        return artifact;
    }

    protected List<ArtifactRepository> getPluginRepositories(MavenSession session) {
        List<ArtifactRepository> repositories = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            repositories.addAll(project.getPluginArtifactRepositories());
        }
        return repositorySystem.getEffectiveRepositories(repositories);
    }
}
