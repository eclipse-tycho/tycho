/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to API
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.target.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2.target.TestResolverFactory;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CustomEEResolutionHandlerTest {

    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private TargetPlatformFactory tpFactory;
    private TargetPlatformConfigurationStub tpConfig;

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private PomDependencyCollector pomDependencyCollector;

    @Before
    public void setUpContext() throws Exception {
        tpFactory = new TestResolverFactory(logVerifier.getLogger()).getTargetPlatformFactory();
        tpConfig = new TargetPlatformConfigurationStub();
        pomDependencyCollector = new PomDependencyCollectorImpl(
                new MavenContextImpl(tempManager.newFolder("localRepo"), logVerifier.getLogger()),
                new ReactorProjectStub(tempManager.newFolder(), "test"));
    }

    @Test
    public void testReadFullSpecificationFromTargetPlatform() throws Exception {
        ExecutionEnvironmentConfigurationCapture eeConfigurationCapture = new ExecutionEnvironmentConfigurationCapture(
                "Custom/Profile-2");
        tpConfig.addP2Repository(ResourceUtil.resourceFile("repositories/custom-profile").toURI());

        /*
         * Test CustomEEResolutionHandler in integration: Check that the CustomEEResolutionHandler
         * (which is wrapped around the eeConfigurationCapture argument by the called method)
         * correctly reads the custom profile specification from the target platform.
         */
        tpFactory.createTargetPlatform(tpConfig, eeConfigurationCapture, null);

        List<SystemCapability> result = eeConfigurationCapture.capturedSystemCapabilities;

        assertThat(result, not(nullValue()));
        assertThat(result, hasItem(new SystemCapability(Type.JAVA_PACKAGE, "javax.activation", "0.0.0")));
        assertThat(result, hasItem(new SystemCapability(Type.JAVA_PACKAGE, "javax.activation", "1.1.1")));
        assertThat(result, hasItem(new SystemCapability(Type.OSGI_EE, "OSGi/Minimum", "1.0.0")));
        assertThat(result, hasItem(new SystemCapability(Type.OSGI_EE, "JavaSE", "1.4.0")));
        assertThat(result, hasItem(new SystemCapability(Type.OSGI_EE, "JavaSE", "1.5.0")));
        assertThat(result.size(), is(5));
    }

    @Test
    public void testMissingSpecificationInTargetPlatform() throws Exception {
        ExecutionEnvironmentConfigurationCapture eeConfigurationCapture = new ExecutionEnvironmentConfigurationCapture(
                "MissingProfile-1.2.3");

        Exception e = assertThrows(Exception.class,
                () -> tpFactory.createTargetPlatform(tpConfig, eeConfigurationCapture, null));
        assertTrue(e.getMessage().contains(
                "Could not find specification for custom execution environment profile 'MissingProfile-1.2.3'"));
    }

    static class ExecutionEnvironmentConfigurationCapture implements ExecutionEnvironmentConfiguration {

        private final String profileName;
        List<SystemCapability> capturedSystemCapabilities;

        ExecutionEnvironmentConfigurationCapture(String profileName) {
            this.profileName = profileName;
        }

        @Override
        public String getProfileName() {
            return profileName;
        }

        @Override
        public boolean isCustomProfile() {
            return true;
        }

        @Override
        public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities) {
            capturedSystemCapabilities = systemCapabilities;
        }

        @Override
        public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
            // not needed
        }

        @Override
        public void setProfileConfiguration(String profileName, String configurationOrigin) {
            // not needed
        }

        @Override
        public ExecutionEnvironment getFullSpecification() {
            // not needed
            return null;
        }

        @Override
        public boolean isIgnoredByResolver() {
            return false;
        }

        @Override
        public Collection<ExecutionEnvironment> getAllKnownEEs() {
            return Collections.emptyList();
        }

        @Override
        public boolean ignoreExecutionEnvironment() {
            return false;
        }

    }
}
