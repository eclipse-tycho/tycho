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
 *******************************************************************************/
package org.eclipse.tycho.core.bnd;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

import aQute.bnd.osgi.Instruction;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

@Named(TargetPlatformRepository.HINT)
@Singleton
public class TargetPlatformRepository implements RepositoryPlugin {

    static final String HINT = "tycho-target-platform";

    @Inject
    private TargetPlatformService targetPlatformService;

    @Override
    public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
            throws Exception {
        TargetPlatform targetPlatform = targetPlatformService.getTargetPlatform().orElse(null);
        if (targetPlatform == null) {
            return null;
        }
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
        TargetPlatform targetPlatform = targetPlatformService.getTargetPlatform().orElse(null);
        if (targetPlatform == null) {
            return List.of();
        }
        Instruction filter = null;
        if (pattern != null) {
            filter = new Instruction(pattern);
        }

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
    public SortedSet<Version> versions(String bsn) throws Exception {
        TargetPlatform targetPlatform = targetPlatformService.getTargetPlatform().orElse(null);
        if (targetPlatform == null) {
            return Collections.emptySortedSet();
        }
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
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public String getName() {
        return "Tycho Target Platform";
    }

    @Override
    public String getLocation() {
        return HINT;
    }

}
