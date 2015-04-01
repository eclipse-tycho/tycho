/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.launching.LaunchConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class EquinoxInstallationLaunchConfigurationTest {

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Test
    public void getLauncherJar_normalLayout() throws Exception {
        File[] layout = makeNormalInstallationLayout();

        LaunchConfiguration launchConfiguration = new EquinoxInstallationLaunchConfiguration(layout[0], noProgramArgs());
        assertEquals(layout[1], launchConfiguration.getLauncherJar());
    }

    @Test
    public void getLauncherJar_MacOsLayout() throws Exception {
        File[] layout = makeMacOsInstallationLayout();

        LaunchConfiguration launchConfiguration = new EquinoxInstallationLaunchConfiguration(layout[0], noProgramArgs());
        assertEquals(layout[1], launchConfiguration.getLauncherJar());
    }

    private static List<String> noProgramArgs() {
        return Collections.<String> emptyList();
    }

    private File[] makeNormalInstallationLayout() throws IOException {
        Path instDir = rootDir.newFolder("installation").toPath();
        Path pluginsDir = Files.createDirectory(instDir.resolve("plugins"));
        Path launcher = Files.createTempFile(pluginsDir, "org.eclipse.equinox.launcher_", ".jar");
        return new File[] { instDir.toFile(), launcher.toFile() };
    }

    private File[] makeMacOsInstallationLayout() throws IOException {
        Path instDir = rootDir.newFolder("installation.app").toPath();
        Path pluginsDir = Files.createDirectories(instDir.resolve("Contents/Eclipse/plugins"));
        Path launcher = Files.createTempFile(pluginsDir, "org.eclipse.equinox.launcher_", ".jar");
        return new File[] { instDir.toFile(), launcher.toFile() };
    }
}
