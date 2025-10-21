/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.osgi.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;

@Named("LocalRepositoryP2Indices")
@Singleton
public class LocalRepositoryP2IndicesConfigurator implements EquinoxLifecycleListener {

    @Inject
    LocalRepositoryP2Indices indices;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(LocalRepositoryP2Indices.class, indices);
    }

}
