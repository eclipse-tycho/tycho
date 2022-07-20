/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import java.io.File;
import java.io.IOException;

import org.eclipse.sisu.osgi.launching.BundleReference;
import org.eclipse.sisu.osgi.launching.FrameworkInstallation;
import org.eclipse.sisu.osgi.launching.FrameworkInstallationDescription;
import org.osgi.framework.Version;

public class DefaultEquinoxInstallation implements FrameworkInstallation {
    private final File location;

    private final File configurationLocation;

    private final FrameworkInstallationDescription description;

    public DefaultEquinoxInstallation(FrameworkInstallationDescription installationDescription, File location,
            File configurationLocation) {
        this.description = installationDescription;
        this.location = location;
        this.configurationLocation = configurationLocation;
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public File getConfigurationLocation() {
        return configurationLocation;
    }

    @Override
    public File getLauncherJar() {
        BundleReference systemBundle = description.getSystemBundle();
        Version osgiVersion = Version.parseVersion(systemBundle.getVersion());
        if (osgiVersion.compareTo(DefaultEquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
            throw new IllegalArgumentException("Eclipse 3.2 and earlier are not supported.");
            // return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
        } else {
            // assume eclipse 3.3 or 3.4
            BundleReference launcher = description.getBundle(DefaultEquinoxInstallationDescription.EQUINOX_LAUNCHER,
                    null);
            if (launcher == null) {
                throw new IllegalArgumentException("Could not find "
                        + DefaultEquinoxInstallationDescription.EQUINOX_LAUNCHER + " bundle in the test runtime.");
            }
            try {
                return launcher.getLocation().getCanonicalFile();
            } catch (IOException e) {
                return launcher.getLocation().getAbsoluteFile();
            }
        }
    }

    @Override
    public FrameworkInstallationDescription getInstallationDescription() {
        return description;
    }

}
