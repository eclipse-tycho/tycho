/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
