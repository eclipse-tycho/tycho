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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import eu.maveniverse.domtrip.Element;

public class Profile {

    private final Element dom;

    public Profile(Element dom) {
        this.dom = dom;
    }

    public List<String> getModules() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Element modules : dom.children("modules").toList()) {
            for (Element module : modules.children("module").toList()) {
                result.add(module.textContentTrimmed());
            }
        }
        return new ArrayList<>(result);
    }

    public String getId() {
        Element child = dom.child("id").orElse(null);
        return child != null ? child.textContentTrimmed() : null;
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
