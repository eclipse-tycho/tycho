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
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = EquinoxRuntimeLocator.class)
public class TychoP2RuntimeLocator implements EquinoxRuntimeLocator {
    /**
     * List of packages exported by org.eclipse.tycho.p2 artifact/bundle.
     */
    public static final String[] SYSTEM_PACKAGES_EXTRA = { "org.eclipse.tycho", // 
            "org.eclipse.tycho.artifacts", //
            "org.eclipse.tycho.core.facade", //
            "org.eclipse.tycho.core.resolver.shared", //
            "org.eclipse.tycho.locking.facade", //
            "org.eclipse.tycho.p2.metadata", //
            "org.eclipse.tycho.p2.repository", //
            "org.eclipse.tycho.p2.resolver.facade", //
            "org.eclipse.tycho.p2.target.facade", //
            "org.eclipse.tycho.p2.tools", //
            "org.eclipse.tycho.p2.tools.director.facade", //
            "org.eclipse.tycho.p2.tools.publisher.facade", //
            "org.eclipse.tycho.p2.tools.mirroring.facade", //
            "org.eclipse.tycho.p2.tools.verifier.facade", //
            "org.eclipse.tycho.repository.registry.facade" };

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
    private Map<String, TychoP2RuntimeMetadata> runtimeMetadata;

    public void locateRuntime(EquinoxRuntimeDescription description) throws MavenExecutionException {
        MavenSession session = buildContext.getSession();

        addRuntimeArtifacts(session, description);

        for (String systemPackage : SYSTEM_PACKAGES_EXTRA) {
            description.addExtraSystemPackage(systemPackage);
        }
    }

    public void addRuntimeArtifacts(MavenSession session, EquinoxRuntimeDescription description)
            throws MavenExecutionException {
        TychoP2RuntimeMetadata framework = runtimeMetadata.get(TychoP2RuntimeMetadata.HINT_FRAMEWORK);
        if (framework != null) {
            addRuntimeArtifacts(description, session, framework);
        }

        for (Map.Entry<String, TychoP2RuntimeMetadata> entry : runtimeMetadata.entrySet()) {
            if (!TychoP2RuntimeMetadata.HINT_FRAMEWORK.equals(entry.getKey())) {
                addRuntimeArtifacts(description, session, entry.getValue());
            }
        }
    }

    private void addRuntimeArtifacts(EquinoxRuntimeDescription description, MavenSession session,
            TychoP2RuntimeMetadata framework) throws MavenExecutionException {
        for (Dependency dependency : framework.getRuntimeArtifacts()) {
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
                    logger.debug("Extracting P2 runtime");

                    if (artifact.getFile().lastModified() > eclipseDir.lastModified()) {
                        logger.debug("Unpacking P2 runtime to " + eclipseDir);
                        try {
                            FileUtils.deleteDirectory(eclipseDir);
                        } catch (IOException e) {
                            logger.warn("Failed to delete P2 runtime " + eclipseDir + ": " + e.getMessage());
                        }
                        unArchiver.setSourceFile(artifact.getFile());
                        unArchiver.setDestDirectory(eclipseDir.getParentFile());
                        try {
                            unArchiver.extract();
                        } catch (ArchiverException e) {
                            throw new MavenExecutionException("Failed to unpack P2 runtime: " + e.getMessage(), e);
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

    private Artifact resolveDependency(MavenSession session, Dependency d) throws MavenExecutionException {
        Artifact artifact = repositorySystem.createArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(),
                d.getType());

        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        for (MavenProject project : session.getProjects()) {
            repositories.addAll(project.getPluginArtifactRepositories());
        }
        repositories = repositorySystem.getEffectiveRepositories(repositories);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(repositories);
        request.setCache(session.getRepositoryCache());
        request.setOffline(session.isOffline());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());

        ArtifactResolutionResult result = repositorySystem.resolve(request);

        try {
            resolutionErrorHandler.throwErrors(request, result);
        } catch (ArtifactResolutionException e) {
            throw new MavenExecutionException("Could not resolve tycho-p2-runtime", e);
        }

        return artifact;
    }

}
