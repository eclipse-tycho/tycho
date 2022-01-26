/*******************************************************************************
 * Copyright (c) 2014, 2021 Bachmann electronic and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bachmann electronic - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
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
    private Logger logger;

    @Before
    public void setup() throws IllegalArgumentException, IllegalAccessException {
        legacySupport = mock(LegacySupport.class);
        mavenSession = mock(MavenSession.class);
        project1 = mock(MavenProject.class);
        project2 = mock(MavenProject.class);
        logger = mock(Logger.class);

        when(legacySupport.getSession()).thenReturn(mavenSession);
        when(mavenSession.getProjects()).thenReturn(Arrays.asList(project1, project2));

        when(project1.getBasedir()).thenReturn(new File("/bathToProject1"));
        when(project2.getBasedir()).thenReturn(new File("/bathToProject2"));

        parser = new BuildPropertiesParserImpl(legacySupport, logger);
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
    public void testInterpolateWithEmptyProperties() {
        parser.interpolate(new Properties(), null);
    }

}
