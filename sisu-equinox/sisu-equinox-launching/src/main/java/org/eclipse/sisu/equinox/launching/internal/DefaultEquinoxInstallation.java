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
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.ArtifactDescriptor;
import org.osgi.framework.Version;

public class DefaultEquinoxInstallation implements EquinoxInstallation {
    private final File location;

    private final File configurationLocation;

    private final EquinoxInstallationDescription description;

    public DefaultEquinoxInstallation(EquinoxInstallationDescription installationDescription, File location,
            File configurationLocation) {
        this.description = installationDescription;
        this.location = location;
        this.configurationLocation = configurationLocation;
    }

    public File getLocation() {
        return location;
    }

    public File getConfigurationLocation() {
        return configurationLocation;
    }

    public File getLauncherJar() {
        ArtifactDescriptor systemBundle = description.getSystemBundle();
        Version osgiVersion = Version.parseVersion(systemBundle.getKey().getVersion());
        if (osgiVersion.compareTo(EquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
            throw new IllegalArgumentException("Eclipse 3.2 and earlier are not supported.");
            // return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
        } else {
            // assume eclipse 3.3 or 3.4
            ArtifactDescriptor launcher = description.getBundle(EquinoxInstallationDescription.EQUINOX_LAUNCHER, null);
            if (launcher == null) {
                throw new IllegalArgumentException("Could not find " + EquinoxInstallationDescription.EQUINOX_LAUNCHER
                        + " bundle in the test runtime.");
            }
            try {
                return launcher.getLocation().getCanonicalFile();
            } catch (IOException e) {
                return launcher.getLocation().getAbsoluteFile();
            }
        }
    }

    public EquinoxInstallationDescription getInstallationDescription() {
        return description;
    }

}
