/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.versions.pom;

import de.pdark.decentxml.Element;

public class GAV {
    private final Element dom;

    public GAV(Element element) {
        this.dom = element;
    }

    public String getGroupId() {
        return getChildText("groupId");
    }

    public String getArtifactId() {
        return getChildText("artifactId");
    }

    public String getVersion() {
        return getChildText("version");
    }

    public void setVersion(String version) {
        dom.getChild("version").setText(version);
    }

    private String getChildText(String name) {
        Element child = dom.getChild(name);
        return child != null ? child.getTrimmedText() : null;
    }
}
