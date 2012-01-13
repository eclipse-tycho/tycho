/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.ClassifiedLocation;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class TargetPlatformImpl implements P2TargetPlatform {

    private final Collection<IInstallableUnit> allIUs;
    private final Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs;
    private final Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectIUs;
    private final Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectSecondaryIUs;
    private final LocalMetadataRepository localMetadataRepository;

    private final String executionEnvironment;
    private final List<URI> remoteArtifactRepositories;
    private final LocalArtifactRepository localMavenRepository;

    private final IProvisioningAgent agent;
    private final MavenLogger logger;

    public TargetPlatformImpl(Collection<IInstallableUnit> allTargetPlatformIUs,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs,
            Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectIUs,
            Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectSecondaryIUs,
            LocalMetadataRepository localMetadataRepository, String executionEnvironment,
            List<URI> allRemoteArtifactRepositories, LocalArtifactRepository localMavenRepository,
            IProvisioningAgent agent, MavenLogger logger) {
        this.allIUs = allTargetPlatformIUs;
        this.mavenArtifactIUs = mavenArtifactIUs;
        this.reactorProjectIUs = reactorProjectIUs;
        this.reactorProjectSecondaryIUs = reactorProjectSecondaryIUs;
        this.localMetadataRepository = localMetadataRepository;
        this.executionEnvironment = executionEnvironment;
        this.remoteArtifactRepositories = allRemoteArtifactRepositories;
        this.localMavenRepository = localMavenRepository;

        this.agent = agent;
        this.logger = logger;
    }

    public Collection<IInstallableUnit> getInstallableUnits() {
        return Collections.unmodifiableCollection(allIUs);
    }

    @SuppressWarnings("restriction")
    public Collection<IInstallableUnit> getJREIUs() {
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, new NullProgressMonitor());
        return results.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet();
    }

    public LinkedHashSet<IInstallableUnit> getReactorProjectIUs(File projectRoot, boolean primary) {
        LinkedHashSet<IInstallableUnit> ius = new LinkedHashSet<IInstallableUnit>();
        boolean projectExists = false;

        Map<ClassifiedLocation, Set<IInstallableUnit>> projectIUs = primary ? reactorProjectIUs
                : reactorProjectSecondaryIUs;
        for (Map.Entry<ClassifiedLocation, Set<IInstallableUnit>> entry : projectIUs.entrySet()) {
            if (projectRoot.equals(entry.getKey().getLocation())) {
                ius.addAll(entry.getValue());
                projectExists = true;
            }
        }

        if (!projectExists)
            throw new IllegalArgumentException("Not a reactor project: " + projectRoot);
        return ius;
    }

    public IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        return mavenArtifactIUs.get(iu);
    }

    public File getLocalArtifactFile(IArtifactKey key) {
        return localMavenRepository.getArtifactFile(key);
    }

    public void reportUsedIUs(Collection<IInstallableUnit> usedUnits) {
        warnAboutLocalIus(usedUnits);
    }

    public void warnAboutLocalIus(Collection<IInstallableUnit> units) {
        final Set<IInstallableUnit> localIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS, null).toSet();
        localIUs.retainAll(units);
        if (!localIUs.isEmpty()) {
            logger.warn("The following locally built units have been used to resolve project dependencies:");
            for (IInstallableUnit localIu : localIUs) {
                logger.warn("  " + localIu.getId() + "/" + localIu.getVersion());
            }
        }
    }

    // TODO this method should not be necessary; instead download should happen on access
    public void downloadArtifacts(Collection<IInstallableUnit> usedUnits) {
        P2ArtifactDownloadTool downloadTool = new P2ArtifactDownloadTool(agent, logger);

        List<IArtifactKey> remoteArtifacts = new ArrayList<IArtifactKey>();
        for (IInstallableUnit iu : usedUnits) {
            // maven IUs either come from reactor or local maven repository, no need to download them from p2 repos
            if (getMavenArtifact(iu) == null) {
                Collection<IArtifactKey> artifactKeys = iu.getArtifacts();
                remoteArtifacts.addAll(artifactKeys);
            }
        }

        downloadTool.downloadArtifactsToLocalMavenRepository(remoteArtifacts, remoteArtifactRepositories,
                localMavenRepository);

        // TODO is this needed?
        localMetadataRepository.save();
    }
}
