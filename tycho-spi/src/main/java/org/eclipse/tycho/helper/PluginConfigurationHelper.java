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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A helper that can be used to access the configuration of the currently executing mojo
 */
@Component(role = PluginConfigurationHelper.class)
public class PluginConfigurationHelper {

    @Requirement
    ProjectHelper projectHelper;

    @Requirement
    LegacySupport legacySupport;

    private Map<URL, PluginDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public Configuration getConfiguration() {
        MojoExecution execution = MojoExecutionHelper.getExecution().orElse(null);
        if (execution == null) {
            return new Configuration(null);
        }
        Xpp3Dom configuration = execution.getConfiguration();
        return getConfiguration(configuration);
    }

    public Configuration getConfiguration(Xpp3Dom configuration) {
        return new Configuration(configuration);
    }

    public <M extends Mojo> Configuration getConfiguration(Class<M> mojo) {
        URL resource = mojo.getResource("/META-INF/maven/plugin.xml");
        if (resource == null) {
            throw new IllegalStateException("can't find plugin descriptor of mojo " + mojo.getName());
        }
        PluginDescriptor pluginDescriptor = descriptorCache.computeIfAbsent(resource, url -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                InterpolationFilterReader interpolationFilterReader = new InterpolationFilterReader(reader, Map.of());
                return new PluginDescriptorBuilder().build(interpolationFilterReader);
            } catch (Exception e) {
            }
            return null;
        });
        if (pluginDescriptor == null) {
            throw new IllegalStateException("can't load plugin descriptor of mojo " + mojo.getName());
        }
        for (MojoDescriptor mojoDescriptor : pluginDescriptor.getMojos()) {
            if (mojo.getName().equals(mojoDescriptor.getImplementation())) {
                Xpp3Dom configuration = projectHelper.getPluginConfiguration(pluginDescriptor.getGroupId(),
                        pluginDescriptor.getArtifactId(), mojoDescriptor.getGoal());
                return getConfiguration(configuration);

            }
        }
        throw new IllegalArgumentException("can't find mojo " + mojo.getName()
                + " goal in descriptor, possible goals are "
                + pluginDescriptor.getMojos().stream().map(MojoDescriptor::getGoal).collect(Collectors.joining(",")));
    }

    public static final class Configuration {

        private Xpp3Dom configuration;

        Configuration(Xpp3Dom configuration) {
            this.configuration = configuration;
        }

        public Optional<Configuration> getChild(String name) {
            if (configuration == null) {
                return Optional.empty();
            }
            Xpp3Dom child = configuration.getChild(name);
            if (child == null) {
                return Optional.empty();
            }
            return Optional.of(new Configuration(child));
        }

        public Optional<String> getString(String name) {
            return getChild(name).map(child -> {
                String value = child.configuration.getValue();
                if (value == null) {
                    return child.configuration.getAttribute("default-value");
                }
                return value;
            });
        }

        public Optional<Boolean> getBoolean(String name) {
            return getString(name).map(Boolean::valueOf);
        }

        public <E extends Enum<E>> Optional<E> getEnum(String name, Class<E> type) {
            return getString(name).map(value -> {

                for (E e : type.getEnumConstants()) {
                    if (e.name().equals(value)) {
                        return e;
                    }
                }
                return null;
            });
        }

    }

}
