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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.junit.Test;

public class TargetEnvironmentFilterTest {

    private static final TargetEnvironment LINUX_GTK_64 = new TargetEnvironment("linux", "gtk", "x86_64");

    private TargetEnvironmentFilter subject;

    @Test
    public void testNonMatchingOsFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("win32");
        assertThat(subject.matches(LINUX_GTK_64), is(false));
    }

    @Test
    public void testMatchingOsFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux");
        assertThat(subject.matches(LINUX_GTK_64), is(true));
    }

    @Test
    public void testNonMatchingOsWsFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.motif");
        assertThat(subject.matches(LINUX_GTK_64), is(false));
    }

    @Test
    public void testMatchingOsWsFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.gtk");
        assertThat(subject.matches(LINUX_GTK_64), is(true));
    }

    @Test
    public void testNonMatchingOsWsArchFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.gtk.x86");
        assertThat(subject.matches(LINUX_GTK_64), is(false));
    }

    @Test
    public void testMatchingOsWsArchFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.gtk.x86_64");
        assertThat(subject.matches(LINUX_GTK_64), is(true));
    }

    @Test
    public void testMatchingWsArchFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("any.gtk.x86_64");
        assertThat(subject.matches(LINUX_GTK_64), is(true));
    }

    @Test
    public void testMatchingOsArchFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.any.x86_64");
        assertThat(subject.matches(LINUX_GTK_64), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOsWsFilterWithTrailingAny() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.gtk.any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOsFilterWithTrailingAny() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.any");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFilterWithTooManySegments() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("one.two.three.four");
    }

    @Test
    public void testFilteredFieldCountOfOsWsArchFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("linux.gtk.x86_64");

        assertThat(subject.filteredFieldCount(), is(3));
    }

    @Test
    public void testFilteredFieldCountOfWsFilter() {
        subject = TargetEnvironmentFilter.parseConfigurationElement("any.gtk");

        assertThat(subject.filteredFieldCount(), is(1));
    }

}
