/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.util;

import java.io.File;

import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;

public class NoopFileLockService implements FileLockService {

    @Override
    public FileLocker getFileLocker(File file) {
        return new FileLocker() {

            @Override
            public void release() {
            }

            @Override
            public void lock() {
            }

            @Override
            public boolean isLocked() {
                return false;
            }

            @Override
            public void lock(long timeout) throws LockTimeoutException {
            }
        };
    }

}
