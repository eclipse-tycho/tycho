/*******************************************************************************
 * Copyright (c) 2012, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.sisu.equinox.launching.LaunchConfiguration;

/**
 * Launch configuration for an Equinox installation in the standard Eclipse installation layout.
 */
public class EquinoxInstallationLaunchConfiguration implements LaunchConfiguration {

    private static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher_";
    private final File equinoxDirectory;
    private final String[] programArguments;
    private File launcherJar;

    public EquinoxInstallationLaunchConfiguration(File equinoxDirectory, List<String> programArguments) {
        this.equinoxDirectory = equinoxDirectory;
        this.programArguments = programArguments.toArray(new String[0]);
    }

    public static File findLauncherJar(File equinoxDirectory) {
        File pluginsDir = new File(equinoxDirectory, "plugins");
        File[] launchers = pluginsDir.listFiles((FilenameFilter) (dir, name) -> name.startsWith(EQUINOX_LAUNCHER));

        if (launchers == null || launchers.length == 0) {

            StringBuilder allFiles = new StringBuilder();
            allFiles.append(System.lineSeparator());
            try {
                Files.walkFileTree(getTarget(equinoxDirectory), new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        allFiles.append(dir);
                        allFiles.append(System.lineSeparator());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        allFiles.append(file);
                        allFiles.append(System.lineSeparator());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                allFiles.append(e);
            }

            throw new IllegalArgumentException("The launcher bundle " + EQUINOX_LAUNCHER + "*.jar was not found in "
                    + pluginsDir + ", files in directory: \r\n " + allFiles);
        } else if (launchers.length > 1)
            throw new IllegalArgumentException("Multiple versions of the launcher bundle found in " + pluginsDir);
        else
            return launchers[0];
    }

    private static Path getTarget(File dir) {
        if (dir.getName().equals("target")) {
            return dir.toPath();
        }
        return getTarget(dir.getParentFile());
    }

    public static File findConfigurationArea(File location) {
        return new File(location, "configuration");
    }

    @Override
    public File getWorkingDirectory() {
        return equinoxDirectory;
    }

    @Override
    public String getJvmExecutable() {
        return null;
    }

    @Override
    public File getLauncherJar() {
        if (launcherJar == null) {
            this.launcherJar = findLauncherJar(equinoxDirectory);
        }
        return launcherJar;
    }

    @Override
    public String[] getVMArguments() {
        return new String[0];
    }

    @Override
    public String[] getProgramArguments() {
        return programArguments;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return Collections.emptyMap();
    }

}
