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

public class FeatureRef {

    protected final Element dom;

    public FeatureRef(Element dom) {
        this.dom = dom;
    }

    public String getId() {
        return dom.getAttributeValue("id");
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

    public Element getDom() {
        return dom;
    }

}
