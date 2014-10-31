/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.maven;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.tycho.core.maven.MavenInterpolationService;
import org.eclipse.tycho.core.shared.InterpolationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenInterpolationServiceTest {

    private InterpolationService service;

    @Before
    public void setup() throws Exception {
        service = new MavenInterpolationService();
        setLegacySupport();
    }

    @Test
    public void testSystemPropertyGetsInterpolated() {
        String interpolated = service.interpolate("someText/${systemKey1}/SomeMoreText");
        Assert.assertEquals("someText/SystemValue1/SomeMoreText", interpolated);
    }

    @Test
    public void testUserPropertyGetsInterpolated() {
        String interpolated = service.interpolate("someText/${userKey1}/SomeMoreText");
        Assert.assertEquals("someText/UserValue1/SomeMoreText", interpolated);
    }

    @Test
    public void testProjectPropertyGetsInterpolated() {
        String interpolated = service.interpolate("someText/${projectKey1}/SomeMoreText");
        Assert.assertEquals("someText/ProjectValue1/SomeMoreText", interpolated);
    }

    @Test
    public void testLocalRepositoryGetsInterpolated() {
        String interpolated = service.interpolate("${localRepository}");
        Assert.assertEquals("pathToLocalRepo", interpolated);
    }

    @Test
    public void testBaseDirGetsInterpolated() {
        String interpolated = service.interpolate("${basedir}");
        Assert.assertEquals(new File("C:/pathToBaseDir").getAbsolutePath(), interpolated);
    }

    @Test
    public void testProjectFieldGetsInterpolated() {
        String interpolated = service.interpolate("${project.version}");
        Assert.assertEquals("1.0", interpolated);
    }

    @Test
    public void testSettingsFieldGetsInterpolated() {
        String interpolated = service.interpolate("${settings.modelEncoding}");
        Assert.assertEquals("myEncoding", interpolated);
    }

    @Test
    public void testNonExistingFieldGetsInterpolated() {
        String interpolated = service.interpolate("${settings.notExistingField}");
        Assert.assertEquals("${settings.notExistingField}", interpolated);
    }

    private LegacySupport createMockSupport() {
        LegacySupport support = createMock(LegacySupport.class);
        MavenSession session = createMock(MavenSession.class);
        MavenProject project = createMock(MavenProject.class);
        Settings settings = createMock(Settings.class);

        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();
        Properties projectProperties = new Properties();

        expect(support.getSession()).andReturn(session);
        expect(session.getSystemProperties()).andReturn(systemProperties);
        expect(session.getUserProperties()).andReturn(userProperties);
        expect(session.getCurrentProject()).andReturn(project);
        expect(project.getProperties()).andReturn(projectProperties);
        expect(session.getSettings()).andReturn(settings);
        expect(settings.getLocalRepository()).andReturn("pathToLocalRepo");
        expect(project.getBasedir()).andReturn(new File("C:/pathToBaseDir"));
        expect(project.getVersion()).andReturn("1.0");
        expect(settings.getModelEncoding()).andReturn("myEncoding");

        systemProperties.put("systemKey1", "SystemValue1");
        userProperties.put("userKey1", "UserValue1");
        projectProperties.put("projectKey1", "ProjectValue1");

        replay(support, session, project, settings);

        return support;
    }

    private void setLegacySupport() throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses("legacySupport",
                MavenInterpolationService.class);
        field.setAccessible(true);
        field.set(service, createMockSupport());
    }

}
