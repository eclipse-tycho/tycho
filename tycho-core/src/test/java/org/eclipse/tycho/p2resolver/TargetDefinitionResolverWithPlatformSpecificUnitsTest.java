/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.RepositoryStub;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.UnitStub;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinition.Repository;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TychoPlexusExtension.class)
public class TargetDefinitionResolverWithPlatformSpecificUnitsTest {

    @Inject
    protected PlexusContainer container;
    private static final IVersionedId LAUNCHER_FEATURE = new VersionedId("org.eclipse.equinox.executable.feature.group",
            "3.3.101.R34x_v20081125-7H-ELfE8hXnkE15Wh9Tnyu");
    private static final IVersionedId LAUNCHER_FEATURE_JAR = new VersionedId(
            "org.eclipse.equinox.executable.feature.jar", "3.3.101.R34x_v20081125-7H-ELfE8hXnkE15Wh9Tnyu");
    private static final IVersionedId LAUNCHER_BUNDLE = new VersionedId("org.eclipse.equinox.launcher",
            "1.0.101.R34x_v20081125");
    private static final IVersionedId LAUNCHER_BUNDLE_LINUX = new VersionedId(
            "org.eclipse.equinox.launcher.gtk.linux.x86_64", "1.0.101.R34x_v20080731");
    private static final IVersionedId LAUNCHER_BUNDLE_WINDOWS = new VersionedId(
            "org.eclipse.equinox.launcher.win32.win32.x86", "1.0.101.R34x_v20080731");
    private static final IVersionedId LAUNCHER_BUNDLE_MAC = new VersionedId(
            "org.eclipse.equinox.launcher.carbon.macosx", "1.0.101.R34x_v20080731");

    private static TargetDefinition targetDefinition;

    @TempDir
    Path tempManager;
    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    private TargetDefinitionResolver subject;

    @Test
    public void testResolutionWithGenericPlatform() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE)));
    }

    @Test
    public void testPlannerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units), bagEquals(
                versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testPlannerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("linux", "gtk", "x86_64"),
                new TargetEnvironment("win32", "win32", "x86"), new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(environments);

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units), bagEquals(
                versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testSlicerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("win32", "win32", "x86"),
                new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(environments);

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithIncludeAllEnvironments() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("gtk", "linux", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, container.lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testConflictingIncludeAllEnvironments() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true),
                new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, false));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        assertThrows(TargetDefinitionResolutionException.class, () -> subject.resolveContentWithExceptions(targetDefinition, container.lookup(IProvisioningAgent.class)));
    }

    private TargetDefinitionResolver createResolver(List<TargetEnvironment> environments)
            throws ProvisionException, IOException {
        MavenContext mavenCtx = new MockMavenContext(newFolder("localRepo"), logVerifier.getLogger());
        return new TargetDefinitionResolver(environments, ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS,
                IncludeSourceMode.honor, ReferencedRepositoryMode.ignore, mavenCtx, null,
                new DefaultTargetDefinitionVariableResolver(mavenCtx, logVerifier.getLogger()));
    }

    private static class FilterRepoLocationStubWithLauncherUnit implements TargetDefinition.InstallableUnitLocation {

        private final IncludeMode includeMode;
        private final boolean includeAllEnvironments;

        public FilterRepoLocationStubWithLauncherUnit(IncludeMode includeMode) {
            this(includeMode, false);
        }

        public FilterRepoLocationStubWithLauncherUnit(IncludeMode includeMode, boolean includeAllEnvironments) {
            this.includeMode = includeMode;
            this.includeAllEnvironments = includeAllEnvironments;
        }

        @Override
        public List<? extends Repository> getRepositories() {
            return Collections.singletonList(new RepositoryStub("with-filters"));
        }

        @Override
        public List<? extends Unit> getUnits() {
            return Collections.singletonList(new UnitStub(LAUNCHER_FEATURE));
        }

        @Override
        public String getTypeDescription() {
            return null;
        }

        @Override
        public IncludeMode getIncludeMode() {
            return includeMode;
        }

        @Override
        public boolean includeAllEnvironments() {
            return includeAllEnvironments;
        }

        @Override
        public boolean includeSource() {
            return false;
        }

        @Override
        public boolean includeConfigurePhase() {
            return false;
        }
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempManager.resolve(path)).toFile();
    }
}
