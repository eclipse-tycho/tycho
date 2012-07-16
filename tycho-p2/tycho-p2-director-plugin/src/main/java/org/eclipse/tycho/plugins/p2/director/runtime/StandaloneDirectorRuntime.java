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
package org.eclipse.tycho.plugins.p2.director.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxInstallationLaunchConfiguration;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.p2.tools.director.shared.AbstractDirectorApplicationCommand;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

/**
 * Eclipse installation with the p2 director application. This director runtime is itself a valid p2
 * installation and can therefore be used to install products with meta-requirements, e.g. for
 * custom touchpoint actions.
 */
public class StandaloneDirectorRuntime implements DirectorRuntime {

    private final File runtimeLocation;
    private final EquinoxLauncher launchHelper;
    private Logger logger;

    StandaloneDirectorRuntime(File runtimeLocation, EquinoxLauncher launchHelper, Logger logger) {
        this.runtimeLocation = runtimeLocation;
        this.launchHelper = launchHelper;
        this.logger = logger;
    }

    public Command newInstallCommand() {
        return new AbstractDirectorApplicationCommand() {

            public void execute() throws DirectorCommandException {

                List<String> programArguments = new ArrayList<String>();
                programArguments.add("-application");
                programArguments.add("org.eclipse.equinox.p2.director");
                programArguments.addAll(getDirectorApplicationArguments());

                LaunchConfiguration launch = new EquinoxInstallationLaunchConfiguration(runtimeLocation,
                        programArguments);

                logger.info("Using the standalone p2 Director to install the product...");
                int exitCode = launchHelper.execute(launch, 60);
                if (exitCode != 0) {
                    throw new DirectorCommandException("Call to p2 director application failed with exit code "
                            + exitCode + ". Program arguments were: " + programArguments + ".");
                }
            }
        };
    }
}
