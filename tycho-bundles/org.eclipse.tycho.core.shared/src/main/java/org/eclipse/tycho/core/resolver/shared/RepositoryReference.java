/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour - Bug 453708 Support for site/repository-reference/@location in eclipse-repository
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.shared;

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
