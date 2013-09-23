/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *    SAP AG - create immutable target platform instances
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

public class PreliminaryTargetPlatformImpl extends TargetPlatformBaseImpl {

    /**
     * IInstallableUnits available from p2 repositories, either directly or via .target files, and
     * from local maven repository
     */
    private final Collection<IInstallableUnit> externalIUs;

    /**
     * Reactor build projects
     */
    private final Collection<ReactorProject> reactorProjects;

    // FIXME only used to warn about locally installed artifacts, this logic does not belong here
    private final LocalMetadataRepository localMetadataRepository;

    private final MavenLogger logger;

    /**
     * Reactor project IU filter. Non-reactor IUs are pre-filtered for performance reasons
     */
    private final TargetPlatformFilterEvaluator filter;

    private final boolean includeLocalRepo;

    public PreliminaryTargetPlatformImpl(Collection<ReactorProject> reactorProjects, Collection<IInstallableUnit> ius,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs,
            ExecutionEnvironmentResolutionHints executionEnvironment, TargetPlatformFilterEvaluator filter,
            LocalMetadataRepository localMetadataRepository, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository, boolean includeLocalRepo, MavenLogger logger) {
        super(executionEnvironment, jointArtifacts, localArtifactRepository, mavenArtifactIUs);
        this.reactorProjects = reactorProjects;
        this.externalIUs = ius;
        this.filter = filter;
        this.localMetadataRepository = localMetadataRepository;
        this.includeLocalRepo = includeLocalRepo;
        this.logger = logger;
    }

    public Set<IInstallableUnit> getInstallableUnits() {
        Set<IInstallableUnit> allius = new LinkedHashSet<IInstallableUnit>();

        allius.addAll(getReactorProjectIUs().keySet());

        allius.addAll(externalIUs);

        allius.addAll(mavenArtifactLookup.keySet());

        // TODO this should be done by the builder
        allius.addAll(executionEnvironment.getMandatoryUnits());

        return Collections.unmodifiableSet(allius);
    }

    public Map<IInstallableUnit, ReactorProjectIdentities> getReactorProjectIUs() {
        Map<IInstallableUnit, ReactorProjectIdentities> allius = new LinkedHashMap<IInstallableUnit, ReactorProjectIdentities>();

        for (ReactorProject project : reactorProjects) {
            Set<?> projectUnits = project.getDependencyMetadata();
            if (projectUnits == null)
                continue;

            for (Object iu : projectUnits) {
                allius.put((IInstallableUnit) iu, project.getIdentities());
            }
        }

        filterUnits(allius.keySet());

        return Collections.unmodifiableMap(allius);
    }

    private void filterUnits(Collection<IInstallableUnit> keySet) {
        if (filter != null) {
            filter.filterUnits(keySet);
        }
    }

    public Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap() {
        return getReactorProjectIUs();
    }

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

    public Set<IInstallableUnit> getExternalUnits() {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();
        result.addAll(externalIUs);
        result.addAll(mavenArtifactLookup.keySet());
        result.addAll(executionEnvironment.getMandatoryUnits());
        return result;
    }

    public IRawArtifactFileProvider getJointArtifacts() {
        return jointArtifacts;
    }

}
