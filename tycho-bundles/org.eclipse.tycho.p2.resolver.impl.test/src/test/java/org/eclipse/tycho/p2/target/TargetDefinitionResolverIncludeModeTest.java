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

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.MAIN_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.OPTIONAL_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TARGET_FEATURE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.defaultEnvironments;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.util.resolution.ResolverException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TargetDefinitionResolverIncludeModeTest {

    @Rule
    public final P2Context p2Context = new P2Context();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private TargetDefinitionResolver subject;
    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    @Before
    public void initSubject() throws Exception {
        subject = new TargetDefinitionResolver(defaultEnvironments(),
                ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS, IncludeSourceMode.honor,
                new MockMavenContext(tempManager.newFolder("localRepo"), logVerifier.getLogger()), null);
    }

    @Test
    public void testResolveWithPlanner() throws Exception {
        TargetDefinition definition = definitionWith(
                new PlannerLocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test(expected = ResolverException.class)
    public void testUnsatisfiedDependencyWithPlannerFails() throws Exception {
        // ignore logged errors
        logVerifier.expectError(any(String.class));

        TargetDefinition definition = definitionWith(
                new PlannerLocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test(expected = ResolverException.class)
    public void testUnsatisfiedInclusionWithPlannerFails() throws Exception {
        // ignore logged errors
        logVerifier.expectError(any(String.class));

        TargetDefinition definition = definitionWith(
                new PlannerLocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test
    public void testResolveWithSlicer() throws Exception {
        TargetDefinition definition = definitionWith(
                new SlicerLocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1)));
    }

    @Test
    public void testUnsatisfiedDependencyWithSlicerIsOk() throws Exception {
        // TODO this missing dependency is not logged because Slicer is created
        // in SlicerResolutionStrategy with configuration to only consider strict dependencies
        // so it does not consider this dependency at all
        // expectWarningMissingDependency(MAIN_BUNDLE, REFERENCED_BUNDLE_V1);
        expectNoErrors();
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(MAIN_BUNDLE)));
    }

    @Test
    public void testUnsatisfiedInclusionWithSlicerIsOk() throws Exception {
        expectWarningMissingDependency(TARGET_FEATURE, REFERENCED_BUNDLE_V1);
        expectNoErrors();
        TargetDefinition definition = definitionWith(
                new SlicerLocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testResolveConflictingIncludeMode() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.V1, MAIN_BUNDLE),
                new PlannerLocationStub(TestRepositories.V2));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    private void expectWarningMissingDependency(IVersionedId from, IVersionedId to) {
        var msgFrom = "from " + from.getId() + " " + from.getVersion();
        var msgTo = "to org.eclipse.equinox.p2.iu; " + to.getId() + " [" + to.getVersion();
        logVerifier.expectWarning(allOf(containsString(msgFrom), containsString(msgTo)));
    }

    private void expectNoErrors() {
        logVerifier.expectError("");
    }

    static class PlannerLocationStub extends LocationStub implements InstallableUnitLocation {

        public PlannerLocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            super(repositories, seedUnits);
        }

        @Override
        public IncludeMode getIncludeMode() {
            return IncludeMode.PLANNER;
        }
    }

    static class SlicerLocationStub extends LocationStub implements InstallableUnitLocation {

        public SlicerLocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            super(repositories, seedUnits);
        }

        @Override
        public IncludeMode getIncludeMode() {
            return IncludeMode.SLICER;
        }
    }
}
