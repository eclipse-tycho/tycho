/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - Bug 564363 - Make ReactorProject available in MavenContext
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.tycho.ReactorProject;

public abstract class MavenContextImpl implements MavenContext {

    private File localRepositoryRoot;
    private MavenLogger mavenLogger;
    private boolean offline;
    private Properties mergedProperties;
    private List<ReactorProject> projects = new ArrayList<>();

    public MavenContextImpl(File localRepositoryRoot, boolean offline, MavenLogger mavenLogger,
            Properties mergedProperties) {
        this.localRepositoryRoot = localRepositoryRoot;
        this.offline = offline;
        this.mavenLogger = mavenLogger;
        this.mergedProperties = mergedProperties;
    }

    // constructor for tests
    public MavenContextImpl(File localRepositoryRoot, MavenLogger mavenLogger) {
        this(localRepositoryRoot, false, mavenLogger, new Properties());
    }

    @Override
    public File getLocalRepositoryRoot() {
        return localRepositoryRoot;
    }

    @Override
    public MavenLogger getLogger() {
        return mavenLogger;
    }

    @Override
    public boolean isOffline() {
        return offline;
    }

    @Override
    public Properties getSessionProperties() {
        return mergedProperties;
    }

    @Override
    public Collection<ReactorProject> getProjects() {
        return Collections.unmodifiableCollection(projects);
    }

    public void addProject(ReactorProject reactorProject) {
        projects.add(reactorProject);
    }

}
