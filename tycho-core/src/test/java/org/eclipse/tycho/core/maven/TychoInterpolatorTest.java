/*******************************************************************************
 * Copyright (c) 2014, 2015 Bachmann electronic and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bachmann electronic - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TychoInterpolatorTest {

    private Settings settings;
    private TychoInterpolator interpolator;
    private MavenProject project;

    @Before
    public void setUp() {
        MavenSession session = mock(MavenSession.class);
        project = mock(MavenProject.class);
        settings = mock(Settings.class);
        File baseDir = mock(File.class);

        Properties projectProperties = new Properties();
        projectProperties.put("myProjectPropertyKey", "myProjectPropertyValue");

        Properties userProperties = new Properties();
        userProperties.put("myUserPropertyKey", "myUserPropertyValue");

        Properties systemProperties = new Properties();
        systemProperties.put("mySystemPropertyKey", "mySystemPropertyValue");

        when(project.getProperties()).thenReturn(projectProperties);
        when(session.getSystemProperties()).thenReturn(systemProperties);
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getSettings()).thenReturn(settings);
        when(settings.getLocalRepository()).thenReturn("myLocalRepo");
        when(project.getBasedir()).thenReturn(baseDir);
        when(project.getVersion()).thenReturn("1.0.0");
        when(baseDir.getAbsolutePath()).thenReturn("absolutePathToBaseDir");

        interpolator = new TychoInterpolator(session, project);
    }

    @Test
    public void testProjectPropertiesGetInterpolated() {
        String interpolated = interpolator.interpolate("${myProjectPropertyKey}");
        Assert.assertEquals("myProjectPropertyValue", interpolated);
    }

    @Test
    public void testUserPropertiesGetInterpolated() {
        String interpolated = interpolator.interpolate("${myUserPropertyKey}");
        Assert.assertEquals("myUserPropertyValue", interpolated);
    }

    @Test
    public void testSystemPropertiesGetInterpolated() {
        String interpolated = interpolator.interpolate("${mySystemPropertyKey}");
        Assert.assertEquals("mySystemPropertyValue", interpolated);
    }

    @Test
    public void testLocalRepositoryGetInterpolated() {
        String interpolated = interpolator.interpolate("${localRepository}");
        Assert.assertEquals("myLocalRepo", interpolated);
    }

    @Test
    public void testBaseDirGetInterpolated() {
        String interpolated = interpolator.interpolate("${basedir}");
        Assert.assertEquals("absolutePathToBaseDir", interpolated);
    }

    @Test
    public void testVersionGetInterpolated() {
        String interpolated = interpolator.interpolate("${version}");
        Assert.assertEquals("1.0.0", interpolated);
    }

    @Test
    public void testProjectMembersGetInterpolated() {
        when(project.getArtifactId()).thenReturn("myArtifactId");
        String interpolated = interpolator.interpolate("${project.artifactId}");
        Assert.assertEquals("myArtifactId", interpolated);
    }

    @Test
    public void testSettingsMembersGetInterpolated() {
        when(settings.getSourceLevel()).thenReturn("mySourceLevel");
        String interpolated = interpolator.interpolate("${settings.sourceLevel}");
        Assert.assertEquals("mySourceLevel", interpolated);
    }

    @Test
    public void testInterpolateSubString() {
        assertThat(interpolator.interpolate("pre1${localRepository}1post"), is("pre1myLocalRepo1post"));
    }

    @Test
    public void testInterpolateNonExisting() {
        assertThat(interpolator.interpolate("${undefined}"), is("${undefined}"));
    }

    @Test
    public void testInterpolateSyntaxError() {
        assertThat(interpolator.interpolate("${not closed"), is("${not closed"));
    }

}
