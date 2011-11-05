/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * Several methods of JDT compiler {@link Main} are overridden to allow compiling in-process against
 * a JDK other than the current one. If {@link #setJavaHome(File)} is not invoked, this class
 * behaves just the same as its superclass.
 */
class CompilerMain extends Main {

    private static final FilenameFilter POTENTIAL_ZIP_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return Util.isPotentialZipArchive(name);
        }
    };

    private File javaHome;
    private org.codehaus.plexus.logging.Logger mavenLogger;

    public CompilerMain(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished,
            org.codehaus.plexus.logging.Logger logger) {
        super(outWriter, errWriter, systemExitWhenFinished);
        this.mavenLogger = logger;
    }

    public void setJavaHome(File javaHome) {
        this.javaHome = javaHome;
        mavenLogger.debug("Using javaHome: " + javaHome);
    }

    @Override
    public File getJavaHome() {
        if (javaHome == null) {
            return super.getJavaHome();
        } else {
            return javaHome;
        }
    }

    @Override
    protected ArrayList handleEndorseddirs(ArrayList endorsedDirClasspaths) {
        if (javaHome == null) {
            return super.handleEndorseddirs(endorsedDirClasspaths);
        }
        if (endorsedDirClasspaths == null) {
            endorsedDirClasspaths = new ArrayList(DEFAULT_SIZE_CLASSPATH);
        }
        scanForArchives(endorsedDirClasspaths, new File(javaHome, "lib/endorsed"));
        mavenLogger.debug("Using endorsed dirs: " + endorsedDirClasspaths);
        return endorsedDirClasspaths;
    }

    @Override
    protected ArrayList handleExtdirs(ArrayList extdirsClasspaths) {
        if (javaHome == null) {
            return super.handleExtdirs(extdirsClasspaths);
        }
        if (extdirsClasspaths == null) {
            extdirsClasspaths = new ArrayList(DEFAULT_SIZE_CLASSPATH);
        }
        scanForArchives(extdirsClasspaths, new File(javaHome, "lib/ext"));
        mavenLogger.debug("Using ext dirs: " + extdirsClasspaths);
        return extdirsClasspaths;
    }

    @Override
    protected ArrayList handleBootclasspath(ArrayList bootclasspaths, String customEncoding) {
        if (javaHome == null) {
            return super.handleBootclasspath(bootclasspaths, customEncoding);
        }
        final int bootclasspathsSize;
        if ((bootclasspaths != null) && ((bootclasspathsSize = bootclasspaths.size()) != 0)) {
            String[] paths = new String[bootclasspathsSize];
            bootclasspaths.toArray(paths);
            bootclasspaths.clear();
            for (int i = 0; i < bootclasspathsSize; i++) {
                processPathEntries(DEFAULT_SIZE_CLASSPATH, bootclasspaths, paths[i], customEncoding, false, true);
            }
        } else {
            bootclasspaths = new ArrayList(DEFAULT_SIZE_CLASSPATH);
            File directoryToCheck;
            if (System.getProperty("os.name").startsWith("Mac")) {//$NON-NLS-1$//$NON-NLS-2$
                directoryToCheck = new File(javaHome, "../Classes");
            } else {
                directoryToCheck = new File(javaHome, "lib");
            }
            scanForArchives(bootclasspaths, directoryToCheck);
        }
        if (bootclasspaths.isEmpty()) {
            mavenLogger.warn("No classpath entries for boot classpath found scanning java home " + javaHome);
        }
        mavenLogger.debug("Using boot classpath: " + bootclasspaths);
        return bootclasspaths;
    }

    private void scanForArchives(ArrayList classPathList, File dir) {
        if (dir.isDirectory()) {
            File[] zipFiles = dir.listFiles(POTENTIAL_ZIP_FILTER);
            if (zipFiles != null) {
                for (File zipFile : zipFiles) {
                    classPathList.add(FileSystem.getClasspath(zipFile.getAbsolutePath(), null, null));
                }
            }
        }
    }

}
