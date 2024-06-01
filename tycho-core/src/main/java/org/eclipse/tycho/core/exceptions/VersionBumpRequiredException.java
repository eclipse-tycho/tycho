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

import java.util.Optional;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.osgi.framework.Version;

public class VersionBumpRequiredException extends MojoFailureException {

    private IInstallableUnit unit;
    private Version reactorVersion;
    private Version baselineVersion;
    private MavenProject mavenProject;
    private Version suggestedVersion;

    public VersionBumpRequiredException(String message, IInstallableUnit unit, Version reactorVersion,
            Version baselineVersion) {
        super(message);
        this.unit = unit;
        this.reactorVersion = reactorVersion;
        this.baselineVersion = baselineVersion;
    }

    public VersionBumpRequiredException(String message, MavenProject mavenProject, Version suggestedVersion) {
        super(message);
        this.mavenProject = mavenProject;
        this.suggestedVersion = suggestedVersion;
    }

    public Optional<Version> getSuggestedVersion() {
        return Optional.ofNullable(suggestedVersion);
    }

    @Override
    public String toString() {
        if (unit != null) {
            return "Unit +" + unit + " has version " + reactorVersion + " and baseline version is " + baselineVersion;
        }
        if (mavenProject != null) {
            return mavenProject.getId() + " suggested version " + suggestedVersion;
        }
        return getMessage();
    }

}
