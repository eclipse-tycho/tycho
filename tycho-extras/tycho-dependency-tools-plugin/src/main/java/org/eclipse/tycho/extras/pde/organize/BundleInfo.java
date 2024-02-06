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

import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleInfo {

    private Version version;
    private Manifest manifest;

    public BundleInfo(Manifest manifest) {
        this.manifest = manifest;
    }

    public Manifest manifest() {
        return manifest;
    }

    public Version version() {
        if (version == null) {
            String v = manifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            if (v == null) {
                v = "0";
            }
            version = Version.parseVersion(v);
        }
        return version;
    }
}
