/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model;

import java.util.Objects;

import de.pdark.decentxml.Element;

public class PluginRef {
    private final Element dom;

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getVersion());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof PluginRef other && //
                        Objects.equals(getId(), other.getId()) && //
                        Objects.equals(getVersion(), other.getVersion()));
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

    /**
     * @deprecated The installation format (packed/unpacked) shall be specified through the bundle's
     *             Eclipse-BundleShape manifest header. The feature.xml's unpack attribute may not
     *             be supported in a future version of Tycho.
     */
    @Deprecated
    public boolean isUnpack() {
        return Boolean.parseBoolean(dom.getAttributeValue("unpack"));
    }

    /**
     * @deprecated The installation format (packed/unpacked) shall be specified through the bundle's
     *             Eclipse-BundleShape manifest header. The feature.xml's unpack attribute may not
     *             be supported in a future version of Tycho.
     */
    @Deprecated
    public void setUnpack(boolean unpack) {
        dom.setAttribute("unpack", Boolean.toString(unpack));
    }

    public long getDownloadSize() {
        return Long.parseLong(dom.getAttributeValue("download-size"));
    }

    public void setDownloadSize(long size) {
        dom.setAttribute("download-size", Long.toString(size));
    }

    public long getInstallSize() {
        return Long.parseLong(dom.getAttributeValue("install-size"));
    }

    public void setInstallSize(long size) {
        dom.setAttribute("install-size", Long.toString(size));
    }

    Element getDom() {
        return dom;
    }
}
