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

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.equinox.launching.EquinoxInstallation;
import org.eclipse.tycho.equinox.launching.EquinoxInstallationDescription;
import org.osgi.framework.Version;

public class DefaultEquinoxInstallation implements EquinoxInstallation {
    private final File location;

    private final EquinoxInstallationDescription Description;

    public DefaultEquinoxInstallation(EquinoxInstallationDescription installationDescription, File location) {
        this.Description = installationDescription;
        this.location = location;
    }

    public File getLocation() {
        return location;
    }

    public File getLauncherJar() {
        ArtifactDescriptor systemBundle = Description.getSystemBundle();
        Version osgiVersion = Version.parseVersion(systemBundle.getKey().getVersion());
        if (osgiVersion.compareTo(EquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
            throw new IllegalArgumentException("Eclipse 3.2 and earlier are not supported.");
            // return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
        } else {
            // assume eclipse 3.3 or 3.4
            ArtifactDescriptor launcher = Description.getBundle(EquinoxInstallationDescription.EQUINOX_LAUNCHER, null);
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
        return Description;
    }

}
