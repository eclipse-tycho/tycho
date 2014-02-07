/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.shared;

import org.eclipse.tycho.ArtifactKey;

/**
 * Handle on a unit which defines dependencies of a project. There may be more than one dependency
 * seed per project. The seeds are used for product and p2 repository assembly.
 */
public class DependencySeed {

    // not using ArtifactKey here, because we don't have the same invariants
    private final String type;
    private final String id;
    private final String version;
    private final Object installableUnit;

    /**
     * @param type
     *            The type of the seed unit. See {@link ArtifactKey} for known types. May be
     *            <code>null</code>.
     * @param id
     *            Identifier of the seed unit.
     * @param version
     *            Exact version (i.e. qualified) version of the unit.
     * @param installableUnit
     *            The seed unit as IInstallableUnit. Contains the dependency information.
     */
    public DependencySeed(String type, String id, String version, Object installableUnit) {
        this.type = type;
        this.id = id;
        this.version = version;
        this.installableUnit = installableUnit;
    }

    /**
     * @return the type of the seed unit. See {@link ArtifactKey} for known types. May be
     *         <code>null</code>.
     */
    public String getType() {
        return type;
    }

    /**
     * @return the identifier of the seed unit.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the seed unit as IInstallableUnit.
     */
    public/* IInstallableUnit */Object getInstallableUnit() {
        return installableUnit;
    }

}
