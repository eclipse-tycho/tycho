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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.locking.facade.FileLockService;

@Component(role = FileLockService.class)
public class FileLockServiceImpl implements FileLockService {

    private final Map<String, FileLockerImpl> lockers = new ConcurrentHashMap<>();

    @Override
    public FileLockerImpl getFileLocker(File file) {
        String key;
        try {
            key = file.getCanonicalPath();
        } catch (IOException e) {
            key = file.getAbsolutePath();
        }
        return lockers.computeIfAbsent(key, k -> new FileLockerImpl(file));
    }

}
