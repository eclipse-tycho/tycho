/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
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
import java.net.MalformedURLException;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;

public class FileLockerImpl implements FileLocker {

    private static final String LOCKFILE_SUFFIX = ".tycholock";

    private final Location lockFileLocation;
    final File lockMarkerFile;

    public FileLockerImpl(File file, Location anyLocation) {
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
            if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
                throw new RuntimeException("Could not create parent directory " + parentDir + " of lock marker file");
            }
            this.lockFileLocation = anyLocation.createLocation(null, null, false);
            this.lockFileLocation.set(lockMarkerFile.toURL(), false, lockMarkerFile.getAbsolutePath());
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
        boolean success = false;
        final long waitInterval = 50L;
        long maxTries = (timeout / waitInterval) + 1;
        IOException ioException = null;
        for (long i = 0; i < maxTries; i++) {
            ioException = null;
            try {
                success = lockFileLocation.lock();
            } catch (IOException ioe) {
                // keep trying (and re-throw eventually)
                ioException = ioe;
            }
            if (success) {
                return;
            }
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        String message = "lock timeout: Could not acquire lock on file " + lockFileLocation.getURL() + " for " + timeout
                + " msec";
        if (ioException != null) {
            throw new LockTimeoutException(message, ioException);
        } else {
            throw new LockTimeoutException(message);
        }
    }

    @Override
    public void release() {
        lockFileLocation.release();
        if (lockMarkerFile.isFile() && !lockMarkerFile.delete()) {
            // this can happen if another process already holds the lock again
            lockMarkerFile.deleteOnExit();
        }
    }

    @Override
    public boolean isLocked() {
        try {
            return lockFileLocation.isLocked();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
