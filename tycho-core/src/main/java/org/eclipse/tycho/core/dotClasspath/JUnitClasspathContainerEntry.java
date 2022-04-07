/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.dotClasspath;

import java.util.Collection;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;

public interface JUnitClasspathContainerEntry extends ClasspathContainerEntry {

    static final String JUNIT_CONTAINER_PATH_PREFIX = "org.eclipse.jdt.junit.JUNIT_CONTAINER/";

    static final String JUNIT3 = "3";
    static final String JUNIT4 = "4";
    static final String JUNIT5 = "5";

    static final ArtifactKey JUNIT3_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit",
            "[3.8.2,3.9)");

    static final ArtifactKey JUNIT4_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit",
            "[4.13.0,5.0.0)");

    static final ArtifactKey HAMCREST_CORE_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.hamcrest.core", "[1.1.0,2.0.0)");

    static final ArtifactKey JUNIT_JUPITER_API_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.jupiter.api", "[5.0.0,6.0.0)");

    static final ArtifactKey JUNIT_JUPITER_ENGINE_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.jupiter.engine", "[5.0.0,6.0.0)");

    static final ArtifactKey JUNIT_JUPITER_MIGRATIONSUPPORT_PLUGIN = new DefaultArtifactKey(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit.jupiter.migrationsupport", "[5.0.0,6.0.0)");

    static final ArtifactKey JUNIT_JUPITER_PARAMS_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.jupiter.params", "[5.0.0,6.0.0)");

    static final ArtifactKey JUNIT_PLATFORM_COMMONS_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.platform.commons", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_PLATFORM_ENGINE_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.platform.engine", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_PLATFORM_LAUNCHER_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.platform.launcher", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_PLATFORM_RUNNER_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.platform.runner", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_PLATFORM_SUITE_API_PLUGIN = new DefaultArtifactKey(
            ArtifactType.TYPE_INSTALLABLE_UNIT, "org.junit.platform.suite.api", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_VINTAGE_ENGINE_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.junit.vintage.engine", "[4.12.0,6.0.0)");

    static final ArtifactKey JUNIT_OPENTEST4J_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.opentest4j", "[1.0.0,2.0.0)");

    static final ArtifactKey JUNIT_APIGUARDIAN_PLUGIN = new DefaultArtifactKey(ArtifactType.TYPE_INSTALLABLE_UNIT,
            "org.apiguardian", "[1.0.0,2.0.0)");

    /**
     * 
     * @return the JUnit part of the path
     */
    String getJUnitSegment();

    /**
     * 
     * @return the list of artifacts that are part of this container
     */
    Collection<ArtifactKey> getArtifacts();

}
