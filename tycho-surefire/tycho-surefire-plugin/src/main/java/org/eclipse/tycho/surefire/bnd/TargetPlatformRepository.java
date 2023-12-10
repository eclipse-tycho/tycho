/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.surefire.bnd;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.osgi.resource.InstallableUnitResource;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class TargetPlatformRepository extends ResourcesRepository implements RepositoryPlugin {

    private ReactorProject reactorProject;
    private TargetPlatform targetPlatform;

    public TargetPlatformRepository(ReactorProject reactorProject, TargetPlatform targetPlatform) {
        this.reactorProject = reactorProject;
        this.targetPlatform = targetPlatform;
        IArtifactRepository artifactRepository = targetPlatform.getArtifactRepository();
        Set<IInstallableUnit> allUnits = targetPlatform.getMetadataRepository().query(QueryUtil.ALL_UNITS, null)
                .toSet();
        for (IInstallableUnit unit : allUnits) {
            if (unit.getId().endsWith(".source")) {
                //source might be interesting in other context but here not really required so keep the item count low!
                continue;
            }
            if (unit.getId().endsWith(".feature.group") || unit.getId().equals(".feature.jar")) {
                //also features are interesting to resolve by standard OSGi resolver, but not at the moment...
                continue;
            }
            add(new InstallableUnitResource(unit, artifactRepository));
        }
    }

    @Override
    public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
            throws Exception {
        ArtifactKey artifact = targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, bsn,
                version.toString());
        if (artifact != null) {
            File location = targetPlatform.getArtifactLocation(artifact);
            if (location != null) {
                for (DownloadListener downloadListener : listeners) {
                    downloadListener.success(location, properties);
                }
            }
            return location;

        }
        return null;
    }

    @Override
    public List<String> list(String pattern) throws Exception {
        Instruction filter = null;
        if (pattern != null)
            filter = new Instruction(pattern);

        Set<String> result = new HashSet<>();
        IQueryResult<IInstallableUnit> query = targetPlatform.getMetadataRepository().query(QueryUtil.ALL_UNITS, null);
        for (IInstallableUnit iu : query) {
            for (IArtifactKey artifactKey : iu.getArtifacts()) {
                if (BundlesAction.OSGI_BUNDLE_CLASSIFIER.equals(artifactKey.getClassifier())) {
                    String id = artifactKey.getId();
                    if (filter == null || filter.matches(id)) {
                        result.add(id);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    @Override
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Version> versions(String bsn) throws Exception {
        SortedSet<Version> set = new TreeSet<>();
        for (IInstallableUnit iu : targetPlatform.getMetadataRepository().query(QueryUtil.createIUQuery(bsn), null)) {
            for (IArtifactKey artifactKey : iu.getArtifacts()) {
                if (BundlesAction.OSGI_BUNDLE_CLASSIFIER.equals(artifactKey.getClassifier())) {
                    set.add(new Version(artifactKey.getVersion().toString()));
                }
            }
        }
        return set;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public String getName() {
        return "TargetPlatform of " + reactorProject.getId();
    }

    @Override
    public String getLocation() {
        return reactorProject.getBasedir().toURI().toString();
    }

    @Override
    public List<Capability> findProvider(Requirement requirement) {
        // we probably can provide a more efficient one for identity than dump scanning? We could build up a map of IDs in the constructor!
        // do we get our Requirements passed here so can adapt/optimize here as well? e.g. for download requests with org.osgi.service.repository.RepositoryContent
        List<Capability> provider = super.findProvider(requirement);
//        if (provider.isEmpty()) {
//            System.out.println("TargetPlatformRepository( " + requirement + ") --> " + provider.size());
//        }
        return provider;
    }

}
