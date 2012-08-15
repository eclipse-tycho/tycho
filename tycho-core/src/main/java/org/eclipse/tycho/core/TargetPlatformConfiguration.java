/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;

public class TargetPlatformConfiguration implements DependencyResolverConfiguration {

    public static final String POM_DEPENDENCIES_CONSIDER = "consider";

    private String resolver;

    private List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();

    private boolean implicitTargetEnvironment = true;

    private File target;

    private String pomDependencies;

    private Boolean allowConflictingDependencies;

    private boolean disableP2Mirrors;

    private String executionEnvironment;

    private List<TargetPlatformFilter> filters;

    private OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

    private final List<Dependency> extraRequirements = new ArrayList<Dependency>();

    private boolean includePackedArtifacts;

    private Boolean considerLocalMetadata = null;

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

    public File getTarget() {
        return target;
    }

    public void addEnvironment(TargetEnvironment environment) {
        this.environments.add(environment);
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public void setPomDependencies(String pomDependencies) {
        this.pomDependencies = pomDependencies;
    }

    public String getPomDependencies() {
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

    public void setDisableP2Mirrors(boolean disableP2Mirrors) {
        this.disableP2Mirrors = disableP2Mirrors;
    }

    public boolean isDisableP2Mirrors() {
        return disableP2Mirrors;
    }

    public String getExecutionEnvironment() {
        return executionEnvironment;
    }

    public void setExecutionEnvironment(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
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

    public List<Dependency> getExtraRequirements() {
        return extraRequirements;
    }

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

    public void setConsiderLocalMetadata(Boolean consider) {
        considerLocalMetadata = consider;
    }

    /**
     * may be <code>null</code>
     */
    public Boolean getConsiderLocalMetadata() {
        return considerLocalMetadata;
    }
}
