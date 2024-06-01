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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.tycho.LockTimeoutException;

public class FileLockerImpl {

    private static final String LOCKFILE_SUFFIX = ".tycholock";

    final Path lockMarkerFile;

    private FileLock lock;

    private Path file;

    FileLockerImpl(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.lockMarkerFile = Files.isDirectory(this.file) //
                ? this.file.resolve(LOCKFILE_SUFFIX)
                : this.file.getParent().resolve(this.file.getFileName() + LOCKFILE_SUFFIX);
        try {
            if (Files.isDirectory(lockMarkerFile)) {
                throw new IllegalStateException(
                        "Lock marker file " + lockMarkerFile + " already exists and is a directory");
            }
            Files.createDirectories(lockMarkerFile.getParent());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    void lock(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        if (lock != null) {
            throw new LockTimeoutException("already locked file " + file);
        }
        lock = aquireLock(timeout);

    }

    private FileLock aquireLock(long timeout) {
        final long waitInterval = 50L;
        long maxTries = (timeout / waitInterval) + 1;
        FileChannel channel = null;
        for (long i = 0; i < maxTries; i++) {
            try {
                if (channel == null) {
                    channel = FileChannel.open(lockMarkerFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                }
                FileLock fileLock = channel.tryLock();
                if (fileLock != null) {
                    return fileLock;
                }
            } catch (IOException ioe) {
            }
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockTimeoutException("Interrupted", e);
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e1) {
            } finally {
                channel = null;
            }
        }
        throw new LockTimeoutException(
                "lock timeout: Could not acquire lock on file " + lockMarkerFile + " for " + timeout + " msec");
    }

    synchronized void release() {
        if (lock != null) {
            try {
                lock.acquiredBy().close();
            } catch (Exception e) {
            }
            lock = null;
            File lockFile = lockMarkerFile.toFile();
            if (!lockFile.delete()) {
                lockFile.deleteOnExit();
            }
        }
    }

}
