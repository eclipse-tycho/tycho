/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - cache target definition resolution result (bug 373806)
 *    Christoph Läubrich    - [Bug 538144] - support other target locations (Directory, Feature, Installations)
 *                          - [Bug 533747] - Target file is read and parsed over and over again
 *                          - [Bug 568729] - Support new "Maven" Target location
 *                          - [Bug 569481] - Support for maven target location includeSource="true" attribute
 *                          - [Issue 189]  - Support multiple maven-dependencies for one target location
 *                          - [Issue 194]  - Support additional repositories defined in the maven-target location
 *                          - [Issue 401]  - Support nested targets
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;

public final class TargetDefinitionFile {

    public static final String FILE_EXTENSION = ".target";

    /**
     * List all target files in the given folder
     * 
     * @param folder
     * @return the found target files or empty array if nothing was found, folder is not a directory
     *         or the directory could not be read
     */
    public static File[] listTargetFiles(File folder) {
        if (folder.isDirectory()) {
            File[] targetFiles = folder.listFiles(TargetDefinitionFile::isTargetFile);
            if (targetFiles != null) {
                return targetFiles;
            }
        }
        return new File[0];
    }

    /**
     * 
     * @param file
     * @return <code>true</code> if the given files likely denotes are targetfile based on file
     *         naming, <code>false</code> otherwise
     */
    public static boolean isTargetFile(File file) {
        return file != null && file.isFile()
                && file.getName().toLowerCase().endsWith(TargetDefinitionFile.FILE_EXTENSION)
                && !file.getName().startsWith(".polyglot.");
    }

}
