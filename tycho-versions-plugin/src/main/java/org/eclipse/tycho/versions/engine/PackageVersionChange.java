/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

/**
 * Describes an exported package version change.
 * <p>
 * FIXME the equals/hashcode are including the version to follow what was done in
 * {@link VersionChange} however it seems strange to do so even for VersionChange.
 */
public class PackageVersionChange extends VersionChange {

    private final String packageName;
    private String bundleSymbolicName;

    public PackageVersionChange(String bundleSymbolicName, String packageName, String version, String newVersion) {
        super(version, newVersion);
        this.bundleSymbolicName = bundleSymbolicName;
        this.packageName = packageName;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((bundleSymbolicName == null) ? 0 : bundleSymbolicName.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackageVersionChange other = (PackageVersionChange) obj;
        return bundleSymbolicName.equals(other.bundleSymbolicName) && packageName.equals(other.packageName)
                && super.equals(other);
    }

    @Override
    public String toString() {
        return "PackageVersionChange [" + packageName + ";version=" + super.toString() + "]";
    }

}
