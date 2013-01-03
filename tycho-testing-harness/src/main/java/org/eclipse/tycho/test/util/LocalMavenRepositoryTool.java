/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = LocalMavenRepositoryTool.class)
public class LocalMavenRepositoryTool {

    private File localRepo;

    @Requirement
    private FileLockService fileLockService;

    /**
     * Creates a default instance of the <code>LocalRepositoryUtils</code>, using the local Maven
     * repository location obtained from {@link EnvironmentUtil}.
     */
    public LocalMavenRepositoryTool() {
        localRepo = new File(EnvironmentUtil.getLocalRepo());
        if (!localRepo.isDirectory()) {
            throw new IllegalStateException("Local Maven repository does not exist: " + localRepo);
        }
    }

    public File getArtifactFile(String groupId, String artifactId, String version, String classifier, String extension) {
        String groupPath = groupId.replace('.', '/');
        String artifactPath = groupPath + '/' + artifactId + '/' + version;
        String artifactName = artifactId + "-" + version + (classifier == null ? "" : "-" + classifier) + "."
                + extension;
        return new File(localRepo, artifactPath + '/' + artifactName);
    }

    public Set<String> getArtifactIndexLines() throws IOException {
        File indexFile = getArtifactIndexFile();
        FileLocker locker = fileLockService.getFileLocker(indexFile);
        locker.lock();
        try {
            return readLines(indexFile);
        } finally {
            locker.release();
        }
    }

    public void removeLinesFromArtifactsIndex(String... linesToBeRemoved) throws IOException {
        Set<String> toBeRemoved = new HashSet<String>(Arrays.asList(linesToBeRemoved));
        File indexFile = getArtifactIndexFile();
        filterLinesFromIndex(indexFile, toBeRemoved);
    }

    /**
     * Hides specified artifacts built and installed by Tycho from future Tycho builds by removing
     * the corresponding lines with GAV coordinates format "g:a:v" from the local metadata index
     * file.
     * <p>
     * Background: Artifacts built and installed by Tycho are visible to all other Tycho builds in
     * order to allow re-builds of individual modules. This method allows to prevent this for future
     * builds.
     * 
     * TODO should rather reuse FileBasedTychoRepositoryIndex here
     * 
     * @throws IOException
     *             if index file could not be saved
     */
    public void removeLinesFromMetadataIndex(String... linesToBeRemoved) throws IOException {
        Set<String> toBeRemoved = new HashSet<String>(Arrays.asList(linesToBeRemoved));
        File indexFile = new File(localRepo, ".meta/p2-local-metadata.properties");
        filterLinesFromIndex(indexFile, toBeRemoved);
    }

    private void filterLinesFromIndex(File indexFile, Set<String> toBeRemoved) throws UnsupportedEncodingException,
            FileNotFoundException, IOException {
        FileLocker locker = fileLockService.getFileLocker(indexFile);
        locker.lock();
        try {
            Set<String> currentLines = readLines(indexFile);
            currentLines.removeAll(toBeRemoved);
            writeLines(indexFile, currentLines);
        } finally {
            locker.release();
        }
    }

    private File getArtifactIndexFile() {
        return new File(localRepo, ".meta/p2-artifacts.properties");
    }

    private Set<String> readLines(File indexFile) throws UnsupportedEncodingException, FileNotFoundException,
            IOException {
        Set<String> lines = new LinkedHashSet<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile), "UTF-8"));
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
        } finally {
            reader.close();
        }
        return lines;
    }

    private void writeLines(File indexFile, Collection<String> lines) throws UnsupportedEncodingException,
            FileNotFoundException, IOException {
        PrintStream writer = new PrintStream(indexFile);
        try {
            for (String line : lines) {
                writer.println(line);
            }
        } finally {
            writer.close();
        }
    }

}
