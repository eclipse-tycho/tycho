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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
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

    private static final String ENCODING = "UTF8";
    private static final String EOL = "\n";

    private final File indexFile;
    private final MavenLogger logger;
    private FileLocker fileLocker;

    private Set<GAV> addedGavs = new HashSet<GAV>();
    private Set<GAV> removedGavs = new HashSet<GAV>();

    private FileBasedTychoRepositoryIndex(File indexFile, FileLockService fileLockService, MavenLogger logger) {
        super();
        this.indexFile = indexFile;
        this.fileLocker = fileLockService.getFileLocker(indexFile);
        this.logger = logger;
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
        fileLocker.lock();
    }

    private void unlock() {
        fileLocker.release();
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

    private void write(OutputStream outStream) throws IOException {
        Writer out = new OutputStreamWriter(new BufferedOutputStream(outStream), ENCODING);
        try {
            for (GAV gav : getProjectGAVs()) {
                out.write(gav.toExternalForm());
                out.write(EOL);
            }
            out.flush();
        } finally {
            out.close();
        }
    }

    private Set<GAV> read(InputStream inStream) throws IOException {
        LinkedHashSet<GAV> result = new LinkedHashSet<GAV>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, ENCODING));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                try {
                    GAV parsedGAV = GAV.parse(line);
                    result.add(parsedGAV);
                } catch (IllegalArgumentException e) {
                    logger.warn("Ignoring invalid line '" + line + "' in " + indexFile);
                }
            }
        } finally {
            reader.close();
        }
        return result;
    }

    public static TychoRepositoryIndex createMetadataIndex(File basedir, FileLockService fileLockService,
            MavenLogger logger) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, METADATA_INDEX_RELPATH), fileLockService, logger);
    }

    public static TychoRepositoryIndex createArtifactsIndex(File basedir, FileLockService fileLockService,
            MavenLogger logger) {
        return new FileBasedTychoRepositoryIndex(new File(basedir, ARTIFACTS_INDEX_RELPATH), fileLockService, logger);
    }

}
