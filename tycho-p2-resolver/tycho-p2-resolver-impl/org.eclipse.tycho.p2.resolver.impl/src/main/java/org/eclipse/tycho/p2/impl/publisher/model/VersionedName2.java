/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.model;

import org.eclipse.equinox.p2.metadata.VersionedId;

public class VersionedName2 extends VersionedId {
    private final String os;

    private final String ws;

    private final String arch;

    public VersionedName2(String id, String version, String os, String ws, String arch) {
        super(id, version);
        this.os = os;
        this.ws = ws;
        this.arch = arch;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof VersionedName2)) {
            return false;
        }

        VersionedName2 other = (VersionedName2) obj;

        return super.equals(obj) && eq(os, other.os) && eq(ws, other.ws) && eq(arch, other.arch);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * 31 + (os != null ? os.hashCode() : 0);
        hash = hash * 31 + (ws != null ? ws.hashCode() : 0);
        hash = hash * 31 + (arch != null ? arch.hashCode() : 0);
        return hash;
    }

    private static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

    public String getOs() {
        return os;
    }

    public String getWs() {
        return ws;
    };

    public String getArch() {
        return arch;
    }
}
