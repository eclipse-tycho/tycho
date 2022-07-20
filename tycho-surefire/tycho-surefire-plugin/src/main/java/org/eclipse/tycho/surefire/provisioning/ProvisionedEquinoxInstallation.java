/*******************************************************************************
 * Copyright (c) 2013, 2015 Red Hat Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 ******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import java.io.File;

import org.eclipse.sisu.equinox.launching.FrameworkInstallation;
import org.eclipse.sisu.equinox.launching.FrameworkInstallationDescription;
import org.eclipse.sisu.equinox.launching.internal.EquinoxInstallationLaunchConfiguration;
import org.eclipse.tycho.core.osgitools.BundleReader;

/**
 * This class provides an implementation of an {@link FrameworkInstallation} which represents an RCP
 * application which has been provisioned using p2 director.
 * 
 * @author mistria
 */
public class ProvisionedEquinoxInstallation implements FrameworkInstallation {

    private File location;
    private File launcherJar;
    private File configurationLocation;
    private FrameworkInstallationDescription description;

    public ProvisionedEquinoxInstallation(File location, BundleReader bundleReader) {
        this.location = location;
        description = new ProvisionedInstallationDescription(location, bundleReader);
    }

    @Override
    public File getLauncherJar() {
        if (launcherJar != null) {
            return launcherJar;
        }
        launcherJar = EquinoxInstallationLaunchConfiguration.findLauncherJar(location);
        return launcherJar;
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public File getConfigurationLocation() {
        // TODO should this be configurable?
        if (configurationLocation != null) {
            return configurationLocation;
        }
        configurationLocation = EquinoxInstallationLaunchConfiguration.findConfigurationArea(location);
        return configurationLocation;
    }

    @Override
    public FrameworkInstallationDescription getInstallationDescription() {
        return description;
    }

}
