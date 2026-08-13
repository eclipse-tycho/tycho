/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.SystemCapability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecutionEnvironmentConfigurationTest {

    private static final String DUMMY_ORIGIN = null;
    private static final List<SystemCapability> DUMMY_CUSTOM_PROFILE_SPEC = Collections.<SystemCapability> emptyList();

    private static final String CUSTOM_PROFILE = "Custom-1.5";
    private static final String STANDARD_PROFILE = "OSGi/Minimum-1.1";
    private static final String OTHER_STANDARD_PROFILE = "OSGi/Minimum-1.2";

    ConsoleLogger logger = new ConsoleLogger(Logger.LEVEL_DISABLED, "no-op logger");

    ExecutionEnvironmentConfiguration subject;

    @BeforeEach
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

    @Test
    public void testMustNotIgnoreEEWhenUsingCustomProfile() {
        subject = new ExecutionEnvironmentConfigurationImpl(logger, true, null, null);
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);

        assertThrows(BuildFailureException.class, () -> subject.isCustomProfile());
    }

    // BEGIN fail fast if methods are called in unexpected order

    @Test
    public void disallowSetProfileConfigurationAfterGetters() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
        assertThrows(IllegalStateException.class,
                () -> subject.setProfileConfiguration(OTHER_STANDARD_PROFILE, DUMMY_ORIGIN));
    }

    @Test
    public void disallowOverrideProfileConfigurationAfterGetters() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
        assertThrows(IllegalStateException.class,
                () -> subject.overrideProfileConfiguration(OTHER_STANDARD_PROFILE, DUMMY_ORIGIN));
    }

    @Test
    public void disallowSetCustomProfileSpecificationForStandardProfiles() throws Exception {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);
        assertThrows(IllegalStateException.class,
                () -> subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC));
    }

    @Test
    public void disallowMultipleSetCustomProfileSpecification() throws Exception {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);
        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);
        assertThrows(IllegalStateException.class,
                () -> subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC));
    }

    @Test
    public void testGetMissingFullSpecificationForCustomProfile() {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);
        assertThrows(IllegalStateException.class, () -> subject.getFullSpecification());
    }
}
