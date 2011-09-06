/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

public final class ConfigSpec {

    public static final ConfigSpec GLOBAL = new ConfigSpec(null, null, null);

    private final String ws;

    private final String os;

    private final String arch;

    public static ConfigSpec createFromWsOsArch(String wsOsArchDotSeparated) {
        if (wsOsArchDotSeparated.equals(""))
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
        final int prime = 29;
        int result = 1;
        result = prime * result + ((arch == null) ? 0 : arch.hashCode());
        result = prime * result + ((os == null) ? 0 : os.hashCode());
        result = prime * result + ((ws == null) ? 0 : ws.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof ConfigSpec) {
            ConfigSpec other = (ConfigSpec) obj;
            return equals(ws, other.ws) && equals(os, other.os) && equals(arch, other.arch);
        }
        return false;
    }

    private <T> boolean equals(T left, T right) {
        if (left == right)
            return true;
        if (left == null)
            return false;
        return left.equals(right);
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
