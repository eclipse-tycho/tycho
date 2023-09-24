/*******************************************************************************
 * Copyright (c) 2011, 2023 SAP AG and others.
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

package org.eclipse.tycho.core.locking;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.FileLockService;

@Component(role = FileLockService.class)
public class FileLockServiceImpl implements FileLockService {

    private final Map<Path, FileLockerImpl> lockers = new ConcurrentHashMap<>();

    @Override
    public Closeable lock(File file, long timeout) {
        FileLockerImpl locker = getFileLocker(file.toPath());
        locker.lock(timeout);
        return locker::release;
    }

    FileLockerImpl getFileLocker(Path file) {
        Path key;
        try {
            key = file.toRealPath();
        } catch (IOException e) {
            key = file.toAbsolutePath().normalize();
        }
        return lockers.computeIfAbsent(key, FileLockerImpl::new);
    }

}
