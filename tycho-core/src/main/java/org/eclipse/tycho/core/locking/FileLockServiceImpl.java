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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.FileLockService;
import org.eclipse.tycho.LockTimeoutException;

@Named
@Singleton
public class FileLockServiceImpl implements FileLockService {
    record FileLocks(FileLockerImpl fileLocker, Lock vmLock) {
    }

    private final Map<Path, FileLocks> lockers = new ConcurrentHashMap<>();

    @Override
    public Closeable lock(File file, long timeout) {
        FileLocks locks = getFileLocker(file.toPath());
        FileLockerImpl locker = locks.fileLocker();
        try {
            if (!locks.vmLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                throw new LockTimeoutException("lock timeout: Could not acquire lock on file " + locker.lockMarkerFile
                        + " for " + timeout + " msec");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("Interrupted", e);
        }
        locker.lock(timeout);
        return () -> {
            locks.fileLocker().release();
            locks.vmLock().unlock();
        };
    }

    @Override
    public Closeable lockVirtually(File file) {
        FileLocks locks = getFileLocker(file.toPath());
        locks.vmLock().lock();
        return locks.vmLock()::unlock;
    }

    FileLocks getFileLocker(Path file) {
        Path key;
        try {
            key = file.toRealPath();
        } catch (IOException e) {
            key = file.toAbsolutePath().normalize();
        }
        return lockers.computeIfAbsent(key, f -> new FileLocks(new FileLockerImpl(f), new ReentrantLock()));
    }

}
