/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
package org.eclipse.tycho.p2.publisher.rootfiles;

import java.util.Objects;

public final class ConfigSpec {

    public static final ConfigSpec GLOBAL = new ConfigSpec(null, null, null);

    private final String ws;

    private final String os;

    private final String arch;

    public static ConfigSpec createFromWsOsArch(String wsOsArchDotSeparated) {
        if (wsOsArchDotSeparated.isEmpty())
            return GLOBAL;
        else
            return new ConfigSpec(wsOsArchDotSeparated.split("\\."));
    }

    public static ConfigSpec createFromOsWsArchArray(String[] segments, int beginIndex) {
        return new ConfigSpec(segments[beginIndex + 1], segments[beginIndex], segments[beginIndex + 2]);
    }

    private ConfigSpec(String[] wsOsArch) {
        this.ws = wsOsArch[0];
        this.os = wsOsArch[1];
        this.arch = wsOsArch[2];
    }

    private ConfigSpec(String ws, String os, String arch) {
        this.ws = ws;
        this.os = os;
        this.arch = arch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ws, os, arch);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof ConfigSpec other && //
                        Objects.equals(ws, other.ws) && //
                        Objects.equals(os, other.os) && //
                        Objects.equals(arch, other.arch));
    }

    public String toOsString() {
        return os + '.' + ws + '.' + arch;
    }

    public String toStringForAdvice() {
        if (this.equals(GLOBAL))
            return "";
        return ws + '.' + os + '.' + arch;
    }

}
