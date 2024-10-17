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
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 Support setting the version for pomless builds
 *    Bachmann electronic GmbH. - #512326 Support product file names other than artifact id
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.eclipse.sisu.Typed;
import org.eclipse.tycho.versions.pom.PomFile;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Updates pom version to match Eclipse/OSGi metadata.
 */
@Named
@Typed(PomVersionUpdater.class)
public class PomVersionUpdater extends VersionUpdater {

    @Inject
    public PomVersionUpdater(VersionsEngine engine) {
        super(engine);
    }

    @Override
    protected void addVersionChange(VersionsEngine engine, PomFile pom, String osgiVersion) {
        engine.addVersionChange(new PomVersionChange(pom, osgiVersion));
    }

}
