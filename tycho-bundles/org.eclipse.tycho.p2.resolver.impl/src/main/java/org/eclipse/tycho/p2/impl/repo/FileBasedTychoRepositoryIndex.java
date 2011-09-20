/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.repo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tycho.p2.repository.DefaultTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

/**
 * Simplistic local Maven repository index to allow efficient lookup of all installed Tycho
 * projects. The content is persisted in a local file.
 */
public class FileBasedTychoRepositoryIndex extends DefaultTychoRepositoryIndex {

    public static final String ARTIFACTS_INDEX_RELPATH = ".meta/p2-artifacts.properties";
    public static final String METADATA_INDEX_RELPATH = ".meta/p2-local-metadata.properties";

    private final File indexFile;

    private Set<GAV> addedGavs = new HashSet<GAV>();
    private Set<GAV> removedGavs = new HashSet<GAV>();

    private FileBasedTychoRepositoryIndex(File indexFile) {
        super();
        this.indexFile = indexFile;
        if (indexFile.isFile()) {
            lock();
            try {
                setGavs(read(new FileInputStream(indexFile)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                unlock();
            }
        }
    }

    private void lock() {
        // TODO add file locking
    }

    private void unlock() {
        // TODO add file locking
    }

    @Override
    public void addGav(GAV gav) {
        super.addGav(gav);
        this.addedGavs.add(gav);
        if (removedGavs.contains(gav)) {
            removedGavs.remove(gav);
        }
    }

    @Override
    public void removeGav(GAV gav) {
        super.removeGav(gav);
        this.removedGavs.add(gav);
        if (addedGavs.contains(gav)) {
            addedGavs.remove(gav);
        }
    }

    public void save() throws IOException {
        File parentDir = indexFile.getParentFile();
        if (!parentDir.isDirectory()) {
            parentDir.mkdirs();
        }
        lock();
        try {
            reconcile();
            write(new FileOutputStream(indexFile));
        } finally {
            unlock();
        }
    }

    private void reconcile() throws IOException {
        // re-read index from file system so that changes from other
        // processes which happened in the meantime are not discarded
        if (indexFile.isFile()) {
            setGavs(read(new FileInputStream(indexFile)));
            for (GAV addedGav : addedGavs) {
                addGav(addedGav);
            }
            for (GAV removedGav : removedGavs) {
                removeGav(removedGav);
            }
        }
        addedGavs.clear();
        removedGavs.clear();
    }

    public static TychoRepositoryIndex createMetadataIndex(File basedir) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, METADATA_INDEX_RELPATH));
    }

    public static TychoRepositoryIndex createArtifactsIndex(File basedir) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, ARTIFACTS_INDEX_RELPATH));
    }

}
