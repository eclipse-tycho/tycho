/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
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
     * <li>root.folder.adir:rootfiles1
     * </ul>
     * Configurations like root.&lt;os.ws.arch&gt; is also supported here as well as
     * root.&lt;os.ws.arch&gt;.folder.&lt;subfolder&gt; Following wrongly specified cases are simply
     * ignored when trying to find root files<br>
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
     * @param destinationPath
     *            the relative path where the root files should be placed into or "" if they should
     *            be placed in the installation root folder
     */
    void addFilesFromPatterns(String[] paths, String destinationDir) {
        for (String path : paths) {
            RootFilePath rootFilePath = new RootFilePath(path, baseDir, destinationDir);
            target.addFiles(rootFilePath.toFileSet(useDefaultExcludes).scan());
        }
    }

    public static class RootFilePath {

        private static final String ABSOLUTE_PREFIX = "absolute:";
        private static final String FILE_PREFIX = "file:";

        private String path;
        private File baseDir;
        private boolean isAbsolute = false;
        private boolean isFile = false;
        private String destinationDir;

        public RootFilePath(String path, File baseDir, String destinationDir) {
            this.destinationDir = destinationDir;
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
            return new FileSet(fileSetBasedir, pattern, destinationDir, useDefaultExcludes);
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
