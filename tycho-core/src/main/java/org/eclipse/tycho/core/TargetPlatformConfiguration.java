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
import java.util.List;

import org.apache.maven.model.Dependency;

public class TargetPlatformConfiguration {

    public static final String POM_DEPENDENCIES_CONSIDER = "consider";

    public static final String OPTIONAL_RESOLUTION_REQUIRE = "require";

    public static final String OPTIONAL_RESOLUTION_IGNORE = "ignore";

    private String resolver;

    private List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();

    private List<Dependency> extraRequirements = new ArrayList<Dependency>();

    private boolean implicitTargetEnvironment = true;

    private File target;

    private String pomDependencies;

    private Boolean allowConflictingDependencies;

    private boolean disableP2Mirrors;

    private String optionalAction;

    private String executionEnvironment;

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

    public void addExtraRequirement(Dependency requirement) {
        extraRequirements.add(requirement);
    }

    public List<Dependency> getExtraRequirements() {
        return extraRequirements;
    }

    public void setOptionalResolutionAction(String optionalAction) {
        this.optionalAction = optionalAction;
    }

    public String getOptionalResolutionAction() {
        return optionalAction;
    }

    public String getExecutionEnvironment() {
        return executionEnvironment;
    }

    public void setExecutionEnvironment(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }
}
