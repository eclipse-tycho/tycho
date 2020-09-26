/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - [Bug 550169] - Improve Tychos handling of includeSource="true" in target definition
 *                         [Bug 567098] - pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;

public class TargetPlatformConfiguration implements DependencyResolverConfiguration {

    public enum BREEHeaderSelectionPolicy {
        first, minimal
    }

    public enum PomDependencies {
        /**
         * pom dependencies are ignored
         */
        ignore,
        /**
         * pom dependencies are considered if the are already valid osgi/p2 artifacts
         */
        consider,
        /**
         * pom dependencies are used and wrapped if necessary
         */
        automated;
    }

    private String resolver;

    private List<TargetEnvironment> environments = new ArrayList<>();

    private boolean implicitTargetEnvironment = true;

    private final List<File> targets = new ArrayList<>();
    private IncludeSourceMode targetDefinitionIncludeSourceMode = IncludeSourceMode.honor;

    private PomDependencies pomDependencies;

    private Boolean allowConflictingDependencies;

    private String executionEnvironment;
    private String executionEnvironmentDefault;
    private BREEHeaderSelectionPolicy breeHeaderSelectionPolicy = BREEHeaderSelectionPolicy.first;
    private boolean resolveWithEEConstraints = true;

    private List<TargetPlatformFilter> filters;

    private OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

    private final List<Dependency> extraRequirements = new ArrayList<>();

    private boolean includePackedArtifacts;

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

    public List<File> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public void addEnvironment(TargetEnvironment environment) {
        this.environments.add(environment);
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public void addTarget(File target) {
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
    public List<Dependency> getExtraRequirements() {
        return extraRequirements;
    }

    @Override
    public OptionalResolutionAction getOptionalResolutionAction() {
        return optionalAction;
    }

    public void addExtraRequirement(Dependency requirement) {
        extraRequirements.add(requirement);
    }

    public void setOptionalResolutionAction(OptionalResolutionAction optionalAction) {
        this.optionalAction = optionalAction;
    }

    public void setIncludePackedArtifacts(boolean include) {
        this.includePackedArtifacts = include;
    }

    public boolean isIncludePackedArtifacts() {
        return includePackedArtifacts;
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

}
