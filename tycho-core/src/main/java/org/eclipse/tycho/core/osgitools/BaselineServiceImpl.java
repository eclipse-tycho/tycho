/*******************************************************************************
 * Copyright (c) 2012, 2025 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.helper.StatusTool;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.publisher.P2Artifact;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

@Named
@Singleton
public class BaselineServiceImpl implements BaselineService {

    private IProgressMonitor monitor = new NullProgressMonitor();

    @Inject
    private P2RepositoryManager repositoryManager;
    @Inject
    private IProvisioningAgent agent;

    @Inject
    private Logger logger;

    @Override
    public Map<String, IP2Artifact> getProjectBaseline(Collection<Repository> baselineRepositories,
            Map<String, IP2Artifact> reactor, File target) {

        // baseline repository may contain artifacts with the same id/version but different contents
        // compared to what is installed (or cached) locally.
        // current local repository layout does not track per-repository artifacts and does not allow
        // multiple different artifacts with same id/version.

        baselineRepositories = repositoryManager.normalizeRepositoryLocations(baselineRepositories);

        List<IMetadataRepository> metadataRepositories = new ArrayList<>();
        List<IArtifactRepository> artifactRepositories = new ArrayList<>();
        for (Repository location : baselineRepositories) {
            try {
                metadataRepositories.add(repositoryManager.getMetadataRepository(location));
                artifactRepositories.add(repositoryManager.getArtifactRepository(location));
            } catch (ProvisionException | URISyntaxException e) {
                // baseline repository may not exist yet
                logger.warn(e.getMessage(), e);
            }
        }
        IQueryable<IInstallableUnit> baselineUnits = metadataRepositories.size() == 1 ? metadataRepositories.getFirst()
                : new CompoundQueryable<>(metadataRepositories);
        IArtifactRepository baselineArtifacts = artifactRepositories.size() == 1 ? artifactRepositories.getFirst()
                : new ListCompositeArtifactRepository(artifactRepositories, agent);

        Map<String, IP2Artifact> result = new LinkedHashMap<>();

        for (Map.Entry<String, IP2Artifact> reactorArtifact : reactor.entrySet()) {
            IP2Artifact value = reactorArtifact.getValue();
            IArtifactDescriptor descriptor = value.getArtifactDescriptor();

            IArtifactDescriptor baselineDescriptor = getBaselineDescriptor(baselineArtifacts, descriptor);
            if (baselineDescriptor == null) {
                continue;
            }
            IArtifactKey baselineKey = baselineDescriptor.getArtifactKey();
            String format = baselineDescriptor.getProperty(IArtifactDescriptor.FORMAT);
            File baselineArtifact = new File(target, baselineKey.getClassifier() + "/" + baselineKey.getId() + "-"
                    + baselineKey.getVersion() + (format != null ? "." + format : "") + getExtension(value));

            baselineArtifact.getParentFile().mkdirs();
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(baselineArtifact))) {
                IStatus status = baselineArtifacts.getRawArtifact(baselineDescriptor, os, monitor);
                if (status.matches(IStatus.ERROR | IStatus.CANCEL)) {
                    String repository = baselineDescriptor.getRepository().getLocation().toString();
                    String artifactId = baselineDescriptor.getArtifactKey().getId();
                    String artifactVersion = baselineDescriptor.getArtifactKey().getVersion().toString();
                    String statusMessage = StatusTool.toLogMessage(status);
                    throw new RuntimeException(String.format("Error trying to download %s version %s from %s:\n%s",
                            artifactId, artifactVersion, repository, statusMessage), StatusTool.findException(status));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<IInstallableUnit> units = new ArrayList<>();
            for (IInstallableUnit unit : value.getInstallableUnits()) {
                IInstallableUnit baselineUnit = getBaselineUnit(baselineUnits, unit.getId(), unit.getVersion());
                if (baselineUnit != null) {
                    units.add(baselineUnit);
                }
            }
            result.put(reactorArtifact.getKey(), new P2Artifact(baselineArtifact, units, descriptor));
        }

        return !result.isEmpty() ? result : null;
    }

    private String getExtension(IP2Artifact value) {
        File location = value.getLocation();
        if (location != null) {
            String extension = FilenameUtils.getExtension(location.getName());
            if (!extension.isBlank()) {
                return "." + extension;
            }
        }
        return "";
    }

    private IArtifactDescriptor getBaselineDescriptor(IArtifactRepository baseline, IArtifactDescriptor descriptor) {
        String format = descriptor.getProperty(IArtifactDescriptor.FORMAT);
        IProcessingStepDescriptor[] processingSteps = descriptor.getProcessingSteps();
        return Arrays.stream(baseline.getArtifactDescriptors(descriptor.getArtifactKey()))
                .filter(d -> Objects.equals(format, d.getProperty(IArtifactDescriptor.FORMAT)))
                .filter(d -> Arrays.equals(processingSteps, d.getProcessingSteps())) //
                .findFirst().orElse(null);
    }

    private IInstallableUnit getBaselineUnit(IQueryable<IInstallableUnit> units, String id, Version version) {
        IQueryResult<IInstallableUnit> result = units.query(QueryUtil.createIUQuery(id, version), monitor);

        if (result.isEmpty()) {
            return null;
        }
        Iterator<IInstallableUnit> iterator = result.iterator();
        IInstallableUnit unit = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalArgumentException("Unit not unique in the baseline: " + id + " - " + version);
        }
        return unit;
    }

    @Override
    public boolean isMetadataEqual(IP2Artifact baseline, IP2Artifact reactor) {
        // TODO Auto-generated method stub
        return true;
    }

}
