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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;

/**
 * Simplistic local Maven repository index to allow efficient lookup of all installed Tycho
 * projects.
 */
public class LocalTychoRepositoryIndex extends DefaultTychoRepositoryIndex {
    private final File indexFile;

    public static final String ARTIFACTS_INDEX_RELPATH = ".meta/p2-artifacts.properties";

    public static final String METADATA_INDEX_RELPATH = ".meta/p2-local-metadata.properties";

    public LocalTychoRepositoryIndex(File basedir, String relpath) {
        this.indexFile = new File(basedir, relpath);
        try {
            this.gavs = read(new FileInputStream(indexFile));
        } catch (IOException e) {
            // lets assume index does not exist yet
            this.gavs = new LinkedHashSet<GAV>();
        }
    }

    public static void addProject(File basedir, String groupId, String artifactId, String version) throws IOException {
        lock(basedir);

        try {
            LocalTychoRepositoryIndex artifactsIndex = new LocalTychoRepositoryIndex(basedir, ARTIFACTS_INDEX_RELPATH);
            artifactsIndex.addProject(groupId, artifactId, version);
            artifactsIndex.save();

            LocalTychoRepositoryIndex metadataIndex = new LocalTychoRepositoryIndex(basedir, METADATA_INDEX_RELPATH);
            metadataIndex.addProject(groupId, artifactId, version);
            metadataIndex.save();
        } finally {
            unlock(basedir);
        }
    }

    public void save() throws IOException {
        indexFile.getParentFile().mkdirs();

        write(new FileOutputStream(indexFile));
    }

    public static void unlock(File basedir) {
        // TODO Auto-generated method stub

    }

    public static void lock(File basedir) {
        // TODO Auto-generated method stub

    }

}
