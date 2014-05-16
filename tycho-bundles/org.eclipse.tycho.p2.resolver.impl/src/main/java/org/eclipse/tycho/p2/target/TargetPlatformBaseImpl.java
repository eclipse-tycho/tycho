/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.ArtifactType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.ArtifactType.TYPE_ECLIPSE_PLUGIN;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

abstract class TargetPlatformBaseImpl implements P2TargetPlatform {

    // content

    /**
     * All installable units contained in the target platform. This includes reactor-external
     * content and all results of upstream reactor projects (or all projects in case of the
     * preliminary target platform where the reactor build order isn't known yet). Configured and
     * automatic filters have been applied.
     */
    private final LinkedHashSet<IInstallableUnit> installableUnits;

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

    final IRawArtifactFileProvider jointArtifacts;
    @Deprecated
    private LocalArtifactRepository localArtifactRepository;

    public TargetPlatformBaseImpl(LinkedHashSet<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup) {
        this.installableUnits = installableUnits;
        this.executionEnvironment = executionEnvironment;
        this.reactorProjectLookup = reactorProjectLookup;
        this.mavenArtifactLookup = mavenArtifactLookup;
        this.jointArtifacts = jointArtifacts;
        this.localArtifactRepository = localArtifactRepository;
    }

    public final Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    public final org.eclipse.tycho.ArtifactKey resolveReference(String type, String id, String version) {
        Version parsedVersion = Version.parseVersion(version);
        // TODO check is OSGi
        // TODO share code with AbstractDependenciesAction.getVersionRange(Version)?

        IQuery<IInstallableUnit> query = getQueryToResolve(id, parsedVersion);

        IQueryResult<IInstallableUnit> matchingIUs = query.perform(installableUnits.iterator());
        if (matchingIUs.isEmpty()) {
            throw new RuntimeException("Cannot resolve reference to " + type + " with ID " + id + " and version "
                    + version);
        }

        return new DefaultArtifactKey(type, id, matchingIUs.iterator().next().getVersion().toString());
    }

    private IQuery<IInstallableUnit> getQueryToResolve(String id, Version version) {
        IQuery<IInstallableUnit> query;
        if (version.getSegmentCount() > 3 && "qualifier".equals(version.getSegment(3))) {
            VersionRange range = getRangeOfEquivalentVersions(version);
            query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id, range));
        } else {
            query = QueryUtil.createIUQuery(id, version);
        }
        return query;
    }

    /**
     * Returns a version range which includes "equivalent" versions, i.e. versions with the same
     * major, minor, and micro version.
     */
    private VersionRange getRangeOfEquivalentVersions(Version version) {
        Integer major = (Integer) version.getSegment(0);
        Integer minor = (Integer) version.getSegment(1);
        Integer micro = (Integer) version.getSegment(2);
        VersionRange range = new VersionRange(Version.createOSGi(major, minor, micro), true, Version.createOSGi(major,
                minor, micro + 1), false);
        return range;
    }

    public final ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return executionEnvironment;
    }

    public final Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap() {
        return reactorProjectLookup;
    }

    public final Map<IInstallableUnit, IArtifactFacade> getOriginalMavenArtifactMap() {
        return mavenArtifactLookup;
    }

    // TODO make name match with getArtifactLocation?
    public final File getLocalArtifactFile(IArtifactKey key) {
        return jointArtifacts.getArtifactFile(key);
    }

    // TODO test
    public File getArtifactLocation(org.eclipse.tycho.ArtifactKey artifact) {
        IArtifactKey p2Artifact = toP2ArtifactKey(artifact);
        if (p2Artifact != null) {
            return jointArtifacts.getArtifactFile(p2Artifact);
        }
        return null;
    }

    // TODO share?
    @SuppressWarnings("restriction")
    private static IArtifactKey toP2ArtifactKey(org.eclipse.tycho.ArtifactKey artifact) {
        if (TYPE_ECLIPSE_PLUGIN.equals(artifact.getType())) {
            return createP2ArtifactKey(PublisherHelper.OSGI_BUNDLE_CLASSIFIER, artifact);
        } else if (TYPE_ECLIPSE_FEATURE.equals(artifact.getType())) {
            return createP2ArtifactKey(PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER, artifact);
        } else {
            // other artifacts don't have files that can be referenced by their Eclipse coordinates
            return null;
        }
    }

    @SuppressWarnings("restriction")
    private static IArtifactKey createP2ArtifactKey(String type, org.eclipse.tycho.ArtifactKey artifact) {
        return new org.eclipse.equinox.internal.p2.metadata.ArtifactKey(type, artifact.getId(),
                Version.parseVersion(artifact.getVersion()));
    }

    public final void saveLocalMavenRepository() {
        localArtifactRepository.save();
    }

}
