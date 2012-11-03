/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.RepositoryStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.UnitStub;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;

public class TargetDefinitionResolverWithPlatformSpecificUnitsTest {
    private static final IVersionedId LAUNCHER_FEATURE = new VersionedId(
            "org.eclipse.equinox.executable.feature.group", "3.3.101.R34x_v20081125-7H-ELfE8hXnkE15Wh9Tnyu");
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
    public P2Context p2Context = new P2Context();

    private MavenLoggerStub logger = new MavenLoggerStub();
    private TargetDefinitionResolver subject;

    @Test
    public void testResolutionWithGenericPlatform() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE)));
    }

    @Test
    public void testPlannerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(Collections.singletonList(environment));

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(
                versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE,
                        LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testPlannerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("linux", "gtk", "x86_64"),
                new TargetEnvironment("win32", "win32", "x86"), new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.PLANNER));
        subject = createResolver(environments);

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(
                versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE,
                        LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithOnePlatform() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("linux", "gtk", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(Collections.singletonList(environment));

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(
                versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE,
                        LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testSlicerResolutionWithMultiplePlatforms() throws Exception {
        List<TargetEnvironment> environments = Arrays.asList(new TargetEnvironment("win32", "win32", "x86"),
                new TargetEnvironment("macosx", "carbon", "x86"));
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER));
        subject = createResolver(environments);

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(
                versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE,
                        LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test
    public void testSlicerResolutionWithIncludeAllEnvironments() throws Exception {
        TargetEnvironment environment = new TargetEnvironment("gtk", "linux", "x86_64");
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true));
        subject = createResolver(Collections.singletonList(environment));

        TargetPlatformContent units = subject.resolveContent(targetDefinition);

        assertThat(
                versionedIdsOf(units),
                bagEquals(versionedIdList(LAUNCHER_FEATURE, LAUNCHER_FEATURE_JAR, LAUNCHER_BUNDLE,
                        LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS, LAUNCHER_BUNDLE_MAC)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeAllEnvironments() throws Exception {
        targetDefinition = definitionWith(new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, true),
                new FilterRepoLocationStubWithLauncherUnit(IncludeMode.SLICER, false));
        subject = createResolver(Collections.singletonList(new TargetEnvironment(null, null, null)));

        subject.resolveContent(targetDefinition);
    }

    private TargetDefinitionResolver createResolver(List<TargetEnvironment> environments) throws ProvisionException {
        return new TargetDefinitionResolver(environments, new NoopEEResolverHints(), p2Context.getAgent(), logger);
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

        public List<? extends Repository> getRepositories() {
            return Collections.singletonList(new RepositoryStub("with-filters"));
        }

        public List<? extends Unit> getUnits() {
            return Collections.singletonList(new UnitStub(LAUNCHER_FEATURE));
        }

        public String getTypeDescription() {
            return null;
        }

        public IncludeMode getIncludeMode() {
            return includeMode;
        }

        public boolean includeAllEnvironments() {
            return includeAllEnvironments;
        }
    }

}
