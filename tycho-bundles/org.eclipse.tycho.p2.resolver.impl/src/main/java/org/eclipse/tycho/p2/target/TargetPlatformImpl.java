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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

public class TargetPlatformImpl implements P2TargetPlatform {

    /**
     * IInstallableUnits available from p2 repositories, either directly or via .target files, and
     * from local maven repository
     */
    private final Collection<IInstallableUnit> externalIUs;

    /**
     * Additional information about the execution environment, e.g. the "a.jre" IU with the list of
     * exported packages.
     */
    private final ExecutionEnvironmentResolutionHints executionEnvironment;

    /**
     * IInstallableUnits that correspond to pom dependency artifacts.
     */
    private final Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs;

    /**
     * Reactor build projects
     */
    private final Collection<IReactorArtifactFacade> reactorProjects;

    // FIXME only used to warn about locally installed artifacts, this logic does not belong here
    private final LocalMetadataRepository localMetadataRepository;

    private final IRawArtifactFileProvider jointArtifacts;
    @Deprecated
    private LocalArtifactRepository localArtifactRepository;

    private final MavenLogger logger;

    /**
     * Reactor project IU filter. Non-reactor IUs are pre-filtered for performance reasons
     */
    private final TargetPlatformFilterEvaluator filter;

    public TargetPlatformImpl(Collection<IReactorArtifactFacade> reactorProjects, Collection<IInstallableUnit> ius,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactIUs,
            ExecutionEnvironmentResolutionHints executionEnvironment, TargetPlatformFilterEvaluator filter,
            LocalMetadataRepository localMetadataRepository, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository, MavenLogger logger) {
        this.reactorProjects = reactorProjects;
        this.externalIUs = ius;
        this.executionEnvironment = executionEnvironment;
        this.mavenArtifactIUs = mavenArtifactIUs;
        this.filter = filter;
        this.localMetadataRepository = localMetadataRepository;
        this.jointArtifacts = jointArtifacts;
        this.localArtifactRepository = localArtifactRepository;

        this.logger = logger;
    }

    public Collection<IInstallableUnit> getInstallableUnits() {
        Set<IInstallableUnit> allius = new LinkedHashSet<IInstallableUnit>();

        allius.addAll(getReactorProjectIUs().keySet());

        allius.addAll(externalIUs);

        allius.addAll(mavenArtifactIUs.keySet());

        // TODO this should be done by the builder
        allius.addAll(executionEnvironment.getMandatoryUnits());

        return Collections.unmodifiableCollection(allius);
    }

    public Map<IInstallableUnit, IReactorArtifactFacade> getReactorProjectIUs() {
        Map<IInstallableUnit, IReactorArtifactFacade> allius = new LinkedHashMap<IInstallableUnit, IReactorArtifactFacade>();

        for (IReactorArtifactFacade project : reactorProjects) {
            for (Object iu : project.getDependencyMetadata(true)) {
                allius.put((IInstallableUnit) iu, project);
            }
            for (Object iu : project.getDependencyMetadata(false)) {
                allius.put((IInstallableUnit) iu, project);
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

    public ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return executionEnvironment;
    }

    public Collection<IInstallableUnit> getReactorProjectIUs(File projectRoot, boolean primary) {
        boolean found = false;
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();
        for (IReactorArtifactFacade project : reactorProjects) {
            if (project.getLocation().equals(projectRoot)) {
                found = true;
                result.addAll(TargetPlatformBuilderImpl.toSet(project.getDependencyMetadata(primary),
                        IInstallableUnit.class));
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Not a reactor project: " + projectRoot);
        }
        filterUnits(result);
        return Collections.unmodifiableSet(result);
    }

    // TODO rename: this method doesn't include POM dependency Maven artifacts
    public IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        // number of reactor projects is not huge, so this should not be a performance problem
        Map<IInstallableUnit, IReactorArtifactFacade> reactorProjectIUs = getReactorProjectIUs();

        IArtifactFacade artifact = reactorProjectIUs.get(iu);

        if (artifact == null) {
            artifact = mavenArtifactIUs.get(iu);
        }

        return artifact;
    }

    public File getLocalArtifactFile(IArtifactKey key) {
        return jointArtifacts.getArtifactFile(key);
    }

    public void saveLocalMavenRepository() {
        localArtifactRepository.save();
    }

    public void reportUsedIUs(Collection<IInstallableUnit> usedUnits) {
        final Set<IInstallableUnit> localIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS, null).toSet();
        localIUs.retainAll(usedUnits);

        // workaround to avoid warnings for "a.jre.javase" IUs - TODO avoid this step?
        for (Iterator<IInstallableUnit> iterator = localIUs.iterator(); iterator.hasNext();) {
            if (executionEnvironment.isNonApplicableEEUnit(iterator.next())) {
                iterator.remove();
            }
        }

        if (!localIUs.isEmpty()) {
            logLocalIUMessage("The following locally built units have been used to resolve project dependencies:");
            for (IInstallableUnit localIu : localIUs) {
                logLocalIUMessage("  " + localIu.getId() + "/" + localIu.getVersion());
            }
        }
    }

    private void logLocalIUMessage(String message) {
        if (localMetadataRepository.getIncludeInTargetPlatform()) {
            logger.warn(message);
        }
    }
}
