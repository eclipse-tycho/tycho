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
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.defaultEnvironments;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TargetDefinitionResolverIncludeSourceTest extends TychoPlexusTestCase {

    private static final IVersionedId BUNDLE_WITH_SOURCES = new VersionedId("simple.bundle", "1.0.0.201407081200");
    private static final IVersionedId SOURCE_BUNDLE = new VersionedId("simple.bundle.source", "1.0.0.201407081200");
    private static final IVersionedId NOSOURCE_BUNDLE = new VersionedId("nosource.bundle", "1.0.0");

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private TargetDefinitionResolver subject;

    @Before
    public void initSubject() throws Exception {
        MavenContext mavenCtx = new MockMavenContext(tempManager.newFolder("localRepo"), logVerifier.getLogger());
        subject = new TargetDefinitionResolver(defaultEnvironments(),
                ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS, IncludeSourceMode.honor,
                ReferencedRepositoryMode.ignore, mavenCtx, null,
                new DefaultTargetDefinitionVariableResolver(logVerifier.getLogger(), mavenCtx, null));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testConflictingIncludeSourceLocations() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(null, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES),
                new WithoutSourceLocationStub(null, TestRepositories.SOURCES));
        subject.resolveContentWithExceptions(definition, lookup(IProvisioningAgent.class));
    }

    @Test
    public void testIncludeSourceWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(IncludeMode.SLICER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertEquals(2, getResultSet(content).size());
    }

    @Test
    public void testIncludeSourceWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithSourceLocationStub(IncludeMode.PLANNER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertEquals(2, getResultSet(content).size());
    }

    private Set<IInstallableUnit> getResultSet(TargetDefinitionContent content) {
        return content.query(QueryUtil.ALL_UNITS, null).stream()
                .filter(iu -> !iu.getId().startsWith("generated.target.category.")).collect(Collectors.toSet());
    }

    @Test
    public void testNoSourceIncludeWhenIncludeSourceIsFalseWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithoutSourceLocationStub(IncludeMode.SLICER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), not(hasItem(SOURCE_BUNDLE)));
        assertEquals(1, getResultSet(content).size());
    }

    @Test
    public void testNoSourceIncludeWhenIncludeSourceIsFalseWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(
                new WithoutSourceLocationStub(IncludeMode.PLANNER, TestRepositories.SOURCES, BUNDLE_WITH_SOURCES));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), not(hasItem(SOURCE_BUNDLE)));
        assertEquals(1, getResultSet(content).size());
    }

    @Test
    public void testCanResolveBundlesWithoutSourcesWithSlicerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(IncludeMode.SLICER,
                TestRepositories.SOURCES, BUNDLE_WITH_SOURCES, NOSOURCE_BUNDLE));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertEquals(3, getResultSet(content).size());
    }

    @Test
    public void testCanResolveBundlesWithoutSourcesWithPlannerMode() throws Exception {
        TargetDefinition definition = definitionWith(new WithSourceLocationStub(IncludeMode.PLANNER,
                TestRepositories.SOURCES, BUNDLE_WITH_SOURCES, NOSOURCE_BUNDLE));

        TargetDefinitionContent content = subject.resolveContentWithExceptions(definition,
                lookup(IProvisioningAgent.class));

        assertThat(versionedIdsOf(content), hasItem(NOSOURCE_BUNDLE));
        assertThat(versionedIdsOf(content), hasItem(BUNDLE_WITH_SOURCES));
        assertThat(versionedIdsOf(content), hasItem(SOURCE_BUNDLE));
        assertEquals(3, getResultSet(content).size());
    }

    static class WithSourceLocationStub extends LocationStub {

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

    static class WithoutSourceLocationStub extends LocationStub {
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
