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

public class Build {
    private final Element dom;

    public Build(Element dom) {
        this.dom = dom;
    }

    public List<Plugin> getPlugins() {
        List<Plugin> result = new ArrayList<>();
        Element plugins = dom.getChild("plugins");
        if (plugins != null) {
            for (Element plugin : plugins.getChildren("plugin"))
                result.add(new Plugin(plugin));
        }
        return result;
    }

    public PluginManagement getPluginManagement() {
        Element pluginManagement = dom.getChild("pluginManagement");
        if (pluginManagement == null) {
            return null;
        }
        return new PluginManagement(pluginManagement);
    }

    public static Build getBuild(Element dom) {
        Element build = dom.getChild("build");
        return build != null ? new Build(build) : null;
    }
}
