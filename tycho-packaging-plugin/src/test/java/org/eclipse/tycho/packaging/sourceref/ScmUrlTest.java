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

package org.eclipse.tycho.packaging.sourceref;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

public class ScmUrlTest {

    @Test
    public void testCVSUrl() throws MojoExecutionException {
        ScmUrl cvsUrl = new ScmUrl(
                properties("scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.debug.core;tag=v20100427"));
        assertEquals("cvs", cvsUrl.getType());
    }

    @Test
    public void testGitUrl() throws MojoExecutionException {
        String url = "scm:git:git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.git;path=\"bundles/org.eclipse.releng.tools\";tag=v20111215-1442";
        ScmUrl gitUrl = new ScmUrl(properties(url));
        assertEquals("git", gitUrl.getType());
        assertEquals(url, gitUrl.getUrl());
    }

    @Test
    public void testPipeUrl() throws MojoExecutionException {
        String url = "scm:git|git://git.eclipse.org/gitroot/platform/";
        ScmUrl gitUrl = new ScmUrl(properties(url));
        assertEquals("git", gitUrl.getType());
        assertEquals(url, gitUrl.getUrl());
    }

    @Test(expected = MojoExecutionException.class)
    public void testInvalidUrl() throws MojoExecutionException {
        String gitScmUrl = "git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.git;path=\"bundles/org.eclipse.releng.tools\";tag=v20111215-1442";
        new ScmUrl(properties(gitScmUrl));
    }

    private static Properties properties(String scmUrl) {
        Properties p = new Properties();
        p.setProperty("tycho.scmUrl", scmUrl);
        return p;
    }
}
