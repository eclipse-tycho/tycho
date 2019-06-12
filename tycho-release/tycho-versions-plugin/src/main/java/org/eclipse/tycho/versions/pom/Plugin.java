/*******************************************************************************
 * Copyright (c) 2013, 2019 Igor Fedorenko and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Xored Software Inc - #518354 - add update extraClasspathElement's version
 *******************************************************************************/
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Element;

public class Plugin {
    final Element plugin;

    Plugin(Element plugin) {
        this.plugin = plugin;
    }

    public GAV getGAV() {
        return new GAV(plugin);
    }

    public List<GAV> getDependencies() {
        ArrayList<GAV> result = new ArrayList<>();
        Element dependencies = plugin.getChild("dependencies");
        if (dependencies != null) {
            for (Element dependency : dependencies.getChildren("dependency")) {
                result.add(new GAV(dependency));
            }
        }
        return result;
    }

    public List<GAV> getTargetArtifacts() {
        ArrayList<GAV> result = new ArrayList<>();
        Element configuration = plugin.getChild("configuration");
        if (configuration == null) {
            return result;
        }
        Element target = configuration.getChild("target");
        if (target == null) {
            return result;
        }
        for (Element artifact : target.getChildren("artifact")) {
            result.add(new GAV(artifact));
        }
        return result;
    }

    public List<GAV> getExtraClasspathElements() {
        final ArrayList<GAV> result = new ArrayList<>();
        final Element configuration = plugin.getChild("configuration");
        if (configuration == null) {
            return result;
        }
        final Element extraClasspathElements = configuration.getChild("extraClasspathElements");
        if (extraClasspathElements == null) {
            return result;
        }
        for (final Element extraClasspathElement : extraClasspathElements.getChildren("extraClasspathElement")) {
            result.add(new GAV(extraClasspathElement));
        }
        return result;
    }
}
