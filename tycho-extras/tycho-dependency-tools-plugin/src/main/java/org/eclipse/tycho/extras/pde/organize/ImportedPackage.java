/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.organize;

import org.eclipse.equinox.p2.metadata.VersionRange;
import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;

public class ImportedPackage {

    private ImportedPackages packages;
    private String packageName;
    private Attrs attrs;
    private VersionRange version;

    public ImportedPackage(ImportedPackages packages, String packageName, Attrs attrs) {
        this.packages = packages;
        this.packageName = packageName;
        this.attrs = attrs;
    }

    public String getPackageName() {
        return packageName;
    }

    public VersionRange getVersionRange() {
        if (version == null) {
            String string = attrs.get(Constants.VERSION_ATTRIBUTE);
            if (string == null) {
                version = VersionRange.emptyRange;
            } else {
                version = VersionRange.create(string);
            }
        }
        return version;
    }

    public boolean isJava() {
        return packageName.startsWith("java.");
    }

    @Override
    public String toString() {
        VersionRange version = getVersionRange();
        if (version.equals(VersionRange.emptyRange)) {
            return getPackageName();
        }
        return getPackageName() + " " + getVersionRange();
    }

}
