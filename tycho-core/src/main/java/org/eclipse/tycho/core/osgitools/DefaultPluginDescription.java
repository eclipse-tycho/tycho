/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.model.PluginRef;

public class DefaultPluginDescription extends DefaultArtifactDescriptor implements PluginDescription {

    private PluginRef pluginRef;

    public DefaultPluginDescription(ArtifactKey key, File location, ReactorProject project, String classifier,
            PluginRef pluginRef, Set<IInstallableUnit> installableUnits) {
        super(key, location, project, classifier, installableUnits);
        this.pluginRef = pluginRef;
    }

    @Override
    public PluginRef getPluginRef() {
        return pluginRef;
    }

}
