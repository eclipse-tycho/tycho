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

package org.eclipse.tycho.core.locking;

import java.io.File;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.location.BasicLocation;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = FileLockService.class)
public class FileLockServiceImpl implements FileLockService {

    private Location anyLocation;

    public FileLockServiceImpl() {
        anyLocation = new BasicLocation(null, null, false, null, new EquinoxContainer(null).getConfiguration());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.tycho.p2.repository.FileLockService#getFileLocker(java.io.File)
     */
    public synchronized FileLocker getFileLocker(File file) {
        return new FileLockerImpl(file, anyLocation);
    }

}
