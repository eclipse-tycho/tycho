/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - Bug 453708 Support for site/repository-reference/@location in eclipse-repository
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

public class RepositoryReference {
    private final String name;
    private final String location;
    private final boolean enable;

    public RepositoryReference(String name, String location, boolean enable) {
        super();
        this.name = name;
        this.location = location;
        this.enable = enable;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isEnable() {
        return enable;
    }

}
