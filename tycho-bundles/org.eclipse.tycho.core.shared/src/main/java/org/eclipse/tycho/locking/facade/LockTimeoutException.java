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

public class LockTimeoutException extends RuntimeException {

    public LockTimeoutException(String message) {
        super(message);
    }

    public LockTimeoutException(Throwable cause) {
        super(cause);
    }

    public LockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
