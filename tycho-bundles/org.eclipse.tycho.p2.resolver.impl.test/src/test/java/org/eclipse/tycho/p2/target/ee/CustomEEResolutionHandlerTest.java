/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.target.TestResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomEEResolutionHandlerTest {

    @Rule
    public ExpectedException thrownException = ExpectedException.none();
    @Rule
    public LogVerifier logVerifier = new LogVerifier();

    private TargetPlatformFactory tpFactory;
    private TargetPlatformConfigurationStub tpConfig;

    @Before
    public void setUpContext() throws Exception {
        tpFactory = new TestResolverFactory(logVerifier.getLogger()).getTargetPlatformFactory();
        tpConfig = new TargetPlatformConfigurationStub();
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
        tpFactory.createTargetPlatform(tpConfig, eeConfigurationCapture, null, null);

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

        thrownException
                .expectMessage("Could not find specification for custom execution environment profile 'MissingProfile-1.2.3'");
        tpFactory.createTargetPlatform(tpConfig, eeConfigurationCapture, null, null);
    }

    static class ExecutionEnvironmentConfigurationCapture implements ExecutionEnvironmentConfiguration {

        private final String profileName;
        List<SystemCapability> capturedSystemCapabilities;

        ExecutionEnvironmentConfigurationCapture(String profileName) {
            this.profileName = profileName;
        }

        public String getProfileName() {
            return profileName;
        }

        public boolean isCustomProfile() {
            return true;
        }

        public void setFullSpecificationForCustomProfile(List<SystemCapability> systemCapabilities) {
            capturedSystemCapabilities = systemCapabilities;
        }

        public void overrideProfileConfiguration(String profileName, String configurationOrigin) {
            // not needed
        }

        public void setProfileConfiguration(String profileName, String configurationOrigin) {
            // not needed
        }

        public ExecutionEnvironment getFullSpecification() {
            // not needed
            return null;
        }

    }
}
