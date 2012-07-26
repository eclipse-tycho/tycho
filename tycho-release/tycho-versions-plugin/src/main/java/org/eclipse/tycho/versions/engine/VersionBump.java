/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.osgi.framework.Version;

/**
 * 
 * @author mistria
 */
public class VersionBump {

    int majorDiff;
    int minorDiff;
    int microDiff;

    public VersionBump(String versionDiff) throws NumberFormatException, IllegalArgumentException {
        String[] segments = versionDiff.split("\\.");
        if (segments.length != 3) {
            throw new IllegalArgumentException("A versionDiff must be made of 3 numeric segements");
        }
        this.majorDiff = Integer.parseInt(segments[0]);
        this.minorDiff = Integer.parseInt(segments[1]);
        this.microDiff = Integer.parseInt(segments[2]);
    }

    public boolean isNoop() {
        return this.majorDiff == 0 && this.minorDiff == 0 && this.microDiff == 0;
    }

    public String applyTo(String version) {
        Version osgiVersion = new Version(Versions.toCanonicalVersion(version));
        int newMajor = osgiVersion.getMajor() + this.majorDiff;
        int newMinor = osgiVersion.getMinor() + this.minorDiff;
        int newMicro = osgiVersion.getMicro() + this.microDiff;
        StringBuilder res = new StringBuilder();
        res.append(newMajor);
        res.append('.');
        res.append(newMinor);
        res.append('.');
        res.append(newMicro);
        if (version.endsWith(Versions.SUFFIX_SNAPSHOT)) {
            res.append(Versions.SUFFIX_SNAPSHOT);
        } else if (version.endsWith(Versions.SUFFIX_QUALIFIER)) {
            res.append(Versions.SUFFIX_QUALIFIER);
        }
        return res.toString();
    }
}
