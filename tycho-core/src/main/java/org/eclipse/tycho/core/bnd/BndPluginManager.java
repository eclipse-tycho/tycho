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

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Manager that collects BndPlugins in the plexus domain and installs them into a workspace
 */
@Singleton
@Named
public class BndPluginManager {
    private final Map<String, RepositoryPlugin> repositoryPlugins;

    @Inject
    public BndPluginManager(Map<String, RepositoryPlugin> repositoryPlugins) {
        this.repositoryPlugins = repositoryPlugins;
    }

    public void setupWorkspace(Workspace ws) {
        repositoryPlugins.values().forEach(ws::addBasicPlugin);
        ws.refresh();
    }
}
