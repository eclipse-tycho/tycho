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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class RootFilePatternParser {

    private final File baseDir;

    private final RootFilesProperties target;

    public RootFilePatternParser(File baseDir, RootFilesProperties target) {
        this.baseDir = baseDir;
        this.target = target;
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
     * Configurations like root.<os.ws.arch> is also supported here but patterns, subfolder and
     * permissions so far are not supported. <br>
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
     * @param rootFileEntryValue
     *            specified comma separated root files
     * @return the root files information parsed from the <code>rootFileEntryValue</code> parameter.
     *         If parsing lead to non valid root files cases then an empty Map is returned.
     */
    void addFilesFromPatterns(String[] paths) {
        for (String path : paths) {
            target.addFiles(collectRootFilesMap(parseRootFilePath(path, baseDir)));
        }
    }

    static File parseRootFilePath(String path, File baseDir) {
        boolean isAbsolute = false;
        final String absoluteString = "absolute:";
        if (path.startsWith(absoluteString)) {
            isAbsolute = true;
            path = path.substring(absoluteString.length());
        }

        String fileString = "file:";
        if (path.startsWith(fileString)) {
            path = path.substring(fileString.length());
        }

        return (isAbsolute ? new File(path) : new File(baseDir, path).getAbsoluteFile());
        // TODO we should prevent non-absolute "absolute:" paths
    }

    static Map<File, IPath> collectRootFilesMap(File rootFile) {
        if (rootFile.isFile()) {
            return Collections.singletonMap(rootFile, Path.fromOSString(rootFile.getName()));
        }
        return collectRootFilesMap(rootFile, Path.fromOSString(rootFile.getAbsolutePath()));
    }

    static Map<File, IPath> collectRootFilesMap(File file, IPath basePath) {
        Map<File, IPath> files = new HashMap<File, IPath>();

        if (!file.exists())
            return Collections.emptyMap();
        File[] dirFiles = file.listFiles();
        for (File dirFile : dirFiles) {
            files.put(dirFile, Path.fromOSString(dirFile.getAbsolutePath()).makeRelativeTo(basePath));
            if (dirFile.isDirectory()) {
                files.putAll(collectRootFilesMap(dirFile, basePath));
            }
        }
        return files;
    }

}
