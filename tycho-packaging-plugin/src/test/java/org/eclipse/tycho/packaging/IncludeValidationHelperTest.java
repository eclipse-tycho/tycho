/*******************************************************************************
 * Copyright (c) 2012, 2018 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.BuildPropertiesImpl;
import org.eclipse.tycho.testing.TestUtil;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public class IncludeValidationHelperTest {

    private IncludeValidationHelper subject = new IncludeValidationHelper(NOPLogger.NOP_LOGGER);

    @Test
    public void testCheckSourceIncludesExistAntPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("src.includes", "foo.txt, bar*,**/*.me");
        subject.checkSourceIncludesExist(createMockProject(), buildProperties, true);
    }

    @Test
    public void testCheckBinIncludesExistAntPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes", "foo.txt, bar*,**/*.me");
        subject.checkBinIncludesExist(createMockProject(), buildProperties, true);
    }

    @Test
    public void testCheckBinIncludesDontExist() throws Exception {
		BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes", "foo2.txt, bar2*,**/*.me");
		MojoExecutionException e = assertThrows(MojoExecutionException.class,
				() -> subject.checkBinIncludesExist(createMockProject(), buildProperties, true));
		assertStringContains("bin.includes value(s) [foo2.txt, bar2*] do not match any files.", e.getMessage());
	}

    @Test
    public void testCheckBinIncludesNotSpecified() throws Exception {
		BuildPropertiesImpl buildProperties = createBuildProperties("no.bin.includes", "bin.includes is not specified");
		MojoExecutionException e = assertThrows(MojoExecutionException.class,
				() -> subject.checkBinIncludesExist(createMockProject(), buildProperties, true));
		assertStringContains("bin.includes value(s) must be specified", e.getMessage());
    }

    @Test
    public void testCheckSourceIncludesDontExist() throws Exception {
		BuildPropertiesImpl buildProperties = createBuildProperties("src.includes", "foo3, bar3*,**/*.me");
		MojoExecutionException e = assertThrows(MojoExecutionException.class,
				() -> subject.checkSourceIncludesExist(createMockProject(), buildProperties, true));
		assertStringContains("src.includes value(s) [foo3, bar3*] do not match any files.", e.getMessage());
    }

    @Test
    public void testCheckBinIncludesExistWithIgnoredPatterns() throws Exception {
        BuildPropertiesImpl buildProperties = createBuildProperties("bin.includes",
                "foo.txt, bar*,**/*.me,TO_BE_IGNORED");
        subject.checkBinIncludesExist(createMockProject(), buildProperties, true, "TO_BE_IGNORED");
    }

    @Test
    public void testWarning() throws Exception {
        final List<String> warnings = new ArrayList<>();

        Logger log = mock(Logger.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            warnings.add(args[0].toString());
            return null;
        }).when(log).warn(anyString(), any(Throwable.class));
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            warnings.add(args[0].toString());
            return null;
        }).when(log).warn(anyString());

        IncludeValidationHelper subject = new IncludeValidationHelper(log);

        BuildPropertiesImpl buildProperties = createBuildProperties("src.includes", "foo3, bar3*,**/*.me");
        MavenProject project = createMockProject();
        subject.checkSourceIncludesExist(project, buildProperties, false);

        assertEquals(1, warnings.size());
        assertEquals(new File(project.getBasedir(), "build.properties").getAbsolutePath()
                + ": src.includes value(s) [foo3, bar3*] do not match any files.", warnings.get(0));
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
