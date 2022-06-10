/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.junit.Before;
import org.junit.Test;

public class ExecutionEnvironmentConfigurationTest {

    private static final String DUMMY_ORIGIN = null;
    private static final List<SystemCapability> DUMMY_CUSTOM_PROFILE_SPEC = Collections.emptyList();

    private static final String CUSTOM_PROFILE = "Custom-1.5";
    private static final String STANDARD_PROFILE = "OSGi/Minimum-1.1";
    private static final String OTHER_STANDARD_PROFILE = "OSGi/Minimum-1.2";

    ConsoleLogger logger = new ConsoleLogger(Logger.LEVEL_DISABLED, "no-op logger");

    ExecutionEnvironmentConfiguration subject;

    @Before
    public void initSubject() {
        subject = new ExecutionEnvironmentConfigurationImpl(logger, false, null, null);
    }

    @Test
    public void testDefaults() {
        int javaVersion = Runtime.version().feature();
        assertEquals("JavaSE-" + javaVersion, subject.getProfileName());
        assertFalse(subject.isCustomProfile());
        assertEquals("JavaSE-" + javaVersion, subject.getFullSpecification().getProfileName());
    }

    @Test
    public void testSetProfileConfiguration() {
        subject.setProfileConfiguration("P1", DUMMY_ORIGIN);
        subject.setProfileConfiguration("P2", DUMMY_ORIGIN);

        assertEquals("P2", subject.getProfileName());
    }

    @Test
    public void testOverrideProfileConfiguration() {
        subject.setProfileConfiguration("P1", DUMMY_ORIGIN);
        subject.overrideProfileConfiguration("P2", DUMMY_ORIGIN);
        subject.overrideProfileConfiguration("P3", DUMMY_ORIGIN);
        subject.setProfileConfiguration("P4", DUMMY_ORIGIN);

        assertEquals("P3", subject.getProfileName());
    }

    @Test
    public void testStandardProfile() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);

        assertFalse(subject.isCustomProfile());
        assertTrue(subject.getFullSpecification() instanceof StandardExecutionEnvironment);
        assertEquals(STANDARD_PROFILE, subject.getFullSpecification().getProfileName());
    }

    @Test
    public void testCustomProfile() {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);

        assertTrue(subject.isCustomProfile());

        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);

        assertTrue(subject.getFullSpecification() instanceof CustomExecutionEnvironment);
        assertEquals(CUSTOM_PROFILE, subject.getFullSpecification().getProfileName());
    }

    @Test(expected = BuildFailureException.class)
    public void testMustNotIgnoreEEWhenUsingCustomProfile() {
        subject = new ExecutionEnvironmentConfigurationImpl(logger, true, null, null);
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);

        subject.isCustomProfile();
    }

    // BEGIN fail fast if methods are called in unexpected order

    @Test(expected = IllegalStateException.class)
    public void disallowSetProfileConfigurationAfterGetters() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
        subject.setProfileConfiguration(OTHER_STANDARD_PROFILE, DUMMY_ORIGIN);
    }

    @Test(expected = IllegalStateException.class)
    public void disallowOverrideProfileConfigurationAfterGetters() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
        subject.overrideProfileConfiguration(OTHER_STANDARD_PROFILE, DUMMY_ORIGIN);
    }

    @Test(expected = IllegalStateException.class)
    public void disallowSetCustomProfileSpecificationForStandardProfiles() throws Exception {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);
    }

    @Test(expected = IllegalStateException.class)
    public void disallowMultipleSetCustomProfileSpecification() throws Exception {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);
        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);
        subject.setFullSpecificationForCustomProfile(List.of(new SystemCapability(Type.OSGI_EE, "dummy", "0.0.0")));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMissingFullSpecificationForCustomProfile() {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
    }
}
