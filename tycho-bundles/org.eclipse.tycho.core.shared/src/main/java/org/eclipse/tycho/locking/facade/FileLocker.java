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

/**
 * Provides process-level file locking. Locking is advisory, meaning that processes must
 * cooperatively use the same locking mechanism.
 */
public interface FileLocker {

    /**
     * Equivalent to {{@link #lock(long)} with a timeout argument of 10000 milliseconds.
     */
    public void lock() throws LockTimeoutException;

    /**
     * Attempt to lock the file associated with this locker object. Note that technically, not the
     * file itself is locked, but an empty marker file next to it.
     * 
     * @param timeout
     *            timeout in milliseconds
     * 
     * @throws LockTimeoutException
     *             if lock cannot be obtained for more than the specified timeout in millseconds.
     */
    public void lock(long timeout) throws LockTimeoutException;

    /**
     * Release the lock if acquired. Also removes the lock marker file.
     */
    public void release();

    /**
     * Whether the file associated with this locker object is currently locked (by this process or
     * any other process).
     */
    public boolean isLocked();

}
