/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to new API 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.test.util.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;
import org.eclipse.tycho.test.util.ReactorProjectIdentitiesStub;
import org.eclipse.tycho.test.util.ReactorProjectStub;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReactorRepositoryManagerTest extends MavenServiceStubbingTestBase {

    private ReactorRepositoryManager subject;
    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private PomDependencyCollector pomDependencyCollector;

    @Before
    public void setUpContext() throws Exception {
        pomDependencyCollector = new PomDependencyCollectorImpl(logVerifier.getLogger(),
                new ReactorProjectStub(tempManager.newFolder(), "test"), getProvisioningAgent());
    }

    @Test
    public void testReactorRepositoryManagerServiceAvailability() throws Exception {
        subject = lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }

    @Test
    public void testReactorRepositoryManagerFacadeServiceAvailability() throws Exception {
        subject = lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }

    @Test
    @Ignore("This test currently do no longer work with the mocked project...")
    public void testTargetPlatformComputationInIntegration() throws Exception {
        subject = lookup(ReactorRepositoryManager.class);
        assertNotNull(subject);
        ReactorProject currentProject = new ReactorProjectStub("reactor-artifact");

        TargetPlatformConfigurationStub tpConfig = new TargetPlatformConfigurationStub();
        tpConfig.addP2Repository(
                new MavenRepositoryLocation(null, ResourceUtil.resourceFile("repositories/launchers").toURI()));
        subject.computePreliminaryTargetPlatform(currentProject, tpConfig,
                new ExecutionEnvironmentConfigurationStub("JavaSE-1.7"), null);

        ReactorProjectIdentities upstreamProject = new ReactorProjectIdentitiesStub(
                ResourceUtil.resourceFile("projectresult"));

        subject.computeFinalTargetPlatform(currentProject, Arrays.asList(upstreamProject), pomDependencyCollector);

        P2TargetPlatform finalTP = (P2TargetPlatform) currentProject
                .getContextValue("org.eclipse.tycho.core.TychoConstants/targetPlatform");
        Collection<IInstallableUnit> units = finalTP.getInstallableUnits();
        // units from the p2 repository
        assertThat(units, hasItem(unitWithId("org.eclipse.equinox.launcher")));
        // units from the upstream project
        assertThat(units, hasItem(unitWithId("bundle")));
        assertThat(units, hasItem(unitWithId("bundle.source")));

        // TODO get artifact
    }

}
