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

package org.eclipse.tycho.p2.impl.locking;

import java.io.File;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

public class FileLockServiceImpl implements FileLockService {

    private Location anyLocation;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.FileLockService#getFileLocker(java.io.File)
     */
    public synchronized FileLocker getFileLocker(File file) {
        return new FileLockerImpl(file, anyLocation);
    }

    public void setLocation(Location location) {
        this.anyLocation = location;
    }

}
