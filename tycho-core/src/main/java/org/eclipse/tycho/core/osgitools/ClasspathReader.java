/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.model.classpath.JUnitBundle;
import org.eclipse.tycho.model.classpath.ProjectClasspathEntry;

public interface ClasspathReader {

    Collection<ProjectClasspathEntry> parse(File basedir) throws IOException;

    static Collection<MavenArtifactKey> asMaven(Collection<JUnitBundle> artifacts) {
        return artifacts.stream().map(junit -> toMaven(junit)).toList();
    }

    static MavenArtifactKey toMaven(JUnitBundle junit) {
        return MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT, junit.getBundleName(), junit.getVersionRange(),
                junit.getMavenGroupId(), junit.getMavenArtifactId());
    }

}
