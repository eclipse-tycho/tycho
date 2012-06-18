/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

class TargetPlatformImpl implements P2TargetPlatform {

    /**
     * IInstallableUnits available from p2 repositories, either directly or via .target files, and
     * from local maven repository
     */
    private final LinkedHashSet<IInstallableUnit> p2RepositoryIUs;
    private final Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs;

    /**
     * Installable unit(s) that represent capabilities of the target JRE.
     */
    private final Collection<IInstallableUnit> jreUIs;

    // FIXME only used to warn about locally installed artifacts, this logic does not belong here
    private final LocalMetadataRepository localMetadataRepository;

    private final List<URI> remoteArtifactRepositories;
    private final LocalArtifactRepository localMavenRepository;

    private final IProvisioningAgent agent;
    private final MavenLogger logger;

    private final boolean includePackedArtifacts;

    TargetPlatformImpl(LinkedHashSet<IInstallableUnit> p2RepositoryIUs,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs, Collection<IInstallableUnit> jreUIs,
            LocalMetadataRepository localMetadataRepository, List<URI> allRemoteArtifactRepositories,
            LocalArtifactRepository localMavenRepository, IProvisioningAgent agent, boolean includePackedArtifacts,
            MavenLogger logger) {
        this.p2RepositoryIUs = p2RepositoryIUs;
        this.jreUIs = jreUIs;
        this.mavenArtifactIUs = mavenArtifactIUs;
        this.localMetadataRepository = localMetadataRepository;
        this.remoteArtifactRepositories = allRemoteArtifactRepositories;
        this.localMavenRepository = localMavenRepository;

        this.agent = agent;
        this.includePackedArtifacts = includePackedArtifacts;
        this.logger = logger;
    }

    public Collection<IInstallableUnit> getInstallableUnits() {
        Set<IInstallableUnit> allius = new LinkedHashSet<IInstallableUnit>();
        allius.addAll(p2RepositoryIUs);
        allius.addAll(mavenArtifactIUs.keySet());
        allius.addAll(jreUIs);
        // TODO store merged IU list? (if yes, protect against modification)
        return allius;
    }

    public Collection<IInstallableUnit> getJREIUs() {
        return jreUIs;
    }

    public IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        return mavenArtifactIUs.get(iu);
    }

    public File getLocalArtifactFile(IArtifactKey key) {
        return localMavenRepository.getArtifactFile(key);
    }

    public String getArtifactClassifier(IArtifactKey key) {
        IArtifactDescriptor descriptor = localMavenRepository.getCanonicalArtifactDescriptor(key);
        if (descriptor == null)
            throw new IllegalArgumentException("Artifact key not in local Maven repository: " + key);
        return descriptor.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
    }

    public void reportUsedIUs(Collection<IInstallableUnit> usedUnits) {
        warnAboutLocalIus(usedUnits);
    }

    // FIXME this logic does not belong here
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
            // TODO have the download know about all existing target platform artifacts (including from POM deps)
            // so that we don't need to know here where IUs came from 
            if (p2RepositoryIUs.contains(iu)) {
                Collection<IArtifactKey> artifactKeys = iu.getArtifacts();
                remoteArtifacts.addAll(artifactKeys);
            }
        }

        downloadTool.downloadArtifactsToLocalMavenRepository(remoteArtifacts, remoteArtifactRepositories,
                localMavenRepository, includePackedArtifacts);

        // TODO is this needed?
        localMetadataRepository.save();
    }
}
