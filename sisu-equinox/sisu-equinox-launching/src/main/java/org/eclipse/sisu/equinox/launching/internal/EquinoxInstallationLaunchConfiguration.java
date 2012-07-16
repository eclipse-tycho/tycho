/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.launching.LaunchConfiguration;

/**
 * Launch configuration for an Equinox installation in the standard Eclipse installation layout.
 */
public class EquinoxInstallationLaunchConfiguration implements LaunchConfiguration {

    private final File equinoxDirectory;
    private final String[] programArguments;
    private final File launcherJar;

    public EquinoxInstallationLaunchConfiguration(File equinoxDirectory, List<String> programArguments) {
        this.equinoxDirectory = equinoxDirectory;
        this.programArguments = programArguments.toArray(new String[0]);
        this.launcherJar = findLauncherJar(equinoxDirectory);
    }

    private static File findLauncherJar(File equinoxDirectory) {
        File pluginsDir = new File(equinoxDirectory, "plugins");
        File[] launchers = pluginsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("org.eclipse.equinox.launcher_");
            }
        });

        if (launchers.length == 0)
            throw new IllegalArgumentException("No launcher bundle found in " + pluginsDir);
        else if (launchers.length > 1)
            throw new IllegalArgumentException("Multiple versions of the launcher bundle found in " + pluginsDir);
        else
            return launchers[0];
    }

    public File getWorkingDirectory() {
        return equinoxDirectory;
    }

    public String getJvmExecutable() {
        return null;
    }

    public File getLauncherJar() {
        return launcherJar;
    }

    public String[] getVMArguments() {
        return new String[0];
    }

    public String[] getProgramArguments() {
        return programArguments;
    }

    public Map<String, String> getEnvironment() {
        return Collections.emptyMap();
    }

}
