/*******************************************************************************
 * Copyright (c) 2011 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

public class ProfileName {
    /** default-value="DefaultProfile" */
    private String name;

    public String getName() {
        return name;
    }

    public String getArch() {
        return arch;
    }

    public String getOs() {
        return os;
    }

    public String getWs() {
        return ws;
    }

    private String arch;
    private String os;
    private String ws;

    public ProfileName() {
    }

    public ProfileName(String name) {
        this(name, null, null, null);
    }

    public ProfileName(String name, String arch, String os, String ws) {
        this.name = name;
        this.arch = arch;
        this.os = os;
        this.ws = ws;
    }

}
