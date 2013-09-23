/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.manager;

import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;
import org.eclipse.tycho.test.util.MavenServiceStubbingTestBase;
import org.eclipse.tycho.test.util.ReactorProjectCoordinatesStub;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class ReactorRepositoryManagerTest extends MavenServiceStubbingTestBase {

    private ReactorRepositoryManagerFacade subject;

    @Test
    public void testReactorRepositoryManagerServiceAvailability() throws Exception {
        subject = getService(ReactorRepositoryManager.class);

        assertThat(subject, is(notNullValue()));
    }

    @Test
    public void testReactorRepositoryManagerFacadeServiceAvailability() throws Exception {
        subject = getService(ReactorRepositoryManagerFacade.class);

        assertThat(subject, is(notNullValue()));
    }

    @Test
    public void testTargetPlatformComputationInIntegration() throws Exception {
        subject = getService(ReactorRepositoryManagerFacade.class);

        ReactorProject currentProject = new ReactorProjectStub();

        TargetPlatformConfigurationStub tpConfig = new TargetPlatformConfigurationStub();
        tpConfig.addP2Repository(new MavenRepositoryLocation(null, ResourceUtil.resourceFile("repositories/launchers")
                .toURI()));
        subject.computePreliminaryTargetPlatform(currentProject, tpConfig, new ExecutionEnvironmentConfigurationStub(
                "JavaSE-1.7"), null, null);

        ReactorProjectCoordinates upstreamProject = new ReactorProjectCoordinatesStub(
                ResourceUtil.resourceFile("projectresult/target"));

        subject.computeFinalTargetPlatform(currentProject, Arrays.asList(upstreamProject));

        P2TargetPlatform finalTP = (P2TargetPlatform) currentProject
                .getContextValue("org.eclipse.tycho.core.TychoConstants/targetPlatform");
        Collection<IInstallableUnit> units = finalTP.getInstallableUnits();
        // units from the p2 repository
        assertThat(units, hasItem(unitWithId("org.eclipse.equinox.launcher")));
        // units from the upstream project
        assertThat(units, hasItem(unitWithId("bundle")));
        assertThat(units, hasItem(unitWithId("bundle.source")));
    }

    private <T> T getService(Class<T> type) throws Exception {
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(FrameworkUtil.getBundle(this.getClass())
                .getBundleContext(), type, null);
        tracker.open();
        try {
            return tracker.waitForService(2000);
        } finally {
            tracker.close();
        }
    }

    // TODO move into separate class? remove interface methods to simplify stubbing?
    public class ReactorProjectStub implements ReactorProject {

        private Map<String, Object> contextValues = new HashMap<String, Object>();

        public Object getContextValue(String key) {
            return contextValues.get(key);
        }

        public void setContextValue(String key, Object value) {
            contextValues.put(key, value);
        }

        // methods which are (currently) not needed
        public File getBasedir() {
            throw new UnsupportedOperationException();
        }

        public String getPackaging() {
            throw new UnsupportedOperationException();
        }

        public String getGroupId() {
            throw new UnsupportedOperationException();
        }

        public String getArtifactId() {
            throw new UnsupportedOperationException();
        }

        public String getVersion() {
            throw new UnsupportedOperationException();
        }

        public File getOutputDirectory() {
            throw new UnsupportedOperationException();
        }

        public BuildOutputDirectory getBuildDirectory() {
            throw new UnsupportedOperationException();
        }

        public File getTestOutputDirectory() {
            throw new UnsupportedOperationException();
        }

        public File getArtifact() {
            throw new UnsupportedOperationException();
        }

        public File getArtifact(String artifactClassifier) {
            throw new UnsupportedOperationException();
        }

        public void setDependencyMetadata(String classifier, boolean primary, Set<Object> installableUnits) {
            throw new UnsupportedOperationException();
        }

        public Set<Object> getDependencyMetadata(String classifier, boolean primary) {
            throw new UnsupportedOperationException();
        }

        public Map<String, Set<Object>> getDependencyMetadata() {
            throw new UnsupportedOperationException();
        }

        public String getExpandedVersion() {
            throw new UnsupportedOperationException();
        }

        public void setExpandedVersion(String originalVersion, String qualifier) {
            throw new UnsupportedOperationException();
        }

        public String getId() {
            throw new UnsupportedOperationException();
        }

        public boolean sameProject(Object otherProject) {
            throw new UnsupportedOperationException();
        }
    }

}
