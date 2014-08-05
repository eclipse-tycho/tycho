package org.eclipse.tycho.buildversion;

import java.io.File;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class ValidateIUTest extends AbstractTychoMojoTestCase {

    public void testFailMissingProperty() throws MojoExecutionException, Exception {
        File basedir = getBasedir("projects/iuValidator/missingProperty");
        ValidateIUMojo mojo = getMojo(basedir);
        assertMojoExecutionExceptionThrown(mojo);
    }

    private ValidateIUMojo getMojo(File basedir) throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);
        MavenProject project = result.getProject();
        ValidateIUMojo mojo = (ValidateIUMojo) lookupMojo("validate-iu", project.getFile());
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "packaging", project.getPackaging());
        return mojo;
    }

    private void assertMojoExecutionExceptionThrown(ValidateIUMojo mojo) throws MojoFailureException {
        try {
            mojo.execute();
            fail();
        } catch (MojoExecutionException e) {
            // expected
        }
    }
}
