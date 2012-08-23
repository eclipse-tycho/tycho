/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.baseline;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.impl.publisher.P2Artifact;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.tools.baseline.facade.BaselineService;

@SuppressWarnings("restriction")
public class BaselineServiceImpl implements BaselineService {

    private IProgressMonitor monitor = new NullProgressMonitor();

    // @Inject
    private RemoteAgentManager remoteAgentManager;

    public Map<String, IP2Artifact> getProjectBaseline(Collection<MavenRepositoryLocation> baselineLocations,
            Map<String, IP2Artifact> reactor, File target) {

        // baseline repository may contain artifacts with the same id/version but different contents
        // compared to what is installed (or cached) locally.
        // current local repository layout does not track per-repository artifacts and does not allow
        // multiple different artifacts with same id/version.

        CompositeMetadataRepository baselineUnits;
        CompositeArtifactRepository baselineArtifacts;

        try {
            IProvisioningAgent remoteAgent = remoteAgentManager.getProvisioningAgent();
            IRepositoryIdManager remoteRepositoryIdManager = (IRepositoryIdManager) remoteAgent
                    .getService(IRepositoryIdManager.SERVICE_NAME);
            IMetadataRepositoryManager remoteMetadataRepositoryManager = (IMetadataRepositoryManager) remoteAgent
                    .getService(IMetadataRepositoryManager.SERVICE_NAME);
            IArtifactRepositoryManager remoteArtifactRepositoryManager = (IArtifactRepositoryManager) remoteAgent
                    .getService(IArtifactRepositoryManager.SERVICE_NAME);

            baselineUnits = CompositeMetadataRepository.createMemoryComposite(remoteAgent);
            baselineArtifacts = CompositeArtifactRepository.createMemoryComposite(remoteAgent);

            for (MavenRepositoryLocation location : baselineLocations) {
                URI url = location.getURL();

                try {
                    remoteRepositoryIdManager.addMapping(location.getId(), url);

                    // TODO offline mode https://bugs.eclipse.org/bugs/show_bug.cgi?id=337022

                    // not strictly necessary, but makes sure metadata download is visible in the console/log
                    remoteMetadataRepositoryManager.loadRepository(url, monitor);
                    remoteArtifactRepositoryManager.loadRepository(url, monitor);

                    baselineUnits.addChild(url);
                    baselineArtifacts.addChild(url);
                } catch (ProvisionException e) {
                    // baseline repository may not exist yet
                    // TODO log a warning message
                }
            }
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }

        Map<String, IP2Artifact> result = new LinkedHashMap<String, IP2Artifact>();

        for (Map.Entry<String, IP2Artifact> reactorArtifact : reactor.entrySet()) {
            IArtifactDescriptor descriptor = (IArtifactDescriptor) reactorArtifact.getValue().getArtifactDescriptor();

            IArtifactDescriptor baselineDescriptor = getBaselineDescriptor(baselineArtifacts, descriptor);
            if (baselineDescriptor == null) {
                continue;
            }

            IArtifactKey baslineKey = baselineDescriptor.getArtifactKey();
            String format = baselineDescriptor.getProperty(IArtifactDescriptor.FORMAT);
            File baselineArtifact = new File(target, baslineKey.getClassifier() + "/" + baslineKey.getId() + "/"
                    + baslineKey.getVersion() + (format != null ? "." + format : ""));

            try {
                baselineArtifact.getParentFile().mkdirs();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(baselineArtifact));
                try {
                    baselineArtifacts.getRawArtifact(baselineDescriptor, os, monitor);
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // ignored
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
            for (Object _unit : reactorArtifact.getValue().getInstallableUnits()) {
                IInstallableUnit unit = (IInstallableUnit) _unit;

                IInstallableUnit baselineUnit = getBaselineUnit(baselineUnits, unit.getId(), unit.getVersion());
                if (baselineUnit != null) {
                    units.add(baselineUnit);
                }
            }

            result.put(reactorArtifact.getKey(), new P2Artifact(baselineArtifact, units, descriptor));
        }

        return !result.isEmpty() ? result : null;
    }

    private IArtifactDescriptor getBaselineDescriptor(IArtifactRepository baseline, IArtifactDescriptor descriptor) {
        IArtifactDescriptor[] baselineDescriptors = baseline.getArtifactDescriptors(descriptor.getArtifactKey());

        for (IArtifactDescriptor baselineDescriptor : baselineDescriptors) {
            if (eq(descriptor.getProperty(IArtifactDescriptor.FORMAT),
                    baselineDescriptor.getProperty(IArtifactDescriptor.FORMAT))
                    && Arrays.equals(descriptor.getProcessingSteps(), baselineDescriptor.getProcessingSteps())) {
                return baselineDescriptor;
            }
        }

        return null;
    }

    private static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

    private IInstallableUnit getBaselineUnit(IQueryable<IInstallableUnit> units, String id, Version version) {
        IQueryResult<IInstallableUnit> result = units.query(QueryUtil.createIUQuery(id, version), monitor);

        if (result.isEmpty()) {
            return null;
        }

        Iterator<IInstallableUnit> iterator = result.iterator();

        IInstallableUnit unit = iterator.next();

        if (iterator.hasNext()) {
            throw new IllegalArgumentException();
        }

        return unit;
    }

    public boolean isMetadataEqual(IP2Artifact baseline, IP2Artifact reactor) {
        // TODO Auto-generated method stub
        return true;
    }

    // OSGi DS

    public void setRemoteAgentManager(RemoteAgentManager remoteAgentManager) {
        this.remoteAgentManager = remoteAgentManager;
    }

}
