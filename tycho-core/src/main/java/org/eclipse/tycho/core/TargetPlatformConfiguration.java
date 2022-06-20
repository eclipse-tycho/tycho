/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - [Bug 550169] - Improve Tychos handling of includeSource="true" in target definition
 *                         [Bug 567098] - pomDependencies=consider should wrap non-osgi jars
 *                         [Issue 792]  - Support exclusion of certain dependencies from pom dependency consideration
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile;

public class TargetPlatformConfiguration implements DependencyResolverConfiguration {

    public enum BREEHeaderSelectionPolicy {
        first, minimal
    }

    private String resolver;

    private List<TargetEnvironment> environments = new ArrayList<>();

    private boolean implicitTargetEnvironment = true;

    private final List<URI> targets = new ArrayList<>();
    private IncludeSourceMode targetDefinitionIncludeSourceMode = IncludeSourceMode.honor;

    private PomDependencies pomDependencies = PomDependencies.ignore;

    private Boolean allowConflictingDependencies;

    private String executionEnvironment;
    private String executionEnvironmentDefault;
    private BREEHeaderSelectionPolicy breeHeaderSelectionPolicy = BREEHeaderSelectionPolicy.first;
    private boolean resolveWithEEConstraints = true;

    private List<TargetPlatformFilter> filters;

    private OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

    private final List<ArtifactKey> extraRequirements = new ArrayList<>();
    private final Set<String> exclusions = new HashSet<>();

    private Map<String, String> resolverProfileProperties = new HashMap<>();

    /**
     * Returns the list of configured target environments, or the running environment if no
     * environments have been specified explicitly.
     * 
     * @see #isImplicitTargetEnvironment()
     */
    public List<TargetEnvironment> getEnvironments() {
        return environments;
    }

    public String getTargetPlatformResolver() {
        return resolver;
    }

    public List<TargetDefinitionFile> getTargets() {
        return targets.stream().map(TargetDefinitionFile::read).collect(Collectors.toList());
    }

    public void addEnvironment(TargetEnvironment environment) {
        this.environments.add(environment);
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public void addTarget(File target) {
        addTarget(target.toURI());
    }

    public void addTarget(URI target) {
        this.targets.add(target);
    }

    public IncludeSourceMode getTargetDefinitionIncludeSourceMode() {
        return targetDefinitionIncludeSourceMode;
    }

    public void setTargetDefinitionIncludeSourceMode(IncludeSourceMode includeSourcesMode) {
        this.targetDefinitionIncludeSourceMode = includeSourcesMode;
    }

    public void setPomDependencies(PomDependencies pomDependencies) {
        this.pomDependencies = pomDependencies;
    }

    public PomDependencies getPomDependencies() {
        return pomDependencies;
    }

    public boolean isImplicitTargetEnvironment() {
        return implicitTargetEnvironment;
    }

    public void setImplicitTargetEnvironment(boolean implicitTargetEnvironment) {
        this.implicitTargetEnvironment = implicitTargetEnvironment;
    }

    public void setAllowConflictingDependencies(Boolean allow) {
        this.allowConflictingDependencies = allow;
    }

    public Boolean getAllowConflictingDependencies() {
        return allowConflictingDependencies;
    }

    public String getExecutionEnvironment() {
        return executionEnvironment;
    }

    public void setExecutionEnvironment(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    public String getExecutionEnvironmentDefault() {
        return executionEnvironmentDefault;
    }

    public void setExecutionEnvironmentDefault(String executionEnvironment) {
        this.executionEnvironmentDefault = executionEnvironment;
    }

    public BREEHeaderSelectionPolicy getBREEHeaderSelectionPolicy() {
        return breeHeaderSelectionPolicy;
    }

    public void setBREEHeaderSelectionPolicy(BREEHeaderSelectionPolicy breeHeaderSelectionPolicy) {
        this.breeHeaderSelectionPolicy = breeHeaderSelectionPolicy;
    }

    public boolean isResolveWithEEConstraints() {
        return resolveWithEEConstraints;
    }

    public void setResolveWithEEContraints(boolean value) {
        this.resolveWithEEConstraints = value;
    }

    public void setFilters(List<TargetPlatformFilter> filters) {
        this.filters = filters;
    }

    public List<TargetPlatformFilter> getFilters() {
        if (filters == null)
            return Collections.emptyList();
        else
            return filters;
    }

    public DependencyResolverConfiguration getDependencyResolverConfiguration() {
        return this;
    }

    @Override
    public List<ArtifactKey> getExtraRequirements() {
        return extraRequirements;
    }

    @Override
    public OptionalResolutionAction getOptionalResolutionAction() {
        return optionalAction;
    }

    public void addExtraRequirement(ArtifactKey requirement) {
        extraRequirements.add(requirement);
    }

    public void setOptionalResolutionAction(OptionalResolutionAction optionalAction) {
        this.optionalAction = optionalAction;
    }

    /**
     * Returns the properties to be used for evaluating filters during dependency resolution.
     */
    public Map<String, String> getProfileProperties() {
        return resolverProfileProperties;
    }

    public void addProfileProperty(String key, String value) {
        resolverProfileProperties.put(key, value);
    }

    public void addExclusion(String groupId, String artifactId) {
        exclusions.add(groupId + ":" + artifactId);
    }

    public boolean isExcluded(String groupId, String artifactId) {
        return exclusions.contains(groupId + ":" + artifactId);
    }

}
