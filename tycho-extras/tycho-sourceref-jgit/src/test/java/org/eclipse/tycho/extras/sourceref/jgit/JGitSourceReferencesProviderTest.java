/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP AG and others.
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

package org.eclipse.tycho.extras.sourceref.jgit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.packaging.sourceref.ScmUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class JGitSourceReferencesProviderTest {

    @Test
    public void testGetRelativePathWithDots() throws MojoExecutionException {
        JGitSourceReferencesProvider provider = new JGitSourceReferencesProvider();
        File projectBasedir = new File("/foo/../test/bar/baz");
        File repoRoot = new File("/test/foo/../bar");
        assertEquals("baz", provider.getRelativePath(projectBasedir, repoRoot));
    }

    @Test
    public void testGetRelativePathWithSpaces() throws MojoExecutionException {
        JGitSourceReferencesProvider provider = new JGitSourceReferencesProvider();
        File projectBasedir = new File("/foo/test me/bar/baz boo");
        File repoRoot = new File("/foo/test me/");
        assertEquals("bar/baz boo", provider.getRelativePath(projectBasedir, repoRoot));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testGetRelativePathMixedCaseOnWindows() throws MojoExecutionException {
        JGitSourceReferencesProvider provider = new JGitSourceReferencesProvider();
        File projectBasedir = new File("C:/bar/baz/test/me");
        File repoRoot = new File("c:/bar/baz");
        assertEquals("test/me", provider.getRelativePath(projectBasedir, repoRoot));
    }

    @Test
    public void testGetRelativePathNoCommonBasedir() throws MojoExecutionException {
        JGitSourceReferencesProvider provider = new JGitSourceReferencesProvider();
        File projectBasedir = new File("/foo/test/bar");
        File repoRoot = new File("/baz");
        assertThrows(MojoExecutionException.class, () -> provider.getRelativePath(projectBasedir, repoRoot));
    }

    @Test
    public void testGetSourceReferencesHeader() throws MojoExecutionException {
        JGitSourceReferencesProvider provider = new JGitSourceReferencesProvider();
        MavenProject mockProject = new MavenProject();
        ScmUrl scmUrl = new ScmUrl(properties("scm:git:foo"));
        mockProject.setFile(new File("src/test/resources/pom.xml").getAbsoluteFile());
        String sourceRef = provider.getSourceReferencesHeader(mockProject, scmUrl);
        assertTrue(sourceRef.startsWith("scm:git:foo;path=\"tycho-extras/tycho-sourceref-jgit/src/test/resources\""));
    }

    private Properties properties(String scmUrl) {
        Properties p = new Properties();
        p.setProperty("tycho.scmUrl", scmUrl);
        return p;
    }

}
