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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.tycho.ResolvedArtifactKey;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Instruction;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class ArtifactKeyRepository extends FileSetRepository implements RepositoryPlugin {
    private List<ResolvedArtifactKey> bundles;
    private String name;
    private File location;

    public ArtifactKeyRepository(List<ResolvedArtifactKey> bundles, String name, File location) throws Exception {
        super(name, bundles.stream().map(ResolvedArtifactKey::getLocation).toList());
        this.bundles = bundles;
        this.name = name;
        this.location = location;
    }

    @Override
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
            throws Exception {
        File file = bundles.stream().filter(a -> a.getId().equals(bsn))
                .filter(a -> version.compareTo(new Version(a.getVersion())) == 0).findFirst()
                .map(ResolvedArtifactKey::getLocation).orElse(null);
        if (file != null) {
            for (DownloadListener downloadListener : listeners) {
                downloadListener.success(file, properties);
            }
        }
        return file;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public List<String> list(String pattern) throws Exception {
        Stream<String> stream = bundles.stream().map(ResolvedArtifactKey::getId);
        if (pattern != null) {
            Instruction filter = new Instruction(pattern);
            stream = stream.filter(filter::matches);
        }
        return stream.toList();
    }

    @Override
    public SortedSet<Version> versions(String bsn) throws Exception {
        return bundles.stream().filter(a -> a.getId().equals(bsn)).map(a -> new Version(a.getVersion()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLocation() {
        return location.getAbsolutePath();
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> map = super.findProviders(requirements);
//        map.forEach((r, c) -> {
//            if (c.isEmpty()) {
//                System.out.println("ArtifactKeyRepository(" + r + ") --> " + c.size());
//            }
//        });
        return map;
    }
}
