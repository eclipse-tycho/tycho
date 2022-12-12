/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

import java.util.Objects;

public class SystemCapability {

    public enum Type {
        JAVA_PACKAGE, OSGI_EE
    }

    private final Type type;
    private final String name;
    private final String version;

    public SystemCapability(Type type, String name, String version) {
        this.type = type;
        this.name = name;
        this.version = version;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        return obj instanceof SystemCapability other && //
                Objects.equals(type, other.type) && //
                Objects.equals(name, other.name) && //
                Objects.equals(version, other.version);
    }

}
