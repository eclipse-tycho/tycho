/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.osgi;

import java.io.IOException;
import java.util.Map;

import org.apache.maven.project.MavenProject;

/**
 * A {@link OSGiFrameworkLauncher} allows to start an OSGi framework and execute some code inside
 * it.
 */
public interface OSGiFrameworkLauncher {

    /**
     * Boolean property that can be passed to the {@link #launchFramework(MavenProject, Map)} and is
     * interpreted in the way that all directly executed code is considered standalone executable in
     * a way that it does not need to import any classes from other bundles except the OSGi and Java
     * API. Launcher implementation might use this to perform more efficient execution.
     */
    static String STANDALONE = "OSGiFrameworkLauncher.standalone";

    /**
     * Launch a new Framework using the given properties, see <a href=
     * "https://docs.osgi.org/specification/osgi.core/8.0.0/framework.lifecycle.html#framework.lifecycle.launchingproperties">Launching
     * Properties</a> for standardized properties to use, while custom properties might be
     * supported. Especially the <code>org.osgi.framework.storage</code> should usually be defined
     * to allow the launcher to store files. Apart from that the launcher implementation should
     * choose sensible defaults for the provided type of launcher.
     * 
     * @param project
     *            the maven project this framework should be created for, some launchers might
     *            support to be created without a project
     * @param properties
     * 
     * @return the launched Framework
     * @throws IOException
     *             if creation of the framework fails due to I/O problems
     */
    OSGiFramework launchFramework(MavenProject project, Map<String, String> properties) throws IOException;
}
