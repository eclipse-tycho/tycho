/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.exceptions;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.osgi.framework.Version;

public class VersionBumpRequiredException extends MojoFailureException {

    private IInstallableUnit unit;
    private Version reactorVersion;
    private Version baselineVersion;

    public VersionBumpRequiredException(String message, IInstallableUnit unit, Version reactorVersion,
            Version baselineVersion) {
        super(message);
        this.unit = unit;
        this.reactorVersion = reactorVersion;
        this.baselineVersion = baselineVersion;
    }

    @Override
    public String toString() {
        return "Unit +" + unit + " has version " + reactorVersion + " and baseline version is " + baselineVersion;
    }

}
