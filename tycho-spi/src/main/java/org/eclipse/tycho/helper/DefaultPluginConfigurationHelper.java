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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A helper that can be used to access the configuration of the currently executing mojo
 */
@Named
public class DefaultPluginConfigurationHelper implements PluginConfigurationHelper {

    @Inject
    ProjectHelper projectHelper;

    @Inject
    LegacySupport legacySupport;

    private Map<URL, PluginDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public Configuration getConfiguration() {
        MojoExecution execution = MojoExecutionHelper.getExecution().orElse(null);
        TypeAwareExpressionEvaluator evaluator = getEvaluator(legacySupport.getSession(), execution);
        if (execution == null) {
            return new Configuration(null, evaluator);
        }
        Xpp3Dom configuration = execution.getConfiguration();
        return new Configuration(configuration, evaluator);
    }

    private TypeAwareExpressionEvaluator getEvaluator(MavenSession mavenSession, MojoExecution execution) {
        if (mavenSession == null) {
            return null;
        }
        return new PluginParameterExpressionEvaluator(mavenSession, execution);
    }

    public Configuration getConfiguration(Xpp3Dom configuration) {
        return new Configuration(configuration, getEvaluator(legacySupport.getSession(), null));
    }

    public Configuration getConfiguration(String pluginGroupId, String pluginArtifactId, String goal,
            MavenProject project, MavenSession mavenSession) {
        return new Configuration(
                projectHelper.getPluginConfiguration(pluginGroupId, pluginArtifactId, goal, project, mavenSession),
                getEvaluator(mavenSession, null));
    }

    public <M extends Mojo> Configuration getConfiguration(Class<M> mojo) {
        MavenSession currentSession = legacySupport.getSession();
        if (currentSession == null) {
            return getConfiguration((Xpp3Dom) null);
        }
        MavenProject currentProject = currentSession.getCurrentProject();
        if (currentProject == null) {
            return getConfiguration((Xpp3Dom) null);
        }
        return getConfiguration(mojo, currentProject, currentSession);
    }

    public <M extends Mojo> Configuration getConfiguration(Class<M> mojo, MavenProject project,
            MavenSession mavenSession) {
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
                        pluginDescriptor.getArtifactId(), mojoDescriptor.getGoal(), project, mavenSession);
                return getConfiguration(configuration);

            }
        }
        throw new IllegalArgumentException("can't find mojo " + mojo.getName()
                + " goal in descriptor, possible goals are "
                + pluginDescriptor.getMojos().stream().map(MojoDescriptor::getGoal).collect(Collectors.joining(",")));
    }

    public static final class Configuration implements PluginConfigurationHelper.Configuration {

        private Xpp3Dom configuration;
        private TypeAwareExpressionEvaluator evaluator;

        Configuration(Xpp3Dom configuration, TypeAwareExpressionEvaluator evaluator) {
            this.configuration = configuration;
            this.evaluator = evaluator;
        }

        @Override
        public Optional<PluginConfigurationHelper.Configuration> getChild(String name) {
            if (configuration == null) {
                return Optional.empty();
            }
            Xpp3Dom child = configuration.getChild(name);
            if (child == null) {
                return Optional.empty();
            }
            return Optional.of(new Configuration(child, evaluator));
        }

        @Override
        public Optional<String> getString(String name) {
            return getChild(name).map(child -> {
                return getValue(((Configuration) child).configuration);
            });
        }

        @Override
        public Optional<Boolean> getBoolean(String name) {
            return getString(name).map(Boolean::valueOf);
        }

        @Override
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

        @Override
        public String toString() {
            return configuration == null ? "-empty configuration-" : String.valueOf(configuration);
        }

        @Override
        public Optional<List<String>> getStringList(String name) {
            return getChild(name).map(child -> {
                return Arrays.stream(((Configuration) child).configuration.getChildren()).map(this::getValue).toList();
            });

        }

        private String getValue(Xpp3Dom dom) {
            String value = dom.getValue();
            if (value == null) {
                value = dom.getAttribute("default-value");
            }
            if (value != null && evaluator != null) {
                try {
                    return (String) evaluator.evaluate(value, String.class);
                } catch (ExpressionEvaluationException e) {
                }
            }
            return value;
        }
    }

}
