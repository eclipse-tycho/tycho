/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
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

import org.apache.maven.project.MavenProject;
import org.osgi.framework.Version;

public class PackageVersionBumpRequiredException extends VersionBumpRequiredException {

    private final String packageName;
    private final Version currentPackageVersion;

    public PackageVersionBumpRequiredException(String message, MavenProject mavenProject, String packageName,
            Version currentPackageVersion, Version suggestedVersion) {
        super(message, mavenProject, suggestedVersion);
        this.packageName = packageName;
        this.currentPackageVersion = currentPackageVersion;
    }

    public String getPackageName() {
        return packageName;
    }

    public Version getCurrentPackageVersion() {
        return currentPackageVersion;
    }

}
