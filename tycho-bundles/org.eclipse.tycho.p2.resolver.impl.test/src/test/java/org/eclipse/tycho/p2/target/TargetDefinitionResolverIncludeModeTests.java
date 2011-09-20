/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.MAIN_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.OPTIONAL_BUNDLE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.REFERENCED_BUNDLE_V1;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TARGET_FEATURE;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.bagEquals;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdList;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TargetDefinitionResolverIncludeModeTests {

    @Rule
    public P2Context p2Context = new P2Context();

    private TargetDefinitionResolver subject;

    @Before
    public void initContext() throws Exception {
        Map<String, String> emptyMap = new HashMap<String, String>();
        List<Map<String, String>> environments = Collections.singletonList(emptyMap);

        subject = new TargetDefinitionResolver(environments, p2Context.getAgent(), new MavenLoggerStub());
    }

    @Test
    public void testResolveWithPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new PlannerLocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnsatisfiedDependencyWithPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new PlannerLocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        Map<String, String> emptyMap = new HashMap<String, String>();
        List<Map<String, String>> environments = Collections.singletonList(emptyMap);
        subject = new TargetDefinitionResolver(environments, p2Context.getAgent(), new MavenLoggerStub(false, false));
        subject.resolveContent(definition);
    }

    @Test
    public void testResolveWithSlicer() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1)));
    }

    @Test
    public void testUnsatisfiedDependencyWithSlicerIsOk() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(MAIN_BUNDLE)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnsatisfiedInclusionWithSlicerFails() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.UNSATISFIED,
                TARGET_FEATURE));
        subject.resolveContent(definition);
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testResolveConflictingIncludeMode() throws Exception {
        TargetDefinition definition = definitionWith(new SlicerLocationStub(TestRepositories.V1, MAIN_BUNDLE),
                new PlannerLocationStub(TestRepositories.V2));
        subject.resolveContent(definition);
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
