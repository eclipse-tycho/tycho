/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.defaultEnvironments;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.core.resolver.shared.IncludeSourcesMode;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TargetDefinitionResolverIncludeSourceTest {

    private static final IVersionedId BUNDLE_WITH_SOURCES = new VersionedId("simple.bundle", "1.0.0.201407081200");
    private static final IVersionedId SOURCE_BUNDLE = new VersionedId("simple.bundle.source", "1.0.0.201407081200");
    private static final IVersionedId NOSOURCE_BUNDLE = new VersionedId("nosource.bundle", "1.0.0");

    @Rule
    public final P2Context p2Context = new P2Context();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private TargetDefinitionResolver subject;

    @Before
    public void initSubject() throws Exception {
        subject = new TargetDefinitionResolver(defaultEnvironments(),
                ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS, p2Context.getAgent(), logVerifier.getLogger());
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeSourceLocations() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.SLICER, BUNDLE_WITH_SOURCES), new WithoutSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.SLICER));
        subject.resolveContentWithExceptions(definition);
    }

    @Test
    public void testResultContainsSourceBundleSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.SLICER, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(2));
    }

    @Test
    public void testResultContainsSourceBundlePlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(IncludeMode.PLANNER,
                TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(2));
    }

    @Test
    public void testCanResolveBundlesWithoutSources() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.SLICER, NOSOURCE_BUNDLE));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition);
        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(1));
    }

    @Test
    public void testIgnoreIncludeSource() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.SLICER, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, IncludeSourcesMode.ignore);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(content.getUnits().size(), is(1));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeSourceLocationsPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER, BUNDLE_WITH_SOURCES), new WithoutSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER));
        subject.resolveContentWithExceptions(definition);
    }

    @Test
    public void testResultContainsSourceBundlePlanner() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(2));
    }

    @Test
    public void testCanResolveBundlesWithoutSourcesPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER, NOSOURCE_BUNDLE));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition);
        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(1));
    }

    @Test
    public void testIgnoreIncludeSourcePlanner() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, IncludeSourcesMode.ignore);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(content.getUnits().size(), is(1));
    }

    @Test
    public void testForceIncludeSourcePlanner() throws Exception {
        TargetDefinition definition = definitionWith(new WithoutSourceLocationStub(TestRepositories.SOURCES,
                IncludeMode.PLANNER, BUNDLE_WITH_SOURCES));
        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, IncludeSourcesMode.force);
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.getUnits().size(), is(2));
    }

    static class WithSourceLocationStub extends LocationStub implements InstallableUnitLocation {

        private IncludeMode includeMode;

        public WithSourceLocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            this(IncludeMode.SLICER, repositories, seedUnits);
        }

        public WithSourceLocationStub(IncludeMode includeMode, TestRepositories repositories, IVersionedId... seedUnits) {
            super(repositories, seedUnits);
            this.includeMode = includeMode;
        }

        @Override
        public IncludeMode getIncludeMode() {
            return includeMode;
        }

        @Override
        public boolean includeSource() {
            return true;
        }

        @Override
        public IncludeMode getIncludeMode() {
            return this.includeMode;
        }
    }

    static class WithoutSourceLocationStub extends LocationStub implements InstallableUnitLocation {
        private IncludeMode includeMode;

        public WithoutSourceLocationStub(TestRepositories repositories, IncludeMode includeMode,
                IVersionedId... seedUnits) {
            super(repositories, seedUnits);
            this.includeMode = includeMode;
        }

        @Override
        public boolean includeSource() {
            return false;
        }

        @Override
        public IncludeMode getIncludeMode() {
            return this.includeMode;
        }
    }
}
