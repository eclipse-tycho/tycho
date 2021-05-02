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
import org.apache.maven.surefire.util.ScanResult;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;

/**
 * <p>
 * Executes integration-tests in an OSGi runtime.
 * </p>
 * <p>
 * The goal launches an OSGi runtime and executes the project's integration-tests (default patterns
 * are <code>PluginTest*.class, *IT.class</code>) in that runtime. The "test runtime" consists of
 * the bundle built in this project and its transitive dependencies, plus some Equinox and test
 * harness bundles. The bundles are resolved from the target platform of the project. Note that the
 * test runtime does typically <em>not</em> contain the entire target platform. If there are
 * implicitly required bundles (e.g. <tt>org.apache.felix.scr</tt> to make declarative services
 * work), they need to be added manually through an <tt>extraRequirements</tt> configuration on the
 * <tt>target-platform-configuration</tt> plugin.
 * </p>
 * <p>
 * This goal adopts the maven-failsafe paradigm, that works in the following way:
 * <ol>
 * <li><code>pre-integration-test</code> phase could be used to prepare any prerequisite (e.g.
 * starting web-server, files, ...)</li>
 * <li><code>integration-test</code> phase does not fail the build if there are test failures but a
 * summary file is written</li>
 * <li><code>post-integration-test</code> could be used to cleanup/tear down any resources from the
 * <code>pre-integration-test</code> phase
 * <li>test outcome is checked in the <code>verify</code> phase that might fail the build if there
 * are test failures
 * </ol>
 * </p>
 * summary files are generated according to the default maven-surefire-plugin for integration with
 * tools that already work with maven-surefire-plugin (e.g. CI servers)
 */
@Mojo(name = "integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class TychoIntegrationTestMojo extends AbstractTestMojo {

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
    protected PropertiesWrapper createSurefireProperties(TestFrameworkProvider provider, ScanResult scanResult)
            throws MojoExecutionException {
        PropertiesWrapper properties = super.createSurefireProperties(provider, scanResult);
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
