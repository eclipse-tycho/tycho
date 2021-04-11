/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.container;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.core.dotClasspath.ClasspathParser;
import org.eclipse.tycho.core.dotClasspath.ContainerClasspathEntry;
import org.eclipse.tycho.core.dotClasspath.ProjectClasspathEntry;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class, hint = "JunitClassPathContainer")
public class JunitClassPathContainer implements P2MetadataProvider, Initializable {

    private final static String JUNIT3 = "3";
    private final static String JUNIT4 = "4";
    private final static String JUNIT5 = "5";

    private static final JUnitPluginDescription JUNIT3_PLUGIN = new JUnitPluginDescription("org.junit", "[3.8.2,3.9)");

    private static final JUnitPluginDescription JUNIT4_PLUGIN = new JUnitPluginDescription("org.junit",
            "[4.13.0,5.0.0)");

    private static final JUnitPluginDescription HAMCREST_CORE_PLUGIN = new JUnitPluginDescription("org.hamcrest.core",
            "[1.1.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_JUPITER_API_PLUGIN = new JUnitPluginDescription(
            "org.junit.jupiter.api", "[5.0.0,6.0.0)");

    private static final JUnitPluginDescription JUNIT_JUPITER_ENGINE_PLUGIN = new JUnitPluginDescription(
            "org.junit.jupiter.engine", "[5.0.0,6.0.0)");

    private static final JUnitPluginDescription JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN = new JUnitPluginDescription(
            "org.junit.jupiter.migrationsupport", "[5.0.0,6.0.0)");

    private static final JUnitPluginDescription JUNIT_JUPITER_PARAMS_PLUGIN = new JUnitPluginDescription(
            "org.junit.jupiter.params", "[5.0.0,6.0.0)");

    private static final JUnitPluginDescription JUNIT_PLATFORM_COMMONS_PLUGIN = new JUnitPluginDescription(
            "org.junit.platform.commons", "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_PLATFORM_ENGINE_PLUGIN = new JUnitPluginDescription(
            "org.junit.platform.engine", "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_PLATFORM_LAUNCHER_PLUGIN = new JUnitPluginDescription(
            "org.junit.platform.launcher", "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_PLATFORM_RUNNER_PLUGIN = new JUnitPluginDescription(
            "org.junit.platform.runner", "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_PLATFORM_SUITE_API_PLUGIN = new JUnitPluginDescription(
            "org.junit.platform.suite.api", "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_VINTAGE_ENGINE_PLUGIN = new JUnitPluginDescription(
            "org.junit.vintage.engine", "[4.12.0,6.0.0)");

    private static final JUnitPluginDescription JUNIT_OPENTEST4J_PLUGIN = new JUnitPluginDescription("org.opentest4j",
            "[1.0.0,2.0.0)");

    private static final JUnitPluginDescription JUNIT_APIGUARDIAN_PLUGIN = new JUnitPluginDescription("org.apiguardian",
            "[1.0.0,2.0.0)");

    @Requirement
    private ClasspathParser classpathParser;

    @Requirement
    private Logger logger;

    @Requirement
    private EquinoxServiceFactory equinox;

    private DependencyMetadataGenerator generator;

    @Override
    public void initialize() throws InitializationException {
        this.generator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=dependency-only)");
    }

    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        Collection<ProjectClasspathEntry> entries;
        try {
            entries = classpathParser.parse(project.getBasedir());
        } catch (IOException e) {
            logger.warn("Can't read classpath file", e);
            return Collections.emptyMap();
        }
        Map<String, IDependencyMetadata> result = new HashMap<>();
        for (ProjectClasspathEntry entry : entries) {
            if (entry instanceof ContainerClasspathEntry) {
                ContainerClasspathEntry container = (ContainerClasspathEntry) entry;
                String path = container.getContainerPath();
                if (path.startsWith(ContainerClasspathEntry.JUNIT_CONTAINER_PATH_PREFIX)) {
                    JUnitDependencyMetadata metadata = new JUnitDependencyMetadata(generator);
                    result.put(path, metadata);
                    String junit = path.substring(ContainerClasspathEntry.JUNIT_CONTAINER_PATH_PREFIX.length());
                    if (JUNIT3.equals(junit)) {
                        metadata.addJUnitPlugin(JUNIT3_PLUGIN);
                    } else if (JUNIT4.equals(junit)) {
                        metadata.addJUnitPlugin(JUNIT4_PLUGIN);
                        metadata.addJUnitPlugin(HAMCREST_CORE_PLUGIN);
                    } else if (JUNIT5.equals(junit)) {
                        metadata.addJUnitPlugin(JUNIT_JUPITER_API_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_JUPITER_ENGINE_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_JUPITER_PARAMS_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_PLATFORM_COMMONS_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_PLATFORM_ENGINE_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_PLATFORM_LAUNCHER_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_PLATFORM_RUNNER_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_PLATFORM_SUITE_API_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_VINTAGE_ENGINE_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_OPENTEST4J_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT_APIGUARDIAN_PLUGIN);
                        metadata.addJUnitPlugin(JUNIT4_PLUGIN);
                        metadata.addJUnitPlugin(HAMCREST_CORE_PLUGIN);
                    } else {
                        logger.warn("Unknwon JUnit container: " + path);
                    }
                }

            }
        }
        return result;
    }

    private static final class JUnitDependencyMetadata implements IDependencyMetadata {

        Set<Object> dependencyMetadata = new HashSet<>();
        private DependencyMetadataGenerator generator;

        public JUnitDependencyMetadata(DependencyMetadataGenerator generator) {
            this.generator = generator;
        }

        @Override
        public Set<?> getDependencyMetadata(DependencyMetadataType type) {
            if (type == DependencyMetadataType.COMPILE) {
                return dependencyMetadata;
            }
            return Collections.emptySet();
        }

        @Override
        public Set<?> getDependencyMetadata() {
            return Collections.emptySet();
        }

        @Override
        public void setDependencyMetadata(DependencyMetadataType type, Collection<?> units) {
            throw new UnsupportedOperationException();
        }

        private void addJUnitPlugin(JUnitPluginDescription pluginDescription) {
            dependencyMetadata
                    .add(generator.createBundleRequirement(pluginDescription.id, pluginDescription.versionRange));
        }
    }

    private static final class JUnitPluginDescription {

        private String id;
        private String versionRange;

        public JUnitPluginDescription(String id, String versionRange) {
            this.id = id;
            this.versionRange = versionRange;
        }

    }

}
