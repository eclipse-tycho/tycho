/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.equinox.launching.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.equinox.EquinoxRuntimeLocator;
import org.eclipse.tycho.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.tycho.equinox.launching.EquinoxInstallation;
import org.eclipse.tycho.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.tycho.equinox.launching.EquinoxLauncher;

/**
 * Convenience wrapper around {@link Commandline} to run Eclipse applications from tycho-p2-runtime
 * 
 * @author igor
 */
@Component(role = P2ApplicationLauncher.class, instantiationStrategy = "per-lookup")
public class P2ApplicationLauncher {
    @Requirement
    private Logger logger;

    @Requirement
    private EquinoxInstallationFactory installationFactory;

    @Requirement
    private EquinoxLauncher launcher;

    @Requirement
    private EquinoxRuntimeLocator runtimeLocator;

    @Requirement(role = TychoProject.class, hint = ArtifactKey.TYPE_ECLIPSE_PLUGIN)
    private OsgiBundleProject osgiBundle;

    private File workingDirectory;

    private String applicationName;

    private final List<String> vmargs = new ArrayList<String>();

    private final List<String> args = new ArrayList<String>();

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void addArguments(String... args) {
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    public void addVMArguments(String... vmargs) {
        for (String vmarg : vmargs) {
            this.vmargs.add(vmarg);
        }
    }

    public int execute(int forkedProcessTimeoutInSeconds) {
        try {
            File installationFolder = newTemporaryFolder();

            try {
                EquinoxInstallationDescription description = new DefaultEquinoxInstallationDescription();

                List<File> locations = runtimeLocator.getRuntimeLocations();

                for (File location : locations) {
                    if (location.isDirectory()) {
                        for (File file : new File(location, "plugins").listFiles()) {
                            addBundle(description, file);
                        }
                    } else {
                        addBundle(description, location);
                    }
                }

                EquinoxInstallation installation = installationFactory.createInstallation(description,
                        installationFolder);

                EquinoxLaunchConfiguration launchConfiguration = new EquinoxLaunchConfiguration(installation);
                launchConfiguration.setWorkingDirectory(workingDirectory);

                // logging

                if (logger.isDebugEnabled()) {
                    launchConfiguration.addProgramArguments("-debug", "-consoleLog");
                }

                // application and application arguments

                launchConfiguration.addProgramArguments("-nosplash", "-application", applicationName);

                launchConfiguration.addProgramArguments(true, args.toArray(new String[args.size()]));

                return launcher.execute(launchConfiguration, forkedProcessTimeoutInSeconds);
            } finally {
                try {
                    FileUtils.deleteDirectory(installationFolder);
                } catch (IOException e) {
                    // this may happen if child process did not close all file handles
                    logger.warn("Failed to delete temp folder " + installationFolder);
                }
            }
        } catch (Exception e) {
            // TODO better exception?
            throw new RuntimeException(e);
        }
    }

    private void addBundle(EquinoxInstallationDescription description, File file) {
        ArtifactKey key = osgiBundle.readArtifactKey(file);
        if (key != null) {
            description.addBundle(key, file);
        }
    }

    private File newTemporaryFolder() throws IOException {
        File tmp = File.createTempFile("tycho-p2-runtime", ".tmp");
        tmp.delete();
        tmp.mkdir(); // anyone got any better idea?
        return tmp;
    }
}
