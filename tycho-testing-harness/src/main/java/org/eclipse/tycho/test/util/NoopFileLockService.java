/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
