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

package org.eclipse.tycho;

import java.io.Closeable;
import java.io.File;

/**
 * Provides process-level file locking.
 */
public interface FileLockService {

    /**
     * Locks the given file to protect read/write access from multiple processes on it. Locking is
     * advisory only, i.e. all processes must use the same locking mechanism.
     * <p>
     * This is equivalent to {@link #lock(File, long)} with a timeout argument of 10 seconds.
     * </p>
     */
    default Closeable lock(File file) {
        return lock(file, 10000L);
    }

    /**
     * Locks the given file to protect read/write access from multiple processes on it. Locking is
     * advisory only, i.e. all processes must use the same locking mechanism.
     */
    Closeable lock(File file, long timeout);

    /**
     * Locks the given file for this JVM to protect read/write access from multiple threads in this
     * JVM on it.
     */
    Closeable lockVirtually(File file);
}
