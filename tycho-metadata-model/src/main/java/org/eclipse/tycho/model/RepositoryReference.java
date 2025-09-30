/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - Bug 453708 Support for site/repository-reference/@location in eclipse-repository
 *******************************************************************************/
package org.eclipse.tycho.model;

import java.util.Objects;

import eu.maveniverse.domtrip.Element;

public class RepositoryReference {
    private final Element dom;

    @Override
    public int hashCode() {
        return Objects.hash(getLocation(), isEnabled());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof RepositoryReference other && //
                Objects.equals(getLocation(), other.getLocation()) && //
                isEnabled() == other.isEnabled());
    }

    @Override
    public String toString() {
        return getLocation() + "_" + isEnabled();
    }

    public RepositoryReference(Element dom) {
        this.dom = dom;
    }

    public RepositoryReference(String tagname) {
        this.dom = Element.of(tagname);
    }

    public String getLocation() {
        return dom.attribute("location");
    }

    public void setLocation(String location) {
        dom.attribute("location", location);
    }

    public String getName() {
        return dom.attribute("name");
    }

    public void setName(String name) {
        dom.attribute("name", name);
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(dom.attribute("enabled"));
    }

    public void setEnabled(boolean enabled) {
        dom.attribute("enabled", Boolean.toString(enabled));
    }

    Element getDom() {
        return dom;
    }
}
