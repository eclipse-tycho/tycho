/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Element;

public class DependencyManagement {
    final Element dependencyManagement;

    DependencyManagement(Element dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    public List<GAV> getDependencies() {

        List<GAV> result = new ArrayList<>();

        Element dependencies = dependencyManagement.getChild("dependencies");

        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency"))
                result.add(new GAV(dependency));
        }

        return result;
    }

    public static DependencyManagement getDependencyManagement(Element dom) {
        Element dependencyManagement = dom.getChild("dependencyManagement");
        if (dependencyManagement == null) {
            return null;
        }
        return new DependencyManagement(dependencyManagement);
    }

}
