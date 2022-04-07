/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.locking;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration.ConfigValues;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.location.BasicLocation;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = FileLockService.class)
public class FileLockServiceImpl implements FileLockService {

    private Location anyLocation;

    public FileLockServiceImpl() {
        anyLocation = new BasicLocation(null, null, false, null,
                new ConfigValues(new HashMap<String, String>(), new HashMap<>()), new EquinoxContainer(null, null),
                new AtomicBoolean(false));
    }

    @Override
    public synchronized FileLocker getFileLocker(File file) {
        return new FileLockerImpl(file, anyLocation);
    }

}
