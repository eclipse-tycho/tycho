/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 ******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import java.io.File;

import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.internal.EquinoxInstallationLaunchConfiguration;
import org.eclipse.tycho.core.osgitools.BundleReader;

/**
 * This class provides an implementation of an {@link EquinoxInstallation} which represents an RCP
 * application which has been provisioned using p2 director.
 * 
 * @author mistria
 */
public class ProvisionedEquinoxInstallation implements EquinoxInstallation {

    private File location;
    private File launcherJar;
    private EquinoxInstallationDescription description;

    public ProvisionedEquinoxInstallation(File location, BundleReader bundleReader) {
        this.location = location;
        description = new ProvisionedInstallationDescription(location, bundleReader);
    }

    public File getLauncherJar() {
        if (launcherJar != null) {
            return launcherJar;
        }
        launcherJar = EquinoxInstallationLaunchConfiguration.findLauncherJar(location);
        return launcherJar;
    }

    public File getLocation() {
        return location;
    }

    public File getConfigurationLocation() {
        // TODO should this be configurable?
        return new File(getLocation(), "configuration");
    }

    public EquinoxInstallationDescription getInstallationDescription() {
        return description;
    }

}
