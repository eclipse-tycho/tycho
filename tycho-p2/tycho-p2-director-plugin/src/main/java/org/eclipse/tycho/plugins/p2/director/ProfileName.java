/*******************************************************************************
 * Copyright (c) 2011 Wind River and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.util.List;

import org.eclipse.tycho.core.shared.TargetEnvironment;

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

    private String os;
    private String ws;
    private String arch;

    public ProfileName() {
    }

    public ProfileName(String name) {
        this(name, null, null, null);
    }

    public ProfileName(String name, String os, String ws, String arch) {
        this.name = name;
        this.ws = ws;
        this.os = os;
        this.arch = arch;
    }

    public static String getNameForEnvironment(TargetEnvironment env, List<ProfileName> nameMap, String defaultName) {
        if (nameMap != null) {
            for (ProfileName profileWithEnvironment : nameMap) {
                // first match always wins
                if (env.match(profileWithEnvironment.getOs(), profileWithEnvironment.getWs(),
                        profileWithEnvironment.getArch())) {
                    return profileWithEnvironment.getName();
                }
            }
        }
        return defaultName;
    }
}
