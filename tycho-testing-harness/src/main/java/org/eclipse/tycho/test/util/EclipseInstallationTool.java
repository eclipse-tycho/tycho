/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.TargetEnvironment;

public class EclipseInstallationTool {

    private File installationRoot;

    private EclipseInstallationTool(File installationRoot) {
        this.installationRoot = installationRoot;
    }

    public static EclipseInstallationTool forInstallationInEclipseRepositoryTarget(File projectRootFolder,
            String productId, TargetEnvironment env, String pathInArchive) {
        File installationRoot = new File(projectRootFolder,
                "target/products/" + productId + "/" + env.getOs() + "/" + env.getWs() + "/" + env.getArch());
        if (pathInArchive != null) {
            installationRoot = new File(installationRoot, pathInArchive);
        }
        if (!installationRoot.isDirectory()) {
            throw new IllegalArgumentException("No installation of product " + productId + " for environment " + env
                    + " found at \"" + installationRoot + "\"");
        }

        return new EclipseInstallationTool(installationRoot);
    }

    public List<String> getInstalledFeatureIds() {
        List<String> result = new ArrayList<>();
        for (File file : new File(installationRoot, "features").listFiles()) {
            int separator = file.getName().lastIndexOf('_');
            if (separator > 0) {
                result.add(file.getName().substring(0, separator));
            }
        }
        return result;
    }

}
