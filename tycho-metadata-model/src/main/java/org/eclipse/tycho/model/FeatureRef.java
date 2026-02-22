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

import eu.maveniverse.domtrip.Element;

public class FeatureRef {

    public enum InstallMode {
        include, root
    }

    protected final Element dom;

    public FeatureRef(Element dom) {
        this.dom = dom;
    }

    public FeatureRef(String name) {
        this(Element.of(name));
    }

    public String getId() {
        return dom.attribute("id");
    }

    public void setId(String id) {
        dom.attribute("id", id);
    }

    public String getVersion() {
        return dom.attribute("version");
    }

    public void setVersion(String version) {
        dom.attribute("version", version);
    }

    @Override
    public String toString() {
        return getId() + "_" + getVersion();
    }

    public String getOS() {
        return dom.attribute("os");
    }

    public void setOS(String value) {
        dom.attribute("os", value);
    }

    public String getArch() {
        return dom.attribute("arch");
    }

    public void setArch(String value) {
        dom.attribute("arch", value);
    }

    public String getWS() {
        return dom.attribute("ws");
    }

    public void setWS(String value) {
        dom.attribute("ws", value);
    }

    public InstallMode getInstallMode() throws ModelFileSyntaxException {
        String installModeString = dom.attribute("installMode");

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
