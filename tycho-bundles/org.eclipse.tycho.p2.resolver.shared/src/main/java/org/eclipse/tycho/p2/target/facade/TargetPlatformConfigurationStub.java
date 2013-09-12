/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.facade;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;

// TODO 412416 add an TargetPlatformConfiguration interface with only getters, and add implementation backed by the POM configuration
public class TargetPlatformConfigurationStub {

    private boolean includePackedArtifacts;
    private List<TargetEnvironment> environments;
    private final List<TargetPlatformFilter> iuFilters = new ArrayList<TargetPlatformFilter>();

    private final Set<MavenRepositoryLocation> repositories = new LinkedHashSet<MavenRepositoryLocation>();
    private final List<TargetDefinition> targetDefinitions = new ArrayList<TargetDefinition>();
    private boolean forceIgnoreLocalArtifacts = false;

    private boolean failOnDuplicateIUs = true;

    public void setEnvironments(List<TargetEnvironment> environments) {
        this.environments = environments;
    }

    public List<TargetEnvironment> getEnvironments() {
        return environments;
    }

    public void setIncludePackedArtifacts(boolean include) {
        this.includePackedArtifacts = include;
    }

    public boolean getIncludePackedArtifacts() {
        return includePackedArtifacts;
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

    public void setFailOnDuplicateIUs(boolean failOnDuplicateIUs) {
        this.failOnDuplicateIUs = failOnDuplicateIUs;
    }

    public boolean getFailOnDuplicateIUs() {
        return failOnDuplicateIUs;
    }

    public void setForceIgnoreLocalArtifacts(boolean forceIgnoreLocalArtifacts) {
        this.forceIgnoreLocalArtifacts = forceIgnoreLocalArtifacts;
    }

    public boolean getForceIgnoreLocalArtifacts() {
        return forceIgnoreLocalArtifacts;
    }

}
