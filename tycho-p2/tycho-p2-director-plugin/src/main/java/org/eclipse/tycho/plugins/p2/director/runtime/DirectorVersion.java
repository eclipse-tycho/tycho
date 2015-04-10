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
    legacyMacLayout("0.21.0"), // contains p2 director from Eclipse Luna (4.4) 
    marsMacLayout(null);

    public final String availableInTychoVersion;

    private DirectorVersion(String availableInTychoVersion) {
        this.availableInTychoVersion = availableInTychoVersion;
    }

}
