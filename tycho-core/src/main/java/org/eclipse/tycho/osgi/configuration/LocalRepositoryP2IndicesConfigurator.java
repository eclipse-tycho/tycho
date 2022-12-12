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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;

@Component(role = EquinoxLifecycleListener.class, hint = "LocalRepositoryP2Indices")
public class LocalRepositoryP2IndicesConfigurator implements EquinoxLifecycleListener {

    @Requirement
    LocalRepositoryP2Indices indices;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(LocalRepositoryP2Indices.class, indices);
    }

}
