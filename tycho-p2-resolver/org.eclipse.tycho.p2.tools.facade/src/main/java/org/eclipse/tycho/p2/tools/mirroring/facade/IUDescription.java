/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring.facade;

public class IUDescription {

    private String id;
    private String version;

    /**
     * @param id
     *            id of the installable unit. Must not be <code>null</code>.
     * @param version
     *            version of the installable unit. <code>null</code> means latest available version.
     */
    public IUDescription(String id, String version) {
        if (id == null) {
            throw new NullPointerException("id must not be null");
        }
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "[" + id + ", " + (version != null ? version : "0.0.0") + "]";
    }

}
