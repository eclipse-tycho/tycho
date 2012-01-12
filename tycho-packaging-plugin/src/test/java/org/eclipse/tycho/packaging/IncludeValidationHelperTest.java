/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.testing.TestUtil;
import org.junit.Test;

public class IncludeValidationHelperTest {

    @Test
    public void testCheckSourceIncludesExistAntPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("src.includes", "foo.txt, bar*,**/*.me");
        IncludeValidationHelper.checkSourceIncludesExist(createMockProject(), buildProperties);
    }

    @Test
    public void testCheckBinIncludesExistAntPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes", "foo.txt, bar*,**/*.me");
        IncludeValidationHelper.checkBinIncludesExist(createMockProject(), buildProperties);
    }

    @Test
    public void testCheckBinIncludesDontExist() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes", "foo2.txt, bar2*,**/*.me");
        try {
            IncludeValidationHelper.checkBinIncludesExist(createMockProject(), buildProperties);
            fail();
        } catch (MojoExecutionException e) {
            assertStringContains("bin.includes value(s) [foo2.txt, bar2*] do not match any files.", e.getMessage());
        }
    }

    @Test
    public void testCheckSourceIncludesDontExist() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("src.includes", "foo3, bar3*,**/*.me");
        try {
            IncludeValidationHelper.checkSourceIncludesExist(createMockProject(), buildProperties);
            fail();
        } catch (MojoExecutionException e) {
            assertStringContains("src.includes value(s) [foo3, bar3*] do not match any files.", e.getMessage());
        }
    }

    @Test
    public void testCheckBinIncludesExistWithIgnoredPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes",
                "foo.txt, bar*,**/*.me,TO_BE_IGNORED");
        IncludeValidationHelper.checkBinIncludesExist(createMockProject(), buildProperties, "TO_BE_IGNORED");
    }

    private void assertStringContains(String expected, String actual) {
        assertTrue("String '" + expected + "' not found in '" + actual + "'", actual.contains(expected));
    }

    private MavenProject createMockProject() throws IOException {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File(TestUtil.getBasedir("projects/validationHelper/binInclude"), "pom.xml"));
        return mavenProject;
    }

    private BuildPropertiesImpl createBuildProperties(String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        BuildPropertiesImpl buildProperties = new BuildPropertiesImpl(properties);
        return buildProperties;
    }
}
