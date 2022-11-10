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

import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.MAIN_BUNDLE;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.OPTIONAL_BUNDLE;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.TARGET_FEATURE;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.defaultEnvironments;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.target.TargetDefinitionContent;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TargetDefinitionResolverIncludeModeTest extends TychoPlexusTestCase {

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
        TargetDefinitionContent units = subject.resolveContent(definition, lookup(IProvisioningAgent.class));
        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test(expected = ResolverException.class)
    public void testUnsatisfiedDependencyWithPlannerFails() throws Exception {
        // ignore logged errors
        logVerifier.expectError(any(String.class));

        TargetDefinition definition = definitionWith(
                new PlannerLocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        subject.resolveContentWithExceptions(definition, lookup(IProvisioningAgent.class));
    }

    @Test(expected = ResolverException.class)
    public void testUnsatisfiedInclusionWithPlannerFails() throws Exception {
        // ignore logged errors
        logVerifier.expectError(any(String.class));

        TargetDefinition definition = definitionWith(
                new PlannerLocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE));
        subject.resolveContentWithExceptions(definition, lookup(IProvisioningAgent.class));
    }

    @Test
    public void testResolveWithSlicer() throws Exception {
        TargetDefinition definition = definitionWith(
                new SlicerLocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetDefinitionContent units = subject.resolveContent(definition, lookup(IProvisioningAgent.class));
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
        TargetDefinitionContent units = subject.resolveContent(definition, lookup(IProvisioningAgent.class));
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(MAIN_BUNDLE)));
    }

    @Test
    public void testUnsatisfiedInclusionWithSlicerIsOk() throws Exception {
        expectWarningMissingDependency(TARGET_FEATURE, REFERENCED_BUNDLE_V1);
        expectNoErrors();
        TargetDefinition definition = definitionWith(
                new SlicerLocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE));
        subject.resolveContentWithExceptions(definition, lookup(IProvisioningAgent.class));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testResolveConflictingIncludeMode() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.V1, MAIN_BUNDLE),
                new PlannerLocationStub(TestRepositories.V2));
        subject.resolveContentWithExceptions(definition, lookup(IProvisioningAgent.class));
    }

    private void expectWarningMissingDependency(IVersionedId from, IVersionedId to) {
        var msgFrom = "from " + from.getId() + " " + from.getVersion();
        var msgTo = "to org.eclipse.equinox.p2.iu; " + to.getId() + " [" + to.getVersion();
        logVerifier.expectWarning(allOf(containsString(msgFrom), containsString(msgTo)));
    }

    private void expectNoErrors() {
        logVerifier.expectError("");
    }

    static class PlannerLocationStub extends LocationStub {

        public PlannerLocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            super(repositories, seedUnits);
        }

        @Override
        public IncludeMode getIncludeMode() {
            return IncludeMode.PLANNER;
        }
    }

    static class SlicerLocationStub extends LocationStub {

        public SlicerLocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            super(repositories, seedUnits);
        }

        @Override
        public IncludeMode getIncludeMode() {
            return IncludeMode.SLICER;
        }
    }
}
