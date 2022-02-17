/*******************************************************************************
 * Copyright (c) 2013, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich    - [Bug 550169] - Improve Tychos handling of includeSource="true" in target definition
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.shared.TargetEnvironment;

// TODO 412416 add an TargetPlatformConfiguration interface with only getters, and add implementation backed by the POM configuration
public class TargetPlatformConfigurationStub {

    private List<TargetEnvironment> environments;
    private final List<TargetPlatformFilter> iuFilters = new ArrayList<>();

    private final Set<MavenRepositoryLocation> repositories = new LinkedHashSet<>();
    private final List<TargetDefinition> targetDefinitions = new ArrayList<>();
    private boolean forceIgnoreLocalArtifacts = false;
    private IncludeSourceMode includeSourceMode = IncludeSourceMode.honor;

    public void setEnvironments(List<TargetEnvironment> environments) {
        this.environments = environments;
    }

    public List<TargetEnvironment> getEnvironments() {
        return environments;
    }

    public void addFilters(List<TargetPlatformFilter> filters) {
        this.iuFilters.addAll(filters);
    }

    public List<TargetPlatformFilter> getFilters() {
        return iuFilters;
    }

    public void addP2Repository(MavenRepositoryLocation location) {
        this.repositories.add(location);
    }

    // convenience method for tests
    public void addP2Repository(URI location) {
        addP2Repository(new MavenRepositoryLocation(null, location));
    }

    public Set<MavenRepositoryLocation> getP2Repositories() {
        return repositories;
    }

    public void addTargetDefinition(TargetDefinition definition) {
        targetDefinitions.add(definition);
    }

    public List<TargetDefinition> getTargetDefinitions() {
        return targetDefinitions;
    }

    public void setForceIgnoreLocalArtifacts(boolean forceIgnoreLocalArtifacts) {
        this.forceIgnoreLocalArtifacts = forceIgnoreLocalArtifacts;
    }

    public boolean getForceIgnoreLocalArtifacts() {
        return forceIgnoreLocalArtifacts;
    }

    public IncludeSourceMode getIncludeSourceMode() {
        return includeSourceMode;
    }

    public void setIncludeSourceMode(IncludeSourceMode includeSourceMode) {
        this.includeSourceMode = includeSourceMode;
    }

}
