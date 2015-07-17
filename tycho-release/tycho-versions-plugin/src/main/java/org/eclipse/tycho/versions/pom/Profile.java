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
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.pdark.decentxml.Element;

public class Profile {

    private final Element dom;

    public Profile(Element dom) {
        this.dom = dom;
    }

    public List<String> getModules() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Element modules : dom.getChildren("modules")) {
            for (Element module : modules.getChildren("module")) {
                result.add(module.getTrimmedText());
            }
        }
        return new ArrayList<>(result);
    }

    public String getId() {
        Element child = dom.getChild("id");
        return child != null ? child.getTrimmedText() : null;
    }

    public Build getBuild() {
        return Build.getBuild(dom);
    }

    public List<GAV> getDependencies() {
        return Dependencies.getDependencies(dom);
    }

    public DependencyManagement getDependencyManagement() {
        return DependencyManagement.getDependencyManagement(dom);
    }

    public List<Property> getProperties() {
        return Property.getProperties(dom);
    }
}
