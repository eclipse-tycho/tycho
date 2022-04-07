/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

/**
 * Indicates that an OSGi execution environment is unknown if thrown.
 */
@SuppressWarnings("serial")
public class UnknownEnvironmentException extends RuntimeException {

    private final String environmentName;

    public UnknownEnvironmentException(String environmentName) {
        super("Unknown OSGi execution environment: '" + environmentName + "'");
        this.environmentName = environmentName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }
}
