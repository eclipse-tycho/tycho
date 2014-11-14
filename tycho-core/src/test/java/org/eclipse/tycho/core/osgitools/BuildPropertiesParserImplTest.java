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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.tycho.testing.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BuildPropertiesParserImplTest {

    private BuildPropertiesParserImpl parser;
    private LegacySupport legacySupport;
    private MavenSession mavenSession;
    private MavenProject project1;
    private MavenProject project2;

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException {
        parser = new BuildPropertiesParserImpl();

        legacySupport = createMock(LegacySupport.class);
        mavenSession = createMock(MavenSession.class);
        project1 = createMock(MavenProject.class);
        project2 = createMock(MavenProject.class);

        expect(legacySupport.getSession()).andReturn(mavenSession);
        expect(mavenSession.getProjects()).andReturn(Arrays.asList(project1, project2)).anyTimes();

        expect(project1.getBasedir()).andReturn(new File("/bathToProject1")).anyTimes();
        expect(project2.getBasedir()).andReturn(new File("/bathToProject2")).anyTimes();

        replay(legacySupport, mavenSession, project1, project2);

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
    public void testInterpolateWithABaseDirThatsNotPartOfTheSessionsProjects() {
        parser.interpolate(new Properties(), new File("/bathToSomeProjectThatsNotPartOfTheSessionProjects"));
    }

}
