/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - split target platform computation and dependency resolution
 *    SAP SE - create immutable target platform instances
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.resolver.target.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.ProviderOnlyArtifactRepository;

public class PreliminaryTargetPlatformImpl extends TargetPlatformBaseImpl {

    /**
     * IInstallableUnits available from reactor-external sources, i.e. POM p2 repositories, target
     * files, POM dependencies, and the local Maven repository
     */
    private final Collection<IInstallableUnit> externalIUs;

    // TODO 412416 only used to warn about locally installed artifacts, this logic does not belong here
    private final LocalMetadataRepository localMetadataRepository;

    private final MavenLogger logger;

    /**
     * Reactor project IU filter. Non-reactor IUs are pre-filtered for performance reasons
     */
    private final TargetPlatformFilterEvaluator filter;

    private final boolean includeLocalRepo;

    private IArtifactRepository artifactRepository;

    public PreliminaryTargetPlatformImpl(Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectIUs,
            Collection<IInstallableUnit> externalIUs, ExecutionEnvironmentResolutionHints executionEnvironment,
            TargetPlatformFilterEvaluator filter, LocalMetadataRepository localMetadataRepository,
            IRawArtifactFileProvider externalArtifacts, LocalArtifactRepository localArtifactRepository,
            boolean includeLocalRepo, MavenLogger logger, Set<IInstallableUnit> shadowed) {
        super(collectAllInstallableUnits(reactorProjectIUs, externalIUs, executionEnvironment), executionEnvironment,
                externalArtifacts, localArtifactRepository, reactorProjectIUs, new HashMap<>(), shadowed);
        this.externalIUs = externalIUs;
        this.filter = filter;
        this.localMetadataRepository = localMetadataRepository;
        this.includeLocalRepo = includeLocalRepo;
        this.logger = logger;
        this.artifactRepository = new ProviderOnlyArtifactRepository(artifacts, null, URI.create("preliminary:/"));
    }

    public static LinkedHashSet<IInstallableUnit> collectAllInstallableUnits(
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectIUs, Collection<IInstallableUnit> externalIUs,
            ExecutionEnvironmentResolutionHints executionEnvironment) {
        LinkedHashSet<IInstallableUnit> allius = new LinkedHashSet<>();

        allius.addAll(reactorProjectIUs.keySet());

        allius.addAll(externalIUs);

        allius.addAll(executionEnvironment.getMandatoryUnits());

        return allius;
    }

    @Override
    public void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits) {
        if (!includeLocalRepo) {
            return;
        }
        final Set<IInstallableUnit> localIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS, null).toSet();
        localIUs.retainAll(usedUnits);

        // workaround to avoid warnings for "a.jre.javase" IUs - TODO avoid this step?
        for (Iterator<IInstallableUnit> iterator = localIUs.iterator(); iterator.hasNext();) {
            if (executionEnvironment.isNonApplicableEEUnit(iterator.next())) {
                iterator.remove();
            }
        }

        if (!localIUs.isEmpty()) {
            logger.warn("The following locally built units have been used to resolve project dependencies:");
            for (IInstallableUnit localIu : localIUs) {
                logger.warn("  " + localIu.getId() + "/" + localIu.getVersion());
            }
        }
    }

    public LinkedHashSet<IInstallableUnit> getExternalUnits() {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<>();
        result.addAll(externalIUs);
        // TODO are these "external units"?
        result.addAll(executionEnvironment.getMandatoryUnits());
        return result;
    }

    public TargetPlatformFilterEvaluator getFilter() {
        return filter;
    }

    public IRawArtifactFileProvider getExternalArtifacts() {
        return artifacts;
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        // the preliminary TP shall not be used to create build results, so this method is not needed
        throw new UnsupportedOperationException();
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

}
