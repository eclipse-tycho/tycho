/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

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

}
