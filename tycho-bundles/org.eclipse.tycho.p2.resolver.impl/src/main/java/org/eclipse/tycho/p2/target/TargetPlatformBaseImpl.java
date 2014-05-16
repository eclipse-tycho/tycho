/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactType;
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

    // TODO move implementation out of this class
    @Override
    public final org.eclipse.tycho.ArtifactKey resolveReference(String type, String id, String version) {
        Version parsedVersion = parseAsOSGiVersion(version);
        // TODO share code with AbstractDependenciesAction.getVersionRange(Version)?

        IQuery<IInstallableUnit> query = getQueryToResolve(type, id, parsedVersion);

        IQueryResult<IInstallableUnit> matchingIUs = query.perform(installableUnits.iterator());
        if (matchingIUs.isEmpty()) {
            throw new RuntimeException("Cannot resolve reference to " + type + " with ID '" + id + "' and version '"
                    + version + "'");
            // TODO list other available versions?
        }

        return new DefaultArtifactKey(type, id, matchingIUs.iterator().next().getVersion().toString());
    }

    private static Version parseAsOSGiVersion(String version) {
        try {
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            // TODO revise exception type
            throw new IllegalArgumentException("The version \"" + version + "\" is not a valid OSGi version");
        }
    }

    @SuppressWarnings("restriction")
    private static IQuery<IInstallableUnit> getQueryToResolve(String type, String id, Version version) {
        VersionRange range = getVersionRangeFromSpec(version);

        IQuery<IInstallableUnit> query;
        if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            IRequirement requirement = MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, id,
                    range, null, 1 /* min */, Integer.MAX_VALUE /* max */, true /* greedy */);
            query = QueryUtil.createMatchQuery(requirement.getMatches());
        } else if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type)) {
            query = QueryUtil.createPipeQuery(QueryUtil.createIUQuery(id, range), QueryUtil.createIUProductQuery());
        } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
            query = QueryUtil.createPipeQuery(QueryUtil.createIUQuery(id + ".feature.group", range),
                    QueryUtil.createIUGroupQuery());
        } else if (ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
            query = QueryUtil.createIUQuery(id, range);
        } else {
            // TODO revise exception type
            throw new IllegalArgumentException("Unknown artifact type '" + type + "'");
        }
        return QueryUtil.createLatestQuery(query);
    }

    private static VersionRange getVersionRangeFromSpec(Version version) {
        VersionRange range;
        if (version.getSegmentCount() > 3 && "qualifier".equals(version.getSegment(3))) {
            range = getRangeOfEquivalentVersions(version);
        } else if (Version.emptyVersion.equals(version)) {
            range = VersionRange.emptyRange;
        } else {
            range = getStrictRange(version);
        }
        return range;
    }

    private static VersionRange getStrictRange(Version version) {
        return new VersionRange(version, true, version, true);
    }

    /**
     * Returns a version range which includes "equivalent" versions, i.e. versions with the same
     * major, minor, and micro version.
     */
    private static VersionRange getRangeOfEquivalentVersions(Version version) {
        Integer major = (Integer) version.getSegment(0);
        Integer minor = (Integer) version.getSegment(1);
        Integer micro = (Integer) version.getSegment(2);
        VersionRange range = new VersionRange(Version.createOSGi(major, minor, micro), true, Version.createOSGi(major,
                minor, micro + 1), false);
        return range;
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

    // TODO make name match with getArtifactLocation?
    @Override
    public final File getLocalArtifactFile(IArtifactKey key) {
        return artifacts.getArtifactFile(key);
    }

    @Override
    public final void saveLocalMavenRepository() {
        localArtifactRepository.save();
    }

}
