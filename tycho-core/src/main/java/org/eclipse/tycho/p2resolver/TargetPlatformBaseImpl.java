/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Issue #845 - Feature restrictions are not taken into account when using emptyVersion 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.core.resolver.target.P2TargetPlatform;
import org.eclipse.tycho.p2.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;

abstract class TargetPlatformBaseImpl implements P2TargetPlatform {

    // content

    /**
     * All installable units contained in the target platform. This includes reactor-external
     * content and all results of upstream reactor projects (or all projects in case of the
     * preliminary target platform where the reactor build order isn't known yet). Configured and
     * automatic filters have been applied.
     */
    // TODO store as QueryableCollection, which contains indices to speed up queries?
    protected final LinkedHashSet<IInstallableUnit> installableUnits;

    // reverse lookup from target platform content to the contributing artifact/project 

    /**
     * Map from installable units back to the contributing reactor project. Note: May contain
     * installable units as keys which are not part of the target platform.
     */
    private final Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup;

    /**
     * Map from installable units back to the contributing artifacts. Note: May contain installable
     * units as keys which are not part of the target platform.
     */
    final Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup;

    // additional information on the dependency resolution context

    /**
     * Execution environment information with information about the packages provided by the JRE.
     */
    final ExecutionEnvironmentResolutionHints executionEnvironment;

    final IRawArtifactFileProvider artifacts;
    @Deprecated
    private LocalArtifactRepository localArtifactRepository;

    public TargetPlatformBaseImpl(LinkedHashSet<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider artifacts,
            LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup) {
        this.installableUnits = installableUnits;
        this.executionEnvironment = executionEnvironment;
        this.reactorProjectLookup = reactorProjectLookup;
        this.mavenArtifactLookup = mavenArtifactLookup;
        this.artifacts = artifacts;
        this.localArtifactRepository = localArtifactRepository;
    }

    @Override
    public final Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    @Override
    public final org.eclipse.tycho.ArtifactKey resolveArtifact(String type, String id, String version)
            throws IllegalArtifactReferenceException, DependencyResolutionException {
        IInstallableUnit resolvedUnit;
        if (version != null && (version.startsWith("[") || version.startsWith("("))) {
            resolvedUnit = resolveUnit(type, id, VersionRange.create(version));
        } else if (version != null && version.contains("|")) {
            String[] split = version.split("\\|", 2);
            resolvedUnit = resolveUnit(type, id, ArtifactMatcher.getVersionRangeFromImport(split[0], split[1]));
        } else {
            resolvedUnit = resolveUnit(type, id, ArtifactMatcher.parseAsOSGiVersion(version));
        }
        if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
            return new DefaultArtifactKey(type, id, resolvedUnit.getVersion().toString());
        }
        Collection<IArtifactKey> artifacts = resolvedUnit.getArtifacts();
        if (artifacts.size() == 1) {
            IArtifactKey key = artifacts.iterator().next();
            if ("osgi.bundle".equals(key.getClassifier())) {
                return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, resolvedUnit.getId(),
                        resolvedUnit.getVersion().toString());
            }
        }
        return new DefaultArtifactKey(type, resolvedUnit.getId(), resolvedUnit.getVersion().toString());
    }

    @Override
    public final IInstallableUnit resolveUnit(String type, String id, Version version)
            throws IllegalArtifactReferenceException, DependencyResolutionException {
        VersionRange versionRange = ArtifactMatcher.getVersionRangeFromReference(version);
        return resolveUnit(type, id, versionRange);
    }

    @Override
    public IInstallableUnit resolveUnit(String type, String id, VersionRange versionRange)
            throws IllegalArtifactReferenceException, DependencyResolutionException {
        IInstallableUnit matchingUnit = ArtifactMatcher.resolveReference(type, id, versionRange, installableUnits);
        if (matchingUnit == null) {
            String message = type + " artifact with ID \"" + id + "\" and version matching \"" + versionRange
                    + "\" was not found in the target platform";
            String candidates = installableUnits.stream()
                    .sorted(Comparator.comparing(IInstallableUnit::getId).thenComparing(IInstallableUnit::getVersion))
                    .filter(iu -> iu.getId().contains(id)).map(iu -> iu.getId() + ":" + iu.getVersion())
                    .collect(Collectors.joining(System.lineSeparator()));
            if (!candidates.isBlank()) {
                message = message + ", did you probably mean: " + candidates;
            }
            throw new DependencyResolutionException(message);
        }
        return matchingUnit;
    }

    @Override
    public final ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return executionEnvironment;
    }

    @Override
    public final Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap() {
        return reactorProjectLookup;
    }

    @Override
    public final Map<IInstallableUnit, IArtifactFacade> getOriginalMavenArtifactMap() {
        return mavenArtifactLookup;
    }

    @Override
    public final File getLocalArtifactFile(IArtifactKey key) {
        return artifacts.getArtifactFile(key);
    }

    @Override
    public final void saveLocalMavenRepository() {
        localArtifactRepository.save();
    }

    @Override
    public File getArtifactLocation(org.eclipse.tycho.ArtifactKey artifact) {
        IArtifactKey p2Artifact = ArtifactTypeHelper.toP2ArtifactKey(artifact);
        if (p2Artifact != null) {
            return artifacts.getArtifactFile(p2Artifact);
        }
        return null;
    }

    @Override
    public boolean isFileAlreadyAvailable(org.eclipse.tycho.ArtifactKey artifact) {
        IArtifactKey p2Artifact = ArtifactTypeHelper.toP2ArtifactKey(artifact);
        if (p2Artifact != null) {
            return artifacts.isFileAlreadyAvailable(p2Artifact);
        }
        return false;
    }

}
