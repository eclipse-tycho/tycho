/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts;

import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.osgi.framework.VersionRange;

public interface ArtifactVersionProvider {

    Stream<ArtifactVersion> getPackageVersions(IInstallableUnit unit, String packageName, VersionRange versionRange,
            MavenProject mavenProject);

}
