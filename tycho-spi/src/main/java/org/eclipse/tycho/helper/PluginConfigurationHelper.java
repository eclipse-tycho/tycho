/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.helper;

import java.util.Optional;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A helper that can be used to access the configuration of the currently executing mojo
 */
@Component(role = PluginConfigurationHelper.class)
public class PluginConfigurationHelper {

    public Optional<Xpp3Dom> getDomOption(String name) {
        Optional<MojoExecution> execution = MojoExecutionHelper.getExecution();
        Optional<Xpp3Dom> configuration = execution.map(ex -> ex.getConfiguration());
        Optional<Xpp3Dom> child = configuration.map(cfg -> cfg.getChild(name));
        return child;
    }

    public Optional<String> getStringOption(String name) {
        return getDomOption(name).map(child -> {
            String value = child.getValue();
            if (value == null) {
                String attribute = child.getAttribute("default-value");
                return attribute;
            }
            return value;
        });
    }

    public Optional<Boolean> getBooleanOption(String name) {
        return getStringOption(name).map(s -> Boolean.valueOf(s));
    }

}
