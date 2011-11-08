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
import java.util.Collections;
import java.util.Set;

/**
 * Simplistic local Maven repository index to allow efficient lookup of all installed Tycho
 * projects. The content is persisted in a local file.
 */
public class FileBasedTychoRepositoryIndex extends DefaultTychoRepositoryIndex {
    private final File storageFile;

    public static final String ARTIFACTS_INDEX_RELPATH = ".meta/p2-artifacts.properties";

    public static final String METADATA_INDEX_RELPATH = ".meta/p2-local-metadata.properties";

    private FileBasedTychoRepositoryIndex(File indexFile, Set<GAV> content) {
        super(content);
        this.storageFile = indexFile;
    }

    public void save() throws IOException {
        storageFile.getParentFile().mkdirs();
        DefaultTychoRepositoryIndex.write(this, new FileOutputStream(storageFile));
    }

    public static TychoRepositoryIndex createRepositoryIndex(File basedir, String relpath) {
        File inputFile = new File(basedir, relpath);
        Set<GAV> content = Collections.emptySet();
        if (inputFile.exists()) {
            try {
                content = DefaultTychoRepositoryIndex.read(new FileInputStream(inputFile));
            } catch (IOException e) {
                throw new RuntimeException("Unable to load index file " + inputFile, e);
            }
        }
        return new FileBasedTychoRepositoryIndex(inputFile, content);
    }

    public static void addProject(File basedir, String groupId, String artifactId, String version) throws IOException {
        TychoRepositoryIndex artifactsIndex = FileBasedTychoRepositoryIndex.createRepositoryIndex(basedir,
                ARTIFACTS_INDEX_RELPATH);
        artifactsIndex.addProject(new GAV(groupId, artifactId, version));
        artifactsIndex.save();

        TychoRepositoryIndex metadataIndex = FileBasedTychoRepositoryIndex.createRepositoryIndex(basedir,
                METADATA_INDEX_RELPATH);
        metadataIndex.addProject(new GAV(groupId, artifactId, version));
        metadataIndex.save();
    }
}
