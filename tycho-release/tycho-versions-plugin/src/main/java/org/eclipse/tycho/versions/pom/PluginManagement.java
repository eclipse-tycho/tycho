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

public class PluginManagement {
    final Element pluginManagement;

    PluginManagement(Element pluginManagement) {
        this.pluginManagement = pluginManagement;
    }

    public List<Plugin> getPlugins() {
        List<Plugin> result = new ArrayList<>();
        Element plugins = pluginManagement.getChild("plugins");
        if (plugins != null) {
            for (Element plugin : plugins.getChildren("plugin"))
                result.add(new Plugin(plugin));
        }
        return result;
    }
}
