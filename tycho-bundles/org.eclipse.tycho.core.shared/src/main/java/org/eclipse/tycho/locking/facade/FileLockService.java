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
