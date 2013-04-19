/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.io.File;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Ignore;

@Ignore("maven-plugin-testing harness broken with maven 3.1-SNAPSHOT")
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

    public void testFailIfNonMatchingIdBundle() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/bundle");
        ValidateIdMojo mojo = getMojo(basedir);
        assertMojoExecutionExceptionThrown(mojo);
    }

    public void testFailIfNonMatchingIdTestPlugin() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/test-plugin");
        ValidateIdMojo mojo = getMojo(basedir);
        assertMojoExecutionExceptionThrown(mojo);
    }

    public void testFailIfNonMatchingIdFeature() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/nonMatchingIds/feature");
        ValidateIdMojo mojo = getMojo(basedir);
        assertMojoExecutionExceptionThrown(mojo);
    }

    private void assertMojoExecutionExceptionThrown(ValidateIdMojo mojo) throws MojoFailureException {
        try {
            mojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
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
