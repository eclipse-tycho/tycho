/*******************************************************************************
 * Copyright (c) 2011-2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.locking.facade;

public class LockTimeoutException extends RuntimeException {

    private static final long serialVersionUID = -5817180143050849842L;

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
