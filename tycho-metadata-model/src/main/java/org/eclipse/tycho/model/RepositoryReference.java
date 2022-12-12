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

import de.pdark.decentxml.Element;

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
        this.dom = new Element(tagname);
    }

    public String getLocation() {
        return dom.getAttributeValue("location");
    }

    public void setLocation(String location) {
        dom.setAttribute("location", location);
    }

    public String getName() {
        return dom.getAttributeValue("name");
    }

    public void setName(String name) {
        dom.setAttribute("name", name);
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(dom.getAttributeValue("enabled"));
    }

    public void setEnabled(boolean enabled) {
        dom.setAttribute("enabled", Boolean.toString(enabled));
    }

    Element getDom() {
        return dom;
    }
}
