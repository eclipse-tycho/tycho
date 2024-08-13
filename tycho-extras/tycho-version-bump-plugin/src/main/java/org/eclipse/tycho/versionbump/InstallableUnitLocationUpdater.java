/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2resolver.TargetDefinitionVariableResolver;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.FollowRepositoryReferences;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

import de.pdark.decentxml.Element;

@Named
public class InstallableUnitLocationUpdater {

    @Inject
    private TargetDefinitionVariableResolver varResolver;

    public boolean update(Element iuLocation, UpdateTargetMojo context) throws MojoFailureException {
        TargetDefinitionFile parsedTarget = TargetDefinitionFile.read(context.getFileToBeUpdated());
        TargetPlatformConfigurationStub resolutionContext = new TargetPlatformConfigurationStub();
        resolutionContext.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
        resolutionContext.addTargetDefinition(new LatestVersionTarget(parsedTarget, varResolver));
        resolutionContext.setIgnoreLocalArtifacts(true);
        P2ResolutionResult result = context.createResolver().getTargetPlatformAsResolutionResult(resolutionContext,
                context.getExecutionEnvironment());

        Map<String, String> ius = new HashMap<>();
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            ius.put(entry.getId(), entry.getVersion());
        }
        boolean updated = false;
        List<Element> children = iuLocation.getChildren("unit");
        for (Element unit : children) {
            String id = unit.getAttributeValue("id");
            String version = ius.get(id);
            if (version != null) {
                String currentVersion = unit.getAttributeValue("version");
                if (version.equals(currentVersion)) {
                    context.getLog().debug("unit '" + id + "' is already up-to date");
                } else {
                    updated = true;
                    context.getLog()
                            .info("Update version of unit '" + id + "' from " + currentVersion + " > " + version);
                    unit.setAttribute("version", version);
                }
            } else {
                context.getLog().warn(
                        "Resolution result does not contain root installable unit '" + id + "' update is skipped!");
            }
        }
        return updated;
    }

    private static final class LatestVersionTarget implements TargetDefinition {

        private TargetDefinitionFile delegate;
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionTarget(TargetDefinitionFile delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends Location> getLocations() {
            return delegate.getLocations().stream().map(location -> {
                if (location instanceof InstallableUnitLocation iuLocation) {
                    return new LatestVersionLocation(iuLocation, varResolver);
                } else {
                    return location;
                }
            }).toList();
        }

        @Override
        public boolean hasIncludedBundles() {
            return delegate.hasIncludedBundles();
        }

        @Override
        public String getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public String getTargetEE() {
            return delegate.getTargetEE();
        }

    }

    private static final class LatestVersionLocation implements InstallableUnitLocation {

        private InstallableUnitLocation delegate;
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionLocation(InstallableUnitLocation delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return delegate.getRepositories().stream().map(repo -> {
                URI resolvedLocation = URI.create(varResolver.resolve(repo.getLocation()));
                return new ResolvedRepository(repo.getId(), resolvedLocation.toString());
            }).collect(toList());
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            return delegate.getUnits().stream().map(LatestVersionUnit::new).toList();
        }

        @Override
        public IncludeMode getIncludeMode() {
            return delegate.getIncludeMode();
        }

        @Override
        public boolean includeAllEnvironments() {
            return delegate.includeAllEnvironments();
        }

        @Override
        public boolean includeSource() {
            return delegate.includeSource();
        }

        @Override
        public boolean includeConfigurePhase() {
            return delegate.includeConfigurePhase();
        }

        @Override
        public FollowRepositoryReferences followRepositoryReferences() {
            return delegate.followRepositoryReferences();
        }

    }

    private static final class LatestVersionUnit implements TargetDefinition.Unit {

        private Unit delegate;

        public LatestVersionUnit(TargetDefinition.Unit delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getVersion() {
            return "0.0.0";
        }

    }

    private static final class ResolvedRepository implements TargetDefinition.Repository {

        private final String id;
        private final String uri;

        ResolvedRepository(String id, String uri) {
            this.id = id;
            this.uri = uri;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getLocation() {
            return uri;
        }

    }

}
