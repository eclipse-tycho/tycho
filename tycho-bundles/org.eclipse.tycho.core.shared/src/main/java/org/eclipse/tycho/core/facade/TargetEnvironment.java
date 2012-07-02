/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.facade;

import java.util.HashMap;

public class TargetEnvironment {
    private String os;

    private String ws;

    private String arch;

    // no-args constructor for Mojo configuration
    public TargetEnvironment() {
    }

    public TargetEnvironment(String os, String ws, String arch) {
        this.os = os;
        this.ws = ws;
        this.arch = arch;
    }

    /**
     * Returns the operating system of the represented target environment.
     */
    public String getOs() {
        return os;
    }

    /**
     * Returns the windowing system of the represented target environment.
     */
    public String getWs() {
        return ws;
    }

    /**
     * Returns the architecture of the represented target environment.
     */
    public String getArch() {
        return arch;
    }

    public boolean match(String os, String ws, String arch) {
        return (os == null || os.equals(this.os)) && //
                (ws == null || ws.equals(this.ws)) && //
                (arch == null || arch.equals(this.arch));
    }

    /**
     * Returns the target environment as string of the form <code>ws.os.arch</code>. This format is
     * used by the p2 publishers and in that context called "configuration" or "config spec".
     */
    public String toConfigSpec() {
        return ws + '.' + os + '.' + arch;
    }

    /**
     * Returns the target environment as map. The keys are "osgi.ws", "osgi.os", and "osgi.arch".
     * This format is used by the p2 slicer to filter installable units by environments.
     * 
     * @return a new instance of {@link HashMap} with the target environment set
     */
    public HashMap<String, String> toFilter() {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put("osgi.os", os);
        result.put("osgi.ws", ws);
        result.put("osgi.arch", arch);
        return result;
    }

    @Override
    public String toString() {
        return os + '/' + ws + '/' + arch;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 17 * hash + (os != null ? os.hashCode() : 0);
        hash = 17 * hash + (ws != null ? ws.hashCode() : 0);
        hash = 17 * hash + (arch != null ? arch.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof TargetEnvironment)) {
            return false;
        }
        TargetEnvironment other = (TargetEnvironment) obj;

        return eq(os, other.os) && eq(ws, other.ws) && eq(arch, other.arch);
    }

    private static boolean eq(String a, String b) {
        return a != null ? a.equals(b) : b == null;
    }

}
