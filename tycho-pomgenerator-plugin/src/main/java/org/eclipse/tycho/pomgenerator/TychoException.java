/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomgenerator;

public class TychoException extends Exception {

    private static final long serialVersionUID = -3721311062142369314L;

    public TychoException() {
        super();
    }

    public TychoException(String message, Throwable cause) {
        super(message, cause);
    }

    public TychoException(String message) {
        super(message);
    }

    public TychoException(Throwable cause) {
        super(cause);
    }

}
