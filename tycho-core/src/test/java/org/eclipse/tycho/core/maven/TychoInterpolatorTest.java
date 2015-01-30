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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        MavenSession session = createMock(MavenSession.class);
        project = createMock(MavenProject.class);
        settings = createMock(Settings.class);
        File baseDir = createMock(File.class);

        Properties projectProperties = new Properties();
        projectProperties.put("myProjectPropertyKey", "myProjectPropertyValue");

        Properties userProperties = new Properties();
        userProperties.put("myUserPropertyKey", "myUserPropertyValue");

        Properties systemProperties = new Properties();
        systemProperties.put("mySystemPropertyKey", "mySystemPropertyValue");

        expect(project.getProperties()).andReturn(projectProperties);
        expect(session.getSystemProperties()).andReturn(systemProperties);
        expect(session.getUserProperties()).andReturn(userProperties);
        expect(session.getSettings()).andReturn(settings);
        expect(settings.getLocalRepository()).andReturn("myLocalRepo");
        expect(project.getBasedir()).andReturn(baseDir);
        expect(project.getVersion()).andReturn("1.0.0");
        expect(baseDir.getAbsolutePath()).andReturn("absolutePathToBaseDir");

        replay(session, project, settings, baseDir);

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
        reset(project);
        expect(project.getArtifactId()).andReturn("myArtifactId");
        replay(project);
        String interpolated = interpolator.interpolate("${project.artifactId}");
        Assert.assertEquals("myArtifactId", interpolated);
    }

    @Test
    public void testSettingsMembersGetInterpolated() {
        reset(settings);
        expect(settings.getSourceLevel()).andReturn("mySourceLevel");
        replay(settings);
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
