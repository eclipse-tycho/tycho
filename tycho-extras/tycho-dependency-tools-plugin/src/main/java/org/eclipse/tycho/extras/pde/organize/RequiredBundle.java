/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.organize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;

public final class RequiredBundle {

    private String bundleSymbolicName;
    private Attrs attrs;
    private VersionRange version;
    private List<RequiredBundle> childs = new ArrayList<>();

    public RequiredBundle(String bundleSymbolicName, Attrs attrs) {
        this.bundleSymbolicName = bundleSymbolicName;
        this.attrs = attrs;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public VersionRange getVersionRange() {
        if (this.version == null) {
            String version = attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE, "0");
            this.version = VersionRange.valueOf(version);
        }
        return this.version;
    }

    public boolean isReexport() {
        return Constants.VISIBILITY_REEXPORT.equals(attrs.get(Constants.VISIBILITY_DIRECTIVE + ":"));
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundleSymbolicName, getVersionRange());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequiredBundle other = (RequiredBundle) obj;
        return Objects.equals(bundleSymbolicName, other.bundleSymbolicName)
                && Objects.equals(getVersionRange(), other.getVersionRange());
    }

    public void addChild(RequiredBundle child) {
        childs.add(child);
    }

    public Stream<RequiredBundle> childs(boolean transitive) {
        Stream<RequiredBundle> stream = childs.stream();
        if (transitive) {
            return stream.flatMap(c -> Stream.concat(Stream.of(c), c.childs(transitive))).distinct();
        }
        return stream;
    }

    @Override
    public String toString() {
        return getBundleSymbolicName() + " " + getVersionRange() + (isReexport() ? " (reexported)" : "");
    }

}
