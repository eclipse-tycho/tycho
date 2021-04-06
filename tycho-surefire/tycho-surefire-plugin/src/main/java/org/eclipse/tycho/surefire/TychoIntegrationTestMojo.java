/*******************************************************************************
 * Copyright (c) 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;

@Mojo(name = "integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class TychoIntegrationTestMojo extends TestMojo {

    /**
     * The directory containing generated test classes of the project being tested.
     */
    @Parameter(property = "project.build.testOutputDirectory")
    private File testClassesDirectory;

    @Parameter(property = "skipITs")
    private boolean skipITs;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true)
    private File summaryFile;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports", required = true)
    private File reportDirectory;

    @Override
    protected boolean shouldRun() {
        return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging()) && scanForTests().size() > 0;
    }

    @Override
    protected List<String> getDefaultInclude() {
        return Arrays.asList("**/PluginTest*.class", "**/*IT.class");
    }

    @Override
    protected File getTestClassesDirectory() {
        return testClassesDirectory;
    }

    @Override
    protected File getReportsDirectory() {
        return reportDirectory;
    }

    @Override
    protected boolean shouldSkip() {
        return skipITs || super.shouldSkip();
    }

    @Override
    protected PropertiesWrapper createSurefireProperties(TestFrameworkProvider provider) throws MojoExecutionException {

        PropertiesWrapper properties = super.createSurefireProperties(provider);
        properties.setProperty("failifnotests", String.valueOf(false));
        properties.setProperty("failsafe", summaryFile.getAbsolutePath());
        return properties;
    }

    @Override
    protected void handleNoTestsFound() throws MojoFailureException {
        getLog().info("No tests found");
    }

    @Override
    protected void handleSuccess() {
        //nothing to do will be handled in verify phase
    }

    @Override
    protected void handleTestFailures() throws MojoFailureException {
        //nothing to do will be handled in verify phase
    }
}
