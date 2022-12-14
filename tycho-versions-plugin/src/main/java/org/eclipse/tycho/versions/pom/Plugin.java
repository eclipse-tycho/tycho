/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
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
}
