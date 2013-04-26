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
package org.eclipse.tycho.plugins.p2.director;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.plugins.p2.director.EnvironmentSpecificConfigurations.Entry;
import org.junit.Test;

public class EnvironmentSpecificConfigurationsTest {

    private static final EnvironmentSpecificConfiguration GLOBAL_CONFIG = createConfig("globalValue");
    private static final EnvironmentSpecificConfiguration SPECIFIC_CONFIG = createConfig("specificValue");

    private static final TargetEnvironment LINUX_GTK_64 = new TargetEnvironment("linux", "gtk", "x86_64");

    private static final EnvironmentSpecificConfiguration.Parameter TEST_PARAMETER = EnvironmentSpecificConfiguration.Parameter.PROFILE_NAME;

    private EnvironmentSpecificConfigurations subject;

    @Test
    public void testGlobalConfigOnly() {
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, emptyList());

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("globalValue"));
    }

    @Test
    public void testMatchingSpecificConfiguration() {
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, asList(new Entry("linux.gtk.x86_64",
                SPECIFIC_CONFIG)));

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("specificValue"));
    }

    @Test
    public void testNonMatchingSpecificConfiguration() {
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, asList(new Entry("linux.motif.x86_64",
                SPECIFIC_CONFIG)));

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("globalValue"));
    }

    @Test
    public void testLastMatchingConfigurationWins() {
        List<Entry> specificConfigs = asList( //
                new Entry("linux.gtk", createConfig("firstSpecific")), //
                new Entry("linux.gtk", createConfig("secondSpecific")));
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, specificConfigs);

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("secondSpecific"));
    }

    @Test
    public void testMostSpecificConfigurationWins() {
        List<Entry> specificConfigs = asList( //
                new Entry("linux.gtk", createConfig("firstTwoMatches")), //
                new Entry("linux.any.x86_64", createConfig("secondTwoMatches")), //
                new Entry("linux", createConfig("oneMatch")));
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, specificConfigs);

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("secondTwoMatches"));
    }

    @Test
    public void testNullValuesNeverWin() {
        List<Entry> specificConfigs = asList( //
                new Entry("linux", createConfig("firstOneMatch")), //
                new Entry("linux.gtk", createConfig(null)), //
                new Entry("any.gtk", createConfig("secondOneMatch")));
        subject = new EnvironmentSpecificConfigurations(GLOBAL_CONFIG, specificConfigs);

        assertThat(subject.getEffectiveValue(TEST_PARAMETER, LINUX_GTK_64), is("secondOneMatch"));
    }

    private static EnvironmentSpecificConfiguration createConfig(String value) {
        // just assign value to the TEST_FIELD parameter
        return new EnvironmentSpecificConfiguration(value, null);
    }

    private static List<Entry> emptyList() {
        return Collections.<Entry> emptyList();
    }

}
