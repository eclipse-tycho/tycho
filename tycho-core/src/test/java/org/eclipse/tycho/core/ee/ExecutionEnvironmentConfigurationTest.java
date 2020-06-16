/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.junit.Before;
import org.junit.Test;

public class ExecutionEnvironmentConfigurationTest {

    private static final String DUMMY_ORIGIN = null;
    private static final List<SystemCapability> DUMMY_CUSTOM_PROFILE_SPEC = Collections.<SystemCapability> emptyList();

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
        assertThat(subject.getProfileName(), is("JavaSE-11"));
        assertThat(subject.isCustomProfile(), is(false));
        assertThat(subject.getFullSpecification().getProfileName(), is("JavaSE-11"));
    }

    @Test
    public void testSetProfileConfiguration() {
        subject.setProfileConfiguration("P1", DUMMY_ORIGIN);
        subject.setProfileConfiguration("P2", DUMMY_ORIGIN);

        assertThat(subject.getProfileName(), is("P2"));
    }

    @Test
    public void testOverrideProfileConfiguration() {
        subject.setProfileConfiguration("P1", DUMMY_ORIGIN);
        subject.overrideProfileConfiguration("P2", DUMMY_ORIGIN);
        subject.overrideProfileConfiguration("P3", DUMMY_ORIGIN);
        subject.setProfileConfiguration("P4", DUMMY_ORIGIN);

        assertThat(subject.getProfileName(), is("P3"));
    }

    @Test
    public void testStandardProfile() {
        subject.setProfileConfiguration(STANDARD_PROFILE, DUMMY_ORIGIN);

        assertThat(subject.isCustomProfile(), is(false));
        assertThat(subject.getFullSpecification(), is(instanceOf(StandardExecutionEnvironment.class)));
        assertThat(subject.getFullSpecification().getProfileName(), is(STANDARD_PROFILE));
    }

    @Test
    public void testCustomProfile() {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);

        assertThat(subject.isCustomProfile(), is(true));

        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);

        assertThat(subject.getFullSpecification(), is(instanceOf(CustomExecutionEnvironment.class)));
        assertThat(subject.getFullSpecification().getProfileName(), is(CUSTOM_PROFILE));
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
        subject.setFullSpecificationForCustomProfile(DUMMY_CUSTOM_PROFILE_SPEC);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMissingFullSpecificationForCustomProfile() {
        subject.setProfileConfiguration(CUSTOM_PROFILE, DUMMY_ORIGIN);
        subject.getFullSpecification();
    }
}
