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
package org.eclipse.tycho.model;

import de.pdark.decentxml.Element;

public class PluginRef {
    private final Element dom;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PluginRef))
            return false;
        PluginRef other = (PluginRef) obj;
        if (getId() == null) {
            if (other.getId() != null)
                return false;
        } else if (!getId().equals(other.getId()))
            return false;
        if (getVersion() == null) {
            if (other.getVersion() != null)
                return false;
        } else if (!getVersion().equals(other.getVersion()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getId() + "_" + getVersion();
    }

    public PluginRef(Element dom) {
        this.dom = dom;
    }

    public PluginRef(String name) {
        this.dom = new Element(name);
    }

    public String getId() {
        return dom.getAttributeValue("id");
    }

    public void setId(String id) {
        dom.setAttribute("id", id);
    }

    public String getVersion() {
        return dom.getAttributeValue("version");
    }

    public void setVersion(String version) {
        dom.setAttribute("version", version);
    }

    public String getOs() {
        return dom.getAttributeValue("os");
    }

    public void setOs(String os) {
        dom.setAttribute("os", os);
    }

    public String getWs() {
        return dom.getAttributeValue("ws");
    }

    public void setWs(String ws) {
        dom.setAttribute("ws", ws);
    }

    public String getArch() {
        return dom.getAttributeValue("arch");
    }

    public void setArch(String arch) {
        dom.setAttribute("arch", arch);
    }

    public boolean isUnpack() {
        return Boolean.parseBoolean(dom.getAttributeValue("unpack"));
    }

    public void setUnpack(boolean unpack) {
        dom.setAttribute("unpack", Boolean.toString(unpack));
    }

    public void setDownloadSide(long size) {
        dom.setAttribute("download-size", Long.toString(size));
    }

    public void setInstallSize(long size) {
        dom.setAttribute("install-size", Long.toString(size));
    }

}
