/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - add support for IU type (428310)
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import static org.junit.Assert.assertThrows;

import java.io.File;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class ValidateIdTest extends AbstractTychoMojoTestCase {

    public void testValidateMatchingIdBundle() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/matchingIds/bundle");
        ValidateIdMojo mojo = getMojo(basedir);
        mojo.execute();
    }

    public void testValidateMatchingIdTestPlugin() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/matchingIds/test-plugin");
        ValidateIdMojo mojo = getMojo(basedir);
        mojo.execute();
    }

    public void testValidateMatchingIdFeature() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/matchingIds/feature");
        ValidateIdMojo mojo = getMojo(basedir);
        mojo.execute();
    }

    public void testValidateMatchingIdIU() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/matchingIds/iu");
        ValidateIdMojo mojo = getMojo(basedir);
        mojo.execute();
    }

    public void testFailIfNonMatchingIdBundle() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/bundle");
        ValidateIdMojo mojo = getMojo(basedir);
		assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    public void testFailIfNonMatchingIdTestPlugin() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/test-plugin");
        ValidateIdMojo mojo = getMojo(basedir);
		assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    public void testFailIfNonMatchingIdFeature() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/feature");
        ValidateIdMojo mojo = getMojo(basedir);
		assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    public void testFailIfNonMatchingIdIU() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/iu");
        ValidateIdMojo mojo = getMojo(basedir);
		assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    private ValidateIdMojo getMojo(File basedir) throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);
        MavenProject project = result.getProject();
        ValidateIdMojo mojo = (ValidateIdMojo) lookupMojo("validate-id", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "packaging", project.getPackaging());
        return mojo;
    }

}
