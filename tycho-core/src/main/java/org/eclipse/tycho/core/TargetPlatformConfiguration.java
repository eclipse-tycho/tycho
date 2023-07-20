/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter;

public class TargetPlatformConfiguration implements DependencyResolverConfiguration {

    public enum BREEHeaderSelectionPolicy {
        first, minimal
    }

    public enum LocalArtifactHandling {
        /**
         * local artifacts are included and override items from the target
         */
        include,
        /**
         * NOT SUPPORTED YET! local artifacts override items from the target that are
         * <i>equivalent</i> (major and minor version must equal the version from the target).
         */
        equivalent,
        /**
         * NOT SUPPORTED YET! local artifacts override items from the target that are
         * <i>compatible</i> (major version must equal the version from the target).
         */
        compatible,
        /**
         * NOT SUPPORTED YET! local artifacts override items from the target that match
         * <i>exactly</i> the version from the target.
         */
        perfect,
        /**
         * local artifacts are ignored
         */
        ignore;
    }

    public enum InjectP2MavenMetadataHandling {
        /**
         * ignores P2 maven metadata
         */
        ignore,
        /**
         * inject if found in P2 metadata
         */
        inject,
        /**
         * inject if found in P2 metadata, but validate if it is actually valid for the current
         * build
         */
        validate;
    }

    private String resolver;

    private List<TargetEnvironment> environments = new ArrayList<>();

    private boolean implicitTargetEnvironment = true;

    private final List<URI> targets = new ArrayList<>();
    private IncludeSourceMode targetDefinitionIncludeSourceMode = IncludeSourceMode.honor;

    private PomDependencies pomDependencies;

    private String executionEnvironment;
    private String executionEnvironmentDefault;
    private BREEHeaderSelectionPolicy breeHeaderSelectionPolicy = BREEHeaderSelectionPolicy.first;
    private boolean resolveWithEEConstraints = true;

    private List<TargetPlatformFilter> filters;

    private OptionalResolutionAction optionalAction = OptionalResolutionAction.REQUIRE;

    private final List<ArtifactKey> extraRequirements = new ArrayList<>();
    private final Set<String> exclusions = new HashSet<>();

    private Map<String, String> resolverProfileProperties = new HashMap<>();

    List<Supplier<File>> lazyTargetFiles = new ArrayList<>();

    private LocalArtifactHandling localArtifactHandling;

    private boolean requireEagerResolve;

    private InjectP2MavenMetadataHandling p2MavenMetadataHandling;

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

    public synchronized List<TargetDefinitionFile> getTargets() {
        for (Iterator<Supplier<File>> iterator = lazyTargetFiles.iterator(); iterator.hasNext();) {
            Supplier<File> supplier = iterator.next();
            targets.add(supplier.get().toURI());
            iterator.remove();
        }
        return targets.stream().map(TargetDefinitionFile::read).toList();
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

    public synchronized void addLazyTargetFile(Supplier<File> targetFileSupplier) {
        lazyTargetFiles.add(targetFileSupplier);
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
        if (pomDependencies == null) {
            if (isRequireEagerResolve()) {
                return PomDependencies.ignore;
            } else {
                return PomDependencies.consider;
            }
        }
        return pomDependencies;
    }

    public boolean isImplicitTargetEnvironment() {
        return implicitTargetEnvironment;
    }

    public void setImplicitTargetEnvironment(boolean implicitTargetEnvironment) {
        this.implicitTargetEnvironment = implicitTargetEnvironment;
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

    public boolean isRequireEagerResolve() {
        return requireEagerResolve;
    }

    public void setRequireEagerResolve(boolean value) {
        this.requireEagerResolve = value;
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
    public List<ArtifactKey> getAdditionalArtifacts() {
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

    @Override
    public Collection<IRequirement> getAdditionalRequirements() {
        return List.of();
    }

    public LocalArtifactHandling getIgnoreLocalArtifacts() {
        if (localArtifactHandling == null) {
            return LocalArtifactHandling.include;
        }
        return localArtifactHandling;
    }

    public InjectP2MavenMetadataHandling getP2MetadataHandling() {
        if (p2MavenMetadataHandling == null) {
            return InjectP2MavenMetadataHandling.validate;
        }
        return p2MavenMetadataHandling;
    }

    public void setP2MavenMetadataHandling(InjectP2MavenMetadataHandling p2MavenMetadataHandling) {
        this.p2MavenMetadataHandling = p2MavenMetadataHandling;
    }

    public void setLocalArtifactHandling(LocalArtifactHandling localArtifactHandling) {
        this.localArtifactHandling = localArtifactHandling;
    }

}
