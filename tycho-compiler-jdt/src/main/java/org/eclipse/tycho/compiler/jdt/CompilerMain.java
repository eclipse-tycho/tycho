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
import java.util.List;

import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.env.IMultiModuleEntry;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * Several methods of JDT compiler {@link Main} are overridden to allow compiling in-process against
 * a JDK other than the current one. If {@link #setJavaHome(File)} is not invoked, this class
 * behaves just the same as its superclass.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class CompilerMain extends Main {

    private static final FilenameFilter POTENTIAL_ZIP_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return Util.isPotentialZipArchive(name);
        }
    };

    private File javaHome;
    private org.codehaus.plexus.logging.Logger mavenLogger;
    private String bootclasspathAccessRules;
    private boolean explicitBootClasspath = false;

    public CompilerMain(PrintWriter outWriter, PrintWriter errWriter, boolean systemExitWhenFinished,
            org.codehaus.plexus.logging.Logger logger) {
        super(outWriter, errWriter, systemExitWhenFinished);
        this.mavenLogger = logger;
    }

    public void setJavaHome(File javaHome) {
        this.javaHome = javaHome;
        mavenLogger.debug("Using javaHome: " + javaHome);
    }

    public void setBootclasspathAccessRules(String accessRules) {
        bootclasspathAccessRules = accessRules;
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
    protected ArrayList<FileSystem.Classpath> handleEndorseddirs(ArrayList<String> endorsedDirClasspaths) {
        if (explicitBootClasspath) {
            return new ArrayList<>();
        }
        if (javaHome != null && (endorsedDirClasspaths == null || endorsedDirClasspaths.size() == 0)) {
            ArrayList<Classpath> result = new ArrayList<>();
            scanForArchives(result, new File(javaHome, "lib/endorsed"));
            mavenLogger.debug("Using endorsed dirs: " + result);
            return result;
        } else {
            return super.handleEndorseddirs(endorsedDirClasspaths);
        }
    }

    @Override
    protected ArrayList<Classpath> handleExtdirs(ArrayList<String> extdirsClasspaths) {
        if (explicitBootClasspath) {
            return new ArrayList<>();
        }
        if (javaHome != null && (extdirsClasspaths == null || extdirsClasspaths.size() == 0)) {
            ArrayList<Classpath> result = new ArrayList<>();
            scanForArchives(result, new File(javaHome, "lib/ext"));
            mavenLogger.debug("Using ext dirs: " + result);
            return result;
        } else {
            return super.handleExtdirs(extdirsClasspaths);
        }
    }

    @Override
    protected ArrayList<Classpath> handleBootclasspath(ArrayList<String> bootclasspaths, String customEncoding) {
        ArrayList<Classpath> result = new ArrayList<>(DEFAULT_SIZE_CLASSPATH);
        if (bootclasspaths != null && bootclasspaths.size() != 0) {
            explicitBootClasspath = true;
            for (String path : bootclasspaths) {
                processPathEntries(DEFAULT_SIZE_CLASSPATH, result, path, customEncoding, false, true);
            }
        } else {
            if (javaHome != null) {
                File directoryToCheck;
                if (isMacOS() && hasClassesDirWithJars()) {//$NON-NLS-1$//$NON-NLS-2$
                    directoryToCheck = new File(javaHome, "../Classes");
                } else {
                    directoryToCheck = new File(javaHome, "lib");
                }
                scanForArchives(result, directoryToCheck);
                if (result.isEmpty()) {
                    mavenLogger.warn("No classpath entries for boot classpath found scanning java home " + javaHome);
                }
            } else {
                try {
                    Util.collectRunningVMBootclasspath(result);
                } catch (IllegalStateException e) {
                    this.logger.logWrongJDK();
                    this.proceed = false;
                    return null;
                }
            }
        }

        if (bootclasspathAccessRules != null) {
            List<String> pathsWithAccessRules = new ArrayList<>(result.size());
            for (Classpath resultPath : result) {
                if (resultPath instanceof IMultiModuleEntry) {
                    pathsWithAccessRules
                            .add(resultPath.getPath() + "/lib/" + JRTUtil.JRT_FS_JAR + bootclasspathAccessRules);
                } else {
                    pathsWithAccessRules.add(resultPath.getPath() + bootclasspathAccessRules);
                }
            }
            result.clear();
            for (String pathWithAccessRules : pathsWithAccessRules) {
                processPathEntries(DEFAULT_SIZE_CLASSPATH, result, pathWithAccessRules, customEncoding, false, true);
            }
        }
        mavenLogger.debug("Using boot classpath: " + result);
        return result;
    }

    private boolean hasClassesDirWithJars() {
        File classesDir = new File(javaHome, "../Classes");
        if (!classesDir.isDirectory()) {
            return false;
        }
        File[] jars = classesDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") && new File(dir, name).isFile();
            }

        });
        return jars != null && jars.length > 0;
    }

    protected boolean isMacOS() {
        return System.getProperty("os.name").startsWith("Mac");
    }

    private void scanForArchives(ArrayList<Classpath> classPathList, File dir) {
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
