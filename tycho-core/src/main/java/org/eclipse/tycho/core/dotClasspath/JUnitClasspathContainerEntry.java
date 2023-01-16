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
package org.eclipse.tycho.core.dotClasspath;

import java.util.Collection;
import java.util.List;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenArtifactKey;

public interface JUnitClasspathContainerEntry extends ClasspathContainerEntry {

    static final String JUNIT_CONTAINER_PATH_PREFIX = "org.eclipse.jdt.junit.JUNIT_CONTAINER/";

    static final String JUNIT3 = "3";
    static final String JUNIT4 = "4";
    static final String JUNIT5 = "5";

    static final MavenArtifactKey JUNIT3_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit",
            "[3.8.2,3.9)", "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.junit");

    static final MavenArtifactKey JUNIT4_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit",
            "[4.13.0,5.0.0)", "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.junit");

    static final MavenArtifactKey HAMCREST_CORE_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.hamcrest.core", "[1.1.0,2.0.0)", "org.hamcrest", "hamcrest-core");

    static final MavenArtifactKey JUNIT_JUPITER_API_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-jupiter-api", "[5.0.0,6.0.0)", "org.junit.jupiter", "junit-jupiter-api");

    static final MavenArtifactKey JUNIT_JUPITER_ENGINE_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-jupiter-engine", "[5.0.0,6.0.0)", "org.junit.jupiter", "junit-jupiter-engine");

    static final MavenArtifactKey JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN = MavenArtifactKey.of(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "junit-jupiter-migrationsupport", "[5.0.0,6.0.0)", "org.junit.jupiter",
            "junit-jupiter-migrationsupport");

    static final MavenArtifactKey JUNIT_JUPITER_PARAMS_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-jupiter-params", "[5.0.0,6.0.0)", "org.junit.jupiter", "junit-jupiter-params");

    static final MavenArtifactKey JUNIT_PLATFORM_COMMONS_PLUGIN = MavenArtifactKey.of(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "junit-platform-commons", "[1.0.0,2.0.0)", "org.junit.platform",
            "junit-platform-commons");

    static final MavenArtifactKey JUNIT_PLATFORM_ENGINE_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-platform-engine", "[1.0.0,2.0.0)", "org.junit.platform", "junit-platform-engine");

    static final MavenArtifactKey JUNIT_PLATFORM_LAUNCHER_PLUGIN = MavenArtifactKey.of(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "junit-platform-launcher", "[1.0.0,2.0.0)", "org.junit.platform",
            "junit-platform-launcher");

    static final MavenArtifactKey JUNIT_PLATFORM_RUNNER_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-platform-runner", "[1.0.0,2.0.0)", "org.junit.platform", "junit-platform-runner");

    static final MavenArtifactKey JUNIT_PLATFORM_SUITE_API_PLUGIN = MavenArtifactKey.of(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "junit-platform-suite-api", "[1.0.0,2.0.0)", "org.junit.platform",
            "junit-platform-suite-api");

    static final MavenArtifactKey JUNIT_VINTAGE_ENGINE_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "junit-vintage-engine", "[4.12.0,6.0.0)", "org.junit.vintage", "junit-vintage-engine");

    static final MavenArtifactKey JUNIT_OPENTEST4J_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.opentest4j", "[1.0.0,2.0.0)", "org.opentest4j", "opentest4j");

    static final MavenArtifactKey JUNIT_APIGUARDIAN_PLUGIN = MavenArtifactKey.of(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.apiguardian.api", "[1.0.0,2.0.0)", "org.apiguardian", "apiguardian-api");

    static final List<MavenArtifactKey> JUNIT3_PLUGINS = List.of(JUNIT3_PLUGIN);
    static final List<MavenArtifactKey> JUNIT4_PLUGINS = List.of(JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);
    static final List<MavenArtifactKey> JUNIT5_PLUGINS = List.of(JUNIT_JUPITER_API_PLUGIN, JUNIT_JUPITER_ENGINE_PLUGIN,
            JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN, JUNIT_JUPITER_PARAMS_PLUGIN, JUNIT_PLATFORM_COMMONS_PLUGIN,
            JUNIT_PLATFORM_ENGINE_PLUGIN, JUNIT_PLATFORM_LAUNCHER_PLUGIN, JUNIT_PLATFORM_RUNNER_PLUGIN,
            JUNIT_PLATFORM_SUITE_API_PLUGIN, JUNIT_VINTAGE_ENGINE_PLUGIN, JUNIT_OPENTEST4J_PLUGIN,
            JUNIT_APIGUARDIAN_PLUGIN, JUNIT4_PLUGIN, HAMCREST_CORE_PLUGIN);

    /**
     * 
     * @return the JUnit part of the path
     */
    String getJUnitSegment();

    /**
     * 
     * @return the list of artifacts that are part of this container
     */
    Collection<MavenArtifactKey> getArtifacts();

}
