/*******************************************************************************
 * Copyright (c) 2014, 2020 SAP SE and others.
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

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.defaultEnvironments;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MockMavenContext;
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
import org.junit.rules.TemporaryFolder;

public class TargetDefinitionResolverIncludeSourceTest {

    private static final IVersionedId BUNDLE_WITH_SOURCES = new VersionedId("simple.bundle", "1.0.0.201407081200");
    private static final IVersionedId SOURCE_BUNDLE = new VersionedId("simple.bundle.source", "1.0.0.201407081200");
    private static final IVersionedId NOSOURCE_BUNDLE = new VersionedId("nosource.bundle", "1.0.0");

    @Rule
    public final P2Context p2Context = new P2Context();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private TargetDefinitionResolver subject;

    @Before
    public void initSubject() throws Exception {
        subject = new TargetDefinitionResolver(defaultEnvironments(),
                ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS, IncludeSourceMode.honor,
                new MockMavenContext(tempManager.newFolder("localRepo"), logVerifier.getLogger()), null);
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeSourceLocations() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(null, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES),
                new WithoutSourceLocationStub(null, TestRepositories.SOURCES));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test
    public void testIncludeSourceWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(IncludeMode.SLICER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(2));
    }

    @Test
    public void testIncludeSourceWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(IncludeMode.PLANNER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(2));
    }

    @Test
    public void testNoSourceIncludeWhenIncludeSourceIsFalseWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithoutSourceLocationStub(IncludeMode.SLICER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), not(hasItem(SOURCE_BUNDLE)));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(1));
    }

    @Test
    public void testNoSourceIncludeWhenIncludeSourceIsFalseWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithoutSourceLocationStub(IncludeMode.PLANNER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), not(hasItem(SOURCE_BUNDLE)));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(1));
    }

    @Test
    public void testCanResolveBundlesWithoutSourcesWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(IncludeMode.SLICER,
                TestRepositories.SOURCES, BUNDLE_WITH_SOURCES, NOSOURCE_BUNDLE));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(3));
    }

    @Test
    public void testCanResolveBundlesWithoutSourcesWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(IncludeMode.PLANNER,
                TestRepositories.SOURCES, BUNDLE_WITH_SOURCES, NOSOURCE_BUNDLE));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition, p2Context.getAgent());

        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertThat(content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size(), is(3));
    }

    static class WithSourceLocationStub extends LocationStub implements InstallableUnitLocation {

        private IncludeMode includeMode;

        public WithSourceLocationStub(IncludeMode includeMode, TestRepositories repositories,
                IVersionedId... seedUnits) {
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
    }

    static class WithoutSourceLocationStub extends LocationStub implements InstallableUnitLocation {
        private IncludeMode includeMode;

        public WithoutSourceLocationStub(IncludeMode includeMode, TestRepositories repositories,
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
