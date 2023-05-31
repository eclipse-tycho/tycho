/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Bachmann electronic GmbH - adding support for root.folder and root.<config>.folder
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher.rootfiles;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Ant-like file set. Wildcards *, ** and ? are implemented as described on <br/>
 * {@link https://en.wikibooks.org/wiki/Apache_Ant/Fileset }. This is not a complete equivalent
 * implementation of the ant fileset. Only the subset needed for PDE root files is supported.
 */
public class FileSet extends AbstractFileSet {

    private File baseDir;
    private String destinationDir;

    /**
     * Equivalent to {@link #FileSet(File, String, boolean)} with useDefaultExludes == true;
     * destinationDir = ""
     */
    public FileSet(File baseDir, String pattern) {
        this(baseDir, pattern, "", true);
    }

    /**
     * Creates a fileset.
     * 
     * @param baseDir
     *            the base directory to scan
     * @param pattern
     *            ant file inclusion pattern (relative to baseDir). Wildcards **,* and ? are
     *            supported.
     * @param useDefaultExcludes
     *            whether to use default file excludes for typical SCM metadata files.
     */
    public FileSet(File baseDir, String pattern, String destinationDir, boolean useDefaultExcludes) {
        super(pattern, useDefaultExcludes);
        this.baseDir = baseDir;
        this.destinationDir = destinationDir;
    }

    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Scan the filesystem below baseDir for matching files.
     * 
     * @return map canonical File -> basedir-relative path
     */
    public FileToPathMap scan() {
        FileToPathMap result = new FileToPathMap();
        recursiveScan(baseDir, result, Path.fromOSString(baseDir.getAbsolutePath()), Path.fromOSString(destinationDir));
        return result;
    }

    private void recursiveScan(File file, FileToPathMap result, IPath baseDirPath, IPath destinationPath) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile.isDirectory()) {
                    recursiveScan(subFile, result, baseDirPath, destinationPath);
                } else if (subFile.isFile()) {
                    addFileIfMatch(subFile, result, baseDirPath, destinationPath);
                }
            }
        } else if (file.isFile()) {
            addFileIfMatch(file, result, baseDirPath, destinationPath);
        }
    }

    private void addFileIfMatch(File file, FileToPathMap result, IPath baseDir, IPath destination) {
        IPath relativePath = Path.fromOSString(file.getAbsolutePath()).makeRelativeTo(baseDir);
        IPath destinationPath = destination.append(relativePath);
        if (matches(relativePath)) {
            result.put(file, destinationPath);
        }
    }
}
