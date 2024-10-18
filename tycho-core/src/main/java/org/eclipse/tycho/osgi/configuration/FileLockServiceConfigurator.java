/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.osgi.configuration;

import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.FileLockService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("FileLockServiceConfigurator")
public class FileLockServiceConfigurator implements EquinoxLifecycleListener {

    private final FileLockService fileLockService;

    @Inject
    public FileLockServiceConfigurator(FileLockService fileLockService) {
        this.fileLockService = fileLockService;
    }

    /**
     * Registers the {@link FileLockService} plexus component as an OSGi service so it can be used
     * from OSGi too.
     */
    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(FileLockService.class, fileLockService);
    }

}
