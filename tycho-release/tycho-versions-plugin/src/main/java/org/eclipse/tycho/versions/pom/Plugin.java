/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
}
