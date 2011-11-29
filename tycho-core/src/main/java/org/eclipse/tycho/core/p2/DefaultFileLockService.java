/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.p2;

import java.io.File;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = FileLockService.class)
public class DefaultFileLockService implements FileLockService {

    @Requirement
    EquinoxServiceFactory serviceFactory;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.FileLockService#getFileLocker(java.io.File)
     */
    public FileLocker getFileLocker(File file) {
        return serviceFactory.getService(FileLockService.class).getFileLocker(file);
    }

}
