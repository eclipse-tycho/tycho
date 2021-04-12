/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
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
package org.eclipse.tycho.core.resolver.shared;

import org.eclipse.tycho.ArtifactType;

/**
 * Handle on a unit which defines dependencies of a project. There may be more than one dependency
 * seed per project. The seeds are used for product and p2 repository assembly.
 */
public class DependencySeed {

    // not using ArtifactKey here because we don't have the same invariants
    private final String type;
    private final String id;
    // TODO 372780 add this member, so that a dependency seed uniquely identifies a unit
//    private final String version;

    // TODO 372780 disallow null; the UI should be obtained as well when looking up the version
    private final Object installableUnit;

    private final Filter addOnFilter;

    /**
     * @param type
     *            The type of the seed unit. See {@link ArtifactType} for known types. May be
     *            <code>null</code>.
     * @param id
     *            Identifier of the seed unit.
     * @param version
     *            Exact version (i.e. qualified) version of the unit.
     * @param installableUnit
     *            The seed unit as IInstallableUnit, which contains the dependency information. May
     *            be <code>null</code>.
     */
    public DependencySeed(String type, String id, /* String version, */Object installableUnit) {
        this(type, id, installableUnit, null);
    }

    /**
     * @param type
     *            The type of the seed unit. See {@link ArtifactType} for known types. May be
     *            <code>null</code>.
     * @param id
     *            Identifier of the seed unit.
     * @param version
     *            Exact version (i.e. qualified) version of the unit.
     * @param installableUnit
     *            The seed unit as IInstallableUnit, which contains the dependency information. May
     *            be <code>null</code>.
     * @param isAddOnFor
     *            Filter used to answer calls to {@link #isAddOnFor(String, String)}
     */
    public DependencySeed(String type, String id, /* String version, */Object installableUnit, Filter isAddOnFor) {
        this.type = type;
        this.id = id;
        this.installableUnit = installableUnit;
        this.addOnFilter = isAddOnFor;
    }

    /**
     * @return the type of the seed unit. See {@link ArtifactType} for known types. May be
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
     * @return the (qualified) version of the unit.
     */
//    public String getVersion() {
//        return version;
//    }

    /**
     * @return the seed unit as IInstallableUnit. May be <code>null</code>.
     */
    public/* IInstallableUnit */Object getInstallableUnit() {
        return installableUnit;
    }

    /**
     * Returns <code>true</code> if this dependency is an add-on for the given other dependency
     * seed. This is used to identify features which shall be installed at root level together with
     * products.
     * 
     * @param otherType
     *            Type of the other dependency seed (as returned by {@link #getType()})
     * @param otherId
     *            Identifier of the other dependency see (as returned by {@link #getId()})
     * @return <code>true</code> if this dependency is an add-on for the other dependency seed. The
     *         default is <code>false</code>.
     */
    public boolean isAddOnFor(String otherType, String otherId) {
        if (addOnFilter == null) {
            return false;
        } else {
            return addOnFilter.isAddOnFor(otherType, otherId);
        }
    }

    @Override
    public String toString() {
        return installableUnit.toString();
    }

    public interface Filter {
        /**
         * Returns <code>true</code> if this dependency is an add-on for the given dependency seed.
         */
        boolean isAddOnFor(String type, String id);
    }
}
