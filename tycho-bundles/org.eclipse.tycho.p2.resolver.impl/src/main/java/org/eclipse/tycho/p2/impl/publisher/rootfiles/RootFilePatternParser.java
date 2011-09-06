/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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

public class RootFilePatternParser {

    private final File baseDir;

    private final RootFilesProperties target;

    private boolean useDefaultExcludes;

    public RootFilePatternParser(File baseDir, RootFilesProperties target, boolean useDefaultExcludes) {
        this.baseDir = baseDir;
        this.target = target;
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * According to Eclipse Help > Help Contents: Plug-in Development Environment Guide > Tasks >
     * PDE Build Advanced Topics > Adding Files to the Root of a Build, value(s) of root are a comma
     * separated list of relative paths to folder(s). The contents of the folder are included as
     * root files to the installation. Exception are if a list value starts with: 'file:',
     * 'absolute:' or 'absolute:file:'. 'file:' indicates that the included content is a file only.
     * 'absolute:' indicates that the path is absolute. Examples:
     * <ul>
     * <li>root=rootfiles1, rootfiles2, license.html
     * <li>root=file:license.html
     * <li>root=absolute:/rootfiles1
     * <li>root=absolute:file:/eclipse/about.html
     * </ul>
     * Configurations like root.<os.ws.arch> is also supported here but subfolders so far are not
     * supported. <br>
     * Following wrongly specified cases are simply ignored when trying to find root files<br>
     * <ol>
     * <li>root = license.html -> licence.html exists but is not a directory (contrary to PDE
     * product export where build fails )
     * <li>root = file:not_existing_file.txt, not_existing_dir -> specified file or directory does
     * not exist
     * <li>root = file:C:/_tmp/file_absolute.txt -> existing file with absolute path;but not
     * specified as absolute
     * <li>root = file:absolute:C:/_tmp/file_absolute.txt -> Using 'file:absolute:' (instead of
     * correct 'absolute:file:')
     * </ol>
     * 
     * @param paths
     *            root file paths
     */
    void addFilesFromPatterns(String[] paths) {
        for (String path : paths) {
            RootFilePath rootFilePath = new RootFilePath(path, baseDir);
            target.addFiles(rootFilePath.toFileSet(useDefaultExcludes).scan());
        }
    }

    static class RootFilePath {

        private static final String ABSOLUTE_PREFIX = "absolute:";
        private static final String FILE_PREFIX = "file:";

        private String path;
        private File baseDir;
        private boolean isAbsolute = false;
        private boolean isFile = false;

        public RootFilePath(String path, File baseDir) {
            this.path = parse(path);
            this.baseDir = baseDir;
        }

        public FileSet toFileSet(boolean useDefaultExcludes) {
            File file = isAbsolute ? new File(path) : new File(baseDir, path);
            String pattern;
            File fileSetBasedir;
            if (isFile) {
                fileSetBasedir = file.getParentFile();
                pattern = file.getName();
            } else {
                fileSetBasedir = file;
                pattern = "**/*";
            }
            return new FileSet(fileSetBasedir, pattern, useDefaultExcludes);
        }

        private String parse(String path) {
            if (path.startsWith(ABSOLUTE_PREFIX)) {
                isAbsolute = true;
                path = path.substring(ABSOLUTE_PREFIX.length());
            }
            if (path.startsWith(FILE_PREFIX)) {
                isFile = true;
                path = path.substring(FILE_PREFIX.length());
            }
            return path;
        }

    }

}
