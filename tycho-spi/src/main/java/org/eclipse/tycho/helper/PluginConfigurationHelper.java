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

import java.util.List;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A helper that can be used to access the configuration of the currently executing mojo
 */
public interface PluginConfigurationHelper {

    Configuration getConfiguration();

    Configuration getConfiguration(Xpp3Dom configuration);

    Configuration getConfiguration(String pluginGroupId, String pluginArtifactId, String goal,
            MavenProject project, MavenSession mavenSession);

    <M extends Mojo> Configuration getConfiguration(Class<M> mojo);

    <M extends Mojo> Configuration getConfiguration(Class<M> mojo, MavenProject project,
            MavenSession mavenSession);

    /**
     * Configuration wrapper for accessing plugin configuration
     */
    public interface Configuration {

        Optional<Configuration> getChild(String name);

        Optional<String> getString(String name);

        Optional<Boolean> getBoolean(String name);

        <E extends Enum<E>> Optional<E> getEnum(String name, Class<E> type);

        Optional<List<String>> getStringList(String name);

    }

}
