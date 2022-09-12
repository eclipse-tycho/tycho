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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.locking.facade.FileLockService;

@Component(role = EquinoxLifecycleListener.class, hint = "FileLockServiceConfigurator")
public class FileLockServiceConfigurator implements EquinoxLifecycleListener {

    @Requirement
    private FileLockService fileLockService;

    /**
     * Registers the {@link FileLockService} plexus component as an OSGi service so it can be used
     * from OSGi too.
     */
    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        framework.registerService(FileLockService.class, fileLockService);
    }

}
