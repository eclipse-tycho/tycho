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
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model.classpath;

import java.util.Collection;
import java.util.List;

public interface JUnitClasspathContainerEntry extends ClasspathContainerEntry {

    static final String JUNIT_CONTAINER_PATH_PREFIX = "org.eclipse.jdt.junit.JUNIT_CONTAINER/";

    static final String JUNIT3 = "3";
    static final String JUNIT4 = "4";
    static final String JUNIT5 = "5";

    static final JUnitBundle JUNIT3_PLUGIN = JUnitBundle.of("org.junit", "[3.8.2,3.9)", "org.apache.servicemix.bundles",
            "org.apache.servicemix.bundles.junit");

    static final JUnitBundle JUNIT4_PLUGIN = JUnitBundle.of("org.junit", "[4.13.0,5.0.0)",
            "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.junit");

    static final JUnitBundle HAMCREST_CORE_PLUGIN = JUnitBundle.of("org.hamcrest", "[2.0.0,3.0.0)", "org.hamcrest",
            "hamcrest");

    static final JUnitBundle JUNIT_JUPITER_API_PLUGIN = JUnitBundle.of("junit-jupiter-api", "[5.0.0,6.0.0)",
            "org.junit.jupiter", "junit-jupiter-api");

    static final JUnitBundle JUNIT_JUPITER_ENGINE_PLUGIN = JUnitBundle.of("junit-jupiter-engine", "[5.0.0,6.0.0)",
            "org.junit.jupiter", "junit-jupiter-engine");

    static final JUnitBundle JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN = JUnitBundle.of("junit-jupiter-migrationsupport",
            "[5.0.0,6.0.0)", "org.junit.jupiter", "junit-jupiter-migrationsupport");

    static final JUnitBundle JUNIT_JUPITER_PARAMS_PLUGIN = JUnitBundle.of("junit-jupiter-params", "[5.0.0,6.0.0)",
            "org.junit.jupiter", "junit-jupiter-params");

    static final JUnitBundle JUNIT_PLATFORM_COMMONS_PLUGIN = JUnitBundle.of("junit-platform-commons", "[1.0.0,2.0.0)",
            "org.junit.platform", "junit-platform-commons");

    static final JUnitBundle JUNIT_PLATFORM_ENGINE_PLUGIN = JUnitBundle.of("junit-platform-engine", "[1.0.0,2.0.0)",
            "org.junit.platform", "junit-platform-engine");

    static final JUnitBundle JUNIT_PLATFORM_LAUNCHER_PLUGIN = JUnitBundle.of("junit-platform-launcher", "[1.0.0,2.0.0)",
            "org.junit.platform", "junit-platform-launcher");

    static final JUnitBundle JUNIT_PLATFORM_RUNNER_PLUGIN = JUnitBundle.of("junit-platform-runner", "[1.0.0,2.0.0)",
            "org.junit.platform", "junit-platform-runner");

    static final JUnitBundle JUNIT_PLATFORM_SUITE_API_PLUGIN = JUnitBundle.of("junit-platform-suite-api",
            "[1.0.0,2.0.0)", "org.junit.platform", "junit-platform-suite-api");

    static final JUnitBundle JUNIT_VINTAGE_ENGINE_PLUGIN = JUnitBundle.of("junit-vintage-engine", "[4.12.0,6.0.0)",
            "org.junit.vintage", "junit-vintage-engine");

    static final JUnitBundle JUNIT_OPENTEST4J_PLUGIN = JUnitBundle.of("org.opentest4j", "[1.0.0,2.0.0)",
            "org.opentest4j", "opentest4j");

    static final JUnitBundle JUNIT_APIGUARDIAN_PLUGIN = JUnitBundle.of("org.apiguardian.api", "[1.0.0,2.0.0)",
            "org.apiguardian", "apiguardian-api");

    static final List<JUnitBundle> JUNIT3_PLUGINS = List.of(JUNIT3_PLUGIN);
    static final List<JUnitBundle> JUNIT4_PLUGINS = List.of(JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);
    static final List<JUnitBundle> JUNIT5_PLUGINS = List.of(JUNIT_JUPITER_API_PLUGIN, JUNIT_JUPITER_ENGINE_PLUGIN,
            JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN, JUNIT_JUPITER_PARAMS_PLUGIN, JUNIT_PLATFORM_COMMONS_PLUGIN,
            JUNIT_PLATFORM_ENGINE_PLUGIN, JUNIT_PLATFORM_LAUNCHER_PLUGIN, JUNIT_PLATFORM_RUNNER_PLUGIN,
            JUNIT_PLATFORM_SUITE_API_PLUGIN, JUNIT_VINTAGE_ENGINE_PLUGIN, JUNIT_OPENTEST4J_PLUGIN,
            JUNIT_APIGUARDIAN_PLUGIN, JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);

    static final List<JUnitBundle> JUNIT5_WITHOUT_VINTAGE_PLUGINS = List.of(JUNIT_JUPITER_API_PLUGIN,
            JUNIT_JUPITER_ENGINE_PLUGIN, JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN, JUNIT_JUPITER_PARAMS_PLUGIN,
            JUNIT_PLATFORM_COMMONS_PLUGIN, JUNIT_PLATFORM_ENGINE_PLUGIN, JUNIT_PLATFORM_LAUNCHER_PLUGIN,
            JUNIT_PLATFORM_RUNNER_PLUGIN, JUNIT_PLATFORM_SUITE_API_PLUGIN, JUNIT_OPENTEST4J_PLUGIN,
            JUNIT_APIGUARDIAN_PLUGIN, HAMCREST_CORE_PLUGIN);

    /**
     * 
     * @return the JUnit part of the path
     */
    String getJUnitSegment();

    /**
     * 
     * @return the list of artifacts that are part of this container
     */
    Collection<JUnitBundle> getArtifacts();

    @Override
    default boolean isTest() {
        return true;
    }

    /**
     * Checks if for JUnit5 the vintage engine has to be included. This is only meaningful if
     * {@link #getJUnitSegment()} is equal to {@link #JUNIT5}
     * 
     * @return <code>true</code> if vintage is enabled (the default)
     */
    boolean isVintage();

}
