/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Ant-like file set. Wildcards *, ** and ? are implemented as described on <br/>
 * {@link http://en.wikibooks.org/wiki/Apache_Ant/Fileset }. This is not a complete equivalent
 * implementation of the ant fileset. Only the subset needed for PDE root files is supported.
 */
public class FileSet extends AbstractFileSet {

    private File baseDir;

    /**
     * Equivalent to {@link #FileSet(File, String, boolean)} with useDefaultExludes == true.
     */
    public FileSet(File baseDir, String pattern) {
        this(baseDir, pattern, true);
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
    public FileSet(File baseDir, String pattern, boolean useDefaultExcludes) {
        super(pattern, useDefaultExcludes);
        this.baseDir = baseDir;
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
        recursiveScan(baseDir, result, Path.fromOSString(baseDir.getAbsolutePath()));
        return result;
    }

    private void recursiveScan(File file, FileToPathMap result, IPath baseDirPath) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile.isDirectory()) {
                    recursiveScan(subFile, result, baseDirPath);
                } else if (subFile.isFile()) {
                    addFileIfMatch(subFile, result, baseDirPath);
                }
            }
        } else if (file.isFile()) {
            addFileIfMatch(file, result, baseDirPath);
        }
    }

    private void addFileIfMatch(File file, FileToPathMap result, IPath baseDir) {
        IPath relativePath = Path.fromOSString(file.getAbsolutePath()).makeRelativeTo(baseDir);
        if (matches(relativePath)) {
            result.put(file, relativePath);
        }
    }
}
