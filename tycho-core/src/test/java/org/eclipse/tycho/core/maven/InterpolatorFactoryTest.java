/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic and others.
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

import java.io.File;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InterpolatorFactoryTest {

    private InterpolatorFactory factory;
    private Settings settings;
    private Interpolator interpolator;
    private MavenProject project;

    @Before
    public void setUp() {
        factory = new InterpolatorFactory();
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
        expect(baseDir.getAbsolutePath()).andReturn("absolutePathToBaseDir");

        replay(session, project, settings, baseDir);

        interpolator = factory.createInterpolator(session, project);
    }

    @Test
    public void testProjectPropertiesGetInterpolated() throws InterpolationException {
        String interpolated = interpolator.interpolate("${myProjectPropertyKey}");
        Assert.assertEquals("myProjectPropertyValue", interpolated);
    }

    @Test
    public void testUserPropertiesGetInterpolated() throws InterpolationException {
        String interpolated = interpolator.interpolate("${myUserPropertyKey}");
        Assert.assertEquals("myUserPropertyValue", interpolated);
    }

    @Test
    public void testSystemPropertiesGetInterpolated() throws InterpolationException {
        String interpolated = interpolator.interpolate("${mySystemPropertyKey}");
        Assert.assertEquals("mySystemPropertyValue", interpolated);
    }

    @Test
    public void testLocalRepositoryGetInterpolated() throws InterpolationException {
        String interpolated = interpolator.interpolate("${localRepository}");
        Assert.assertEquals("myLocalRepo", interpolated);
    }

    @Test
    public void testBaseDirGetInterpolated() throws InterpolationException {
        String interpolated = interpolator.interpolate("${basedir}");
        Assert.assertEquals("absolutePathToBaseDir", interpolated);
    }

    @Test
    public void testProjectMembersGetInterpolated() throws InterpolationException {
        reset(project);
        expect(project.getArtifactId()).andReturn("myArtifactId");
        replay(project);
        String interpolated = interpolator.interpolate("${project.artifactId}");
        Assert.assertEquals("myArtifactId", interpolated);
    }

    @Test
    public void testSettingsMembersGetInterpolated() throws InterpolationException {
        reset(settings);
        expect(settings.getSourceLevel()).andReturn("mySourceLevel");
        replay(settings);
        String interpolated = interpolator.interpolate("${settings.sourceLevel}");
        Assert.assertEquals("mySourceLevel", interpolated);
    }
}
