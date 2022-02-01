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
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.RepositoryStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.UnitStub;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TargetDefinitionResolverWithPlatformSpecificUnitsTest {
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

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public final P2Context p2Context = new P2Context();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private TargetDefinitionResolver subject;

    @Test
    public void testResolutionWithGenericPlatform() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE)));
    }

    @Test
    public void testPlannerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units), bagEquals(
                versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testPlannerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("linux", "gtk", "x86_64"),
                new TargetEnvironment("win32", "win32", "x86"), new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(environments);

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units), bagEquals(
                versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testSlicerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("win32", "win32", "x86"),
                new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(environments);

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithIncludeAllEnvironments() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("gtk", "linux", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true));
        subject = createResolver(Collections.singletonList(environment));

        TargetDefinitionContent units = subject.resolveContent(targetDefinition, p2Context.getAgent());

        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR,
                LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeAllEnvironments() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true),
                new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, false));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        subject.resolveContentWithExceptions(targetDefinition, p2Context.getAgent());
    }

    private TargetDefinitionResolver createResolver(List<TargetEnvironment> environments)
            throws ProvisionException, IOException {
        return new TargetDefinitionResolver(environments, ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS,
                IncludeSourceMode.honor,
                new MockMavenContext(tempManager.newFolder("localRepo"), logVerifier.getLogger()), null);
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
    }

}
