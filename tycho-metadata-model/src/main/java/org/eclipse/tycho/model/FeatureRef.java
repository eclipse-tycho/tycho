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

import java.util.Arrays;

import de.pdark.decentxml.Element;

public class FeatureRef {

    public enum InstallMode {
        include, root
    }

    protected final Element dom;

    public FeatureRef(Element dom) {
        this.dom = dom;
    }

    public FeatureRef(String name) {
        this(new Element(name));
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

    @Override
    public String toString() {
        return getId() + "_" + getVersion();
    }

    public String getOS() {
        return dom.getAttributeValue("os");
    }

    public void setOS(String value) {
        dom.setAttribute("os", value);
    }

    public String getArch() {
        return dom.getAttributeValue("arch");
    }

    public void setArch(String value) {
        dom.setAttribute("arch", value);
    }

    public String getWS() {
        return dom.getAttributeValue("ws");
    }

    public void setWS(String value) {
        dom.setAttribute("ws", value);
    }

    public InstallMode getInstallMode() throws ModelFileSyntaxException {
        String installModeString = dom.getAttributeValue("installMode");

        if (installModeString == null) {
            // default
            return InstallMode.include;

        } else {
            try {
                return InstallMode.valueOf(installModeString);
            } catch (IllegalArgumentException e) {
                throw new ModelFileSyntaxException("Invalid installMode \"" + installModeString + "\" in feature \""
                        + getId() + "\"; supported values are " + Arrays.toString(InstallMode.values()));
            }
        }
    }

    public Element getDom() {
        return dom;
    }

}
