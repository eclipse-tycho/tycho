/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class PackageVersionChange {

    private final String packageName;
    private final String version;
    private final String newVersion;
    private String bundleSymbolicName;

    public PackageVersionChange(String bundleSymbolicName, String packageName, String version, String newVersion) {
        this.bundleSymbolicName = bundleSymbolicName;
        this.packageName = packageName;
        this.version = version;
        this.newVersion = newVersion;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public String getNewVersion() {
        return newVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundleSymbolicName == null) ? 0 : bundleSymbolicName.hashCode());
        result = prime * result + ((newVersion == null) ? 0 : newVersion.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
                && version.equals(other.version) && newVersion.equals(other.newVersion);
    }

    @Override
    public String toString() {
        return "PackageVersionChange [" + packageName + ";version=" + version + " => " + newVersion + "]";
    }

}
