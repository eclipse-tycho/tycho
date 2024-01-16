/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.bnd;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

/**
 * Manager that collects BndPlugins in the plexus domain and installs them into a workspace
 */
@Component(role = BndPluginManager.class)
public class BndPluginManager {
    @Requirement
    private Map<String, RepositoryPlugin> repositoryPlugins;

    public void setupWorkspace(Workspace ws) {
        repositoryPlugins.values().forEach(ws::addBasicPlugin);
        ws.refresh();
    }
}
