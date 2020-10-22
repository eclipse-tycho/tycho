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

package org.eclipse.tycho.locking.facade;

import java.io.File;

/**
 * Provides process-level file locking.
 */
public interface FileLockService {

    /**
     * Get a locker object which can be used to protect read/write access from multiple processes on
     * the given file. Locking is advisory only, i.e. all processes must use the same locking
     * mechanism.
     */
    public FileLocker getFileLocker(File file);

}
