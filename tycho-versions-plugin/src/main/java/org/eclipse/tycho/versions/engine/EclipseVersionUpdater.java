/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - Support for release-process like Maven
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.versions.pom.PomFile;

/**
 * Updates Eclipse/OSGi metadata to match pom version.
 */
@Component(role = EclipseVersionUpdater.class, instantiationStrategy = "per-lookup")
public class EclipseVersionUpdater extends VersionUpdater {

    @Override
    protected void addVersionChange(VersionsEngine engine, PomFile pom, String osgiVersion) {
        engine.addVersionChange(new PomVersionChange(pom, osgiVersion, pom.getVersion()));
    }

}
