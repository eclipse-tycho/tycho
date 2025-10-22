/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.MavenRepositorySettings;

@Named("RepositorySettingsConfigurator")
@Singleton
public class RepositorySettingsConfigurator implements EquinoxLifecycleListener {

    @Inject
    private MavenRepositorySettings settings;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(MavenRepositorySettings.class, settings);
    }

}
