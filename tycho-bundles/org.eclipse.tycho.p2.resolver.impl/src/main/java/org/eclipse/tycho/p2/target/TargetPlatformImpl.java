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
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.ClassifiedLocation;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class TargetPlatformImpl implements P2TargetPlatform {

    private final IQueryable<IInstallableUnit> allIUs;
    private final Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs;
    private final Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectIUs;
    private final LocalMetadataRepository localMetadataRepository;

    private final String executionEnvironment;
    private final List<URI> remoteArtifactRepositories;
    private final LocalArtifactRepository localMavenRepository;

    private final IProvisioningAgent agent;
    private final MavenLogger logger;

    public TargetPlatformImpl(IQueryable<IInstallableUnit> allIUs,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs,
            Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectIUs,
            LocalMetadataRepository localMetadataRepository, String executionEnvironment,
            List<URI> allRemoteArtifactRepositories, LocalArtifactRepository localMavenRepository,
            IProvisioningAgent agent, MavenLogger logger) {
        this.allIUs = allIUs;
        this.mavenArtifactIUs = mavenArtifactIUs;
        this.reactorProjectIUs = reactorProjectIUs;
        this.localMetadataRepository = localMetadataRepository;
        this.executionEnvironment = executionEnvironment;
        this.remoteArtifactRepositories = allRemoteArtifactRepositories;
        this.localMavenRepository = localMavenRepository;

        this.agent = agent;
        this.logger = logger;
    }

    public IQueryable<IInstallableUnit> getInstallableUnits() {
        return allIUs;
    }

    @SuppressWarnings("restriction")
    public Collection<IInstallableUnit> getJREIUs() {
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, new NullProgressMonitor());
        return results.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet();
    }

    public LinkedHashSet<IInstallableUnit> getReactorProjectIUs(File projectRoot) {
        LinkedHashSet<IInstallableUnit> ius = new LinkedHashSet<IInstallableUnit>();
        boolean projectExists = false;

        for (Map.Entry<ClassifiedLocation, Set<IInstallableUnit>> entry : reactorProjectIUs.entrySet()) {
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
        downloadArtifacts(usedUnits);
    }

    public void warnAboutLocalIus(Collection<IInstallableUnit> units) {
        final Set<IInstallableUnit> localIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS, null).toSet();
        if (logger.isDebugEnabled()) {
            // TODO 364134 fix this text: these units are _in_ the target platform
            logger.debug("The following locally built units are considered during target platform resolution:");
            for (IInstallableUnit unit : localIUs) {
                logger.debug("  " + unit.getId() + "/" + unit.getVersion());
            }
        }
        localIUs.retainAll(units);
        if (!localIUs.isEmpty()) {
            // TODO 364134 fix this text: these units are actually used 
            logger.warn("Project build target platform includes the following locally built units:");
            for (IInstallableUnit localIu : localIUs) {
                logger.warn("  " + localIu.getId() + "/" + localIu.getVersion());
            }
        }
    }

    private void downloadArtifacts(Collection<IInstallableUnit> usedUnits) {
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
