/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director.runtime;

// camel-case so that it can be used as mojo parameter
public enum DirectorVersion {
    luna("0.21.0"), // last version that supports the legacy Mac installation layout 
    current(null);

    public final String availableInTychoVersion;

    private DirectorVersion(String availableInTychoVersion) {
        this.availableInTychoVersion = availableInTychoVersion;
    }

}
