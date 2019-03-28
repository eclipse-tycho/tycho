/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour - Bug 453708 Support for site/repository-reference/@location in eclipse-repository
 *******************************************************************************/
package org.eclipse.tycho.model;

import de.pdark.decentxml.Element;

public class RepositoryRef {
    private final Element dom;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
        result = prime * result + ((isEnabled()) ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RepositoryRef))
            return false;
        RepositoryRef other = (RepositoryRef) obj;
        if (getLocation() == null) {
            if (other.getLocation() != null)
                return false;
        } else if (!getLocation().equals(other.getLocation()))
            return false;
        if (!isEnabled() != other.isEnabled())
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getLocation() + "_" + isEnabled();
    }

    public RepositoryRef(Element dom) {
        this.dom = dom;
    }

    public RepositoryRef(String tagname) {
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
