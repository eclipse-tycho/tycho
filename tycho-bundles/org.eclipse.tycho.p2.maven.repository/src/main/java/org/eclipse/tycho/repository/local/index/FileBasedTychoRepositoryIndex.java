/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local.index;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

/**
 * Simplistic local Maven repository index to allow efficient lookup of all installed Tycho
 * projects. The content is persisted in a local file.
 */
public class FileBasedTychoRepositoryIndex implements TychoRepositoryIndex {

    public static final String ARTIFACTS_INDEX_RELPATH = ".meta/p2-artifacts.properties";
    public static final String METADATA_INDEX_RELPATH = ".meta/p2-local-metadata.properties";

    private static final String EOL = "\n";

    private final File indexFile;
    private final MavenLogger logger;
    private FileLocker fileLocker;

    private Set<GAV> addedGavs = new HashSet<>();
    private Set<GAV> removedGavs = new HashSet<>();
    private Set<GAV> gavs = new HashSet<>();
    private MavenContext mavenContext;

    private FileBasedTychoRepositoryIndex(File indexFile, FileLockService fileLockService, MavenContext mavenContext) {
        super();
        this.indexFile = indexFile;
        this.mavenContext = mavenContext;
        this.fileLocker = fileLockService.getFileLocker(indexFile);
        this.logger = mavenContext.getLogger();
        if (indexFile.isFile()) {
            lock();
            try {
                gavs = read(new FileInputStream(indexFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                unlock();
            }
        }
    }

    private void lock() {
        fileLocker.lock();
    }

    private void unlock() {
        fileLocker.release();
    }

    @Override
    public MavenContext getMavenContext() {
        return mavenContext;
    }

    @Override
    public synchronized Set<GAV> getProjectGAVs() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(gavs));
    }

    @Override
    public synchronized void addGav(GAV gav) {
        gavs.add(gav);
        this.addedGavs.add(gav);
        if (removedGavs.contains(gav)) {
            removedGavs.remove(gav);
        }
    }

    @Override
    public synchronized void removeGav(GAV gav) {
        gavs.remove(gav);
        this.removedGavs.add(gav);
        if (addedGavs.contains(gav)) {
            addedGavs.remove(gav);
        }
    }

    @Override
    public void save() throws IOException {
        File parentDir = indexFile.getParentFile();
        if (!parentDir.isDirectory()) {
            parentDir.mkdirs();
        }
        lock();
        try {
            reconcile();
            // minimize time window for corrupting the file by first writing to a temp file, then moving it
            File tempFile = File.createTempFile("index", "tmp", indexFile.getParentFile());
            write(new FileOutputStream(tempFile));
            if (indexFile.isFile()) {
                indexFile.delete();
            }
            tempFile.renameTo(indexFile);
        } finally {
            unlock();
        }
    }

    private synchronized void reconcile() throws IOException {
        // re-read index from file system so that changes from other
        // processes which happened in the meantime are not discarded
        if (indexFile.isFile()) {
            gavs = read(new FileInputStream(indexFile));
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

    private void write(OutputStream outStream) throws IOException {
        try (Writer out = new OutputStreamWriter(new BufferedOutputStream(outStream), StandardCharsets.UTF_8)) {
            for (GAV gav : getProjectGAVs()) {
                out.write(gav.toExternalForm());
                out.write(EOL);
            }
            out.flush();
        }
    }

    private Set<GAV> read(InputStream inStream) throws IOException {
        LinkedHashSet<GAV> result = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    GAV parsedGAV = GAV.parse(line);
                    result.add(parsedGAV);
                } catch (IllegalArgumentException e) {
                    logger.warn("Ignoring invalid line '" + line + "' in " + indexFile);
                }
            }
        }
        return result;
    }

    public static TychoRepositoryIndex createMetadataIndex(File basedir, FileLockService fileLockService,
            MavenContext context) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, METADATA_INDEX_RELPATH), fileLockService, context);
    }

    public static TychoRepositoryIndex createArtifactsIndex(File basedir, FileLockService fileLockService,
            MavenContext context) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, ARTIFACTS_INDEX_RELPATH), fileLockService, context);
    }

}
