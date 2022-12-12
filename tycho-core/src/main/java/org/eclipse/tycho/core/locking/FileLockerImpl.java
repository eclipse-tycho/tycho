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

package org.eclipse.tycho.core.locking;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;

public class FileLockerImpl implements FileLocker {

    private static final String LOCKFILE_SUFFIX = ".tycholock";

    final File lockMarkerFile;

    private FileLock lock;

    private File file;

    public FileLockerImpl(File file) {
        this.file = file;
        try {
            if (file.isDirectory()) {
                this.lockMarkerFile = new File(file, LOCKFILE_SUFFIX).getCanonicalFile();
            } else {
                this.lockMarkerFile = new File(file.getParentFile(), file.getName() + LOCKFILE_SUFFIX)
                        .getCanonicalFile();
            }
            if (lockMarkerFile.isDirectory()) {
                throw new RuntimeException("Lock marker file " + lockMarkerFile + " already exists and is a directory");
            }
            File parentDir = lockMarkerFile.getParentFile();
            if (!parentDir.mkdirs() && !parentDir.isDirectory()) {
                throw new RuntimeException("Could not create parent directory " + parentDir + " of lock marker file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lock() {
        lock(10000L);
    }

    @Override
    public void lock(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        if (lock != null) {
            throw new LockTimeoutException("already locked file " + file.getAbsolutePath());
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
                    Path path = lockMarkerFile.toPath();
                    channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
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
        throw new LockTimeoutException("lock timeout: Could not acquire lock on file "
                + lockMarkerFile.getAbsolutePath() + " for " + timeout + " msec");
    }

    @Override
    public synchronized void release() {
        if (lock != null) {
            try {
                lock.acquiredBy().close();
            } catch (Exception e) {
            }
            lock = null;
            if (!lockMarkerFile.delete()) {
                lockMarkerFile.deleteOnExit();
            }
        }
    }

    public synchronized boolean isLocked() {
        return lock != null;
    }

}
