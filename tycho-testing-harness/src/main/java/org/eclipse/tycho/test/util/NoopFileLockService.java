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

package org.eclipse.tycho.test.util;

import java.io.File;

import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;

public class NoopFileLockService implements FileLockService {

    public FileLocker getFileLocker(File file) {
        return new FileLocker() {

            public void release() {
            }

            public void lock(long timeout) throws LockTimeoutException {
            }

            public void lock() throws LockTimeoutException {
            }

            public boolean isLocked() {
                return false;
            }
        };
    }

}
