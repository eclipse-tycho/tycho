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
package org.eclipse.tycho.core.osgitools;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.ReflectionUtils;
import org.easymock.EasyMock;
import org.eclipse.tycho.core.maven.InterpolatorFactory;
import org.eclipse.tycho.testing.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BuildPropertiesParserImplTest {

    private BuildPropertiesParserImpl parser;
    private InterpolatorFactory interpolatorFactory;
    private Interpolator interpolator;
    private LegacySupport legacySupport;
    private MavenSession mavenSession;
    private MavenProject project1;
    private MavenProject project2;

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException, InterpolationException {
        parser = new BuildPropertiesParserImpl();
        interpolatorFactory = createMock(InterpolatorFactory.class);
        interpolator = createMock(Interpolator.class);
        legacySupport = createMock(LegacySupport.class);
        mavenSession = createMock(MavenSession.class);
        project1 = createMock(MavenProject.class);
        project2 = createMock(MavenProject.class);

        expect(legacySupport.getSession()).andReturn(mavenSession);
        expect(mavenSession.getProjects()).andReturn(Arrays.asList(project1, project2)).anyTimes();

        expect(project1.getBasedir()).andReturn(new File("/bathToProject1")).anyTimes();
        expect(project2.getBasedir()).andReturn(new File("/bathToProject2")).anyTimes();

        replay(interpolatorFactory, legacySupport, mavenSession, project1, project2);

        setField("interpolatorFactory", interpolatorFactory);
        setField("legacySupport", legacySupport);
    }

    private void setField(String fieldName, Object value) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(fieldName, BuildPropertiesParserImpl.class);
        field.setAccessible(true);
        field.set(parser, value);
    }

    @Test
    public void testReadPropertiesFileWithExistingFile() throws IOException {
        File baseDir = TestUtil.getBasedir("buildproperties");
        Properties properties = BuildPropertiesParserImpl.readProperties(new File(baseDir, "build.properties"));
        Assert.assertEquals(3, properties.size());
    }

    @Test
    public void testReadPropertiesWithNonExistingFile() {
        Properties properties = BuildPropertiesParserImpl.readProperties(new File("MISSING_FILE"));
        Assert.assertEquals(0, properties.size());
    }

    @Test
    public void testInterpolateWithABaseDirThatsNotPartOfTheSessionsProjects() throws InterpolationException {
        parser.interpolate(new Properties(), new File("/bathToSomeProjectThatsNotPartOfTheSessionProjects"));
    }

    @Test
    public void testCreateInterpolatorWillBeCalledWithCorrectProject() {
        reset(interpolatorFactory);
        expect(interpolatorFactory.createInterpolator(mavenSession, project1)).andReturn(interpolator);
        replay(interpolatorFactory);
        parser.interpolate(new Properties(), new File("/bathToProject1"));
    }

    @Test
    public void testInterpolateGetsCalledForEachProperty() throws InterpolationException {
        reset(interpolatorFactory, interpolator);
        expect(interpolatorFactory.createInterpolator(mavenSession, project1)).andReturn(interpolator);
        expect(interpolator.interpolate("${myValueToInterpolate1}")).andReturn("interpolated1");
        expect(interpolator.interpolate("${myValueToInterpolate2}")).andReturn("interpolated2");
        replay(interpolatorFactory, interpolator);

        Properties propertiesToInterpolate = new Properties();
        propertiesToInterpolate.put("myKey1", "${myValueToInterpolate1}");
        propertiesToInterpolate.put("myKey2", "${myValueToInterpolate2}");
        parser.interpolate(propertiesToInterpolate, new File("/bathToProject1"));

        Assert.assertEquals("interpolated1", propertiesToInterpolate.getProperty("myKey1"));
        Assert.assertEquals("interpolated2", propertiesToInterpolate.getProperty("myKey2"));
    }

    @Test
    public void testLoggerGetsCalledIfInterpolationExceptionOccured() throws InterpolationException,
            IllegalAccessException {

        Logger logger = createMock(Logger.class);
        setField("logger", logger);
        reset(interpolatorFactory, interpolator);
        logger.warn("Unable to interpolate the build property value :${myValueToInterpolate1}");
        EasyMock.expectLastCall();
        expect(interpolatorFactory.createInterpolator(mavenSession, project1)).andReturn(interpolator);
        expect(interpolator.interpolate("${myValueToInterpolate1}")).andThrow(
                new InterpolationException("message", "${myValueToInterpolate1}"));
        replay(interpolatorFactory, interpolator, logger);

        Properties propertiesToInterpolate = new Properties();
        propertiesToInterpolate.put("myKey1", "${myValueToInterpolate1}");
        parser.interpolate(propertiesToInterpolate, new File("/bathToProject1"));

    }
}
