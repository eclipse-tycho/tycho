/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - Bug 564363 - add access to reactor projects
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;

/**
 * Makes maven information which is constant for the whole maven session available as a service to
 * the embedded OSGi runtime.
 */
public interface MavenContext {

    public File getLocalRepositoryRoot();

    public MavenLogger getLogger();

    /**
     * whether maven was started in offline mode (CLI option "-o")
     */
    public boolean isOffline();

    /**
     * whether maven was started with the update-snapshots mode (CLI option "-U")
     */
    boolean isUpdateSnapshots();

    /**
     * Session-global properties merged from (in order of precedence)
     * <ol>
     * <li>user properties ("-Dkey=value" via CLI)</li>
     * <li>properties in active profiles of settings.xml</li>
     * <li>system properties</li>
     * </ol>
     */
    public Properties getSessionProperties();

    /**
     * 
     * @return collection of all reactor projects
     */
    public Collection<ReactorProject> getProjects();

    /**
     * Returns the assigned extension for a given artifact type
     * 
     * @param artifactType
     *            type of the artifact
     * @return the extension for the given type
     */
    public String getExtension(String artifactType);

    /**
     * 
     * @return a collection of all {@link MavenRepositoryLocation}s know to the maven context
     */
    Stream<MavenRepositoryLocation> getMavenRepositoryLocations();

}
