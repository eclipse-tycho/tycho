/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - port to surefire 2.10
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 *    Bachmann electrontic GmbH - 510425 parallel mode requires threadCount>1 or useUnlimitedThreads=true
 *    Christoph Läubrich - improve error message in case of failures
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.PackagingType;

/**
 * <p>
 * Executes tests in an OSGi runtime.
 * </p>
 * <p>
 * The goal launches an OSGi runtime and executes the project's tests in that runtime. The "test
 * runtime" consists of the bundle built in this project and its transitive dependencies, plus some
 * Equinox and test harness bundles. The bundles are resolved from the target platform of the
 * project. Note that the test runtime does typically <em>not</em> contain the entire target
 * platform. If there are implicitly required bundles (e.g. <tt>org.apache.felix.scr</tt> to make
 * declarative services work), they need to be added manually through an <tt>extraRequirements</tt>
 * configuration on the <tt>target-platform-configuration</tt> plugin.
 * </p>
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class TestPluginMojo extends AbstractTestMojo {

    /**
     * The directory containing generated test classes of the project being tested.
     */
    @Parameter(property = "project.build.outputDirectory")
    private File testClassesDirectory;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
    protected File reportsDirectory;

    /**
     * If set to "false" the test execution will not fail in case there are no tests found.
     */
    @Parameter(property = "failIfNoTests", defaultValue = "true")
    private boolean failIfNoTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    private boolean testFailureIgnore;

    @Override
    protected boolean shouldRun() {
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging())) {
            return false;
        }
        if (!PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging())) {
            getLog().warn("Unsupported packaging type " + project.getPackaging());
            return false;
        }
        return true;
    }

    @Override
    protected List<String> getDefaultInclude() {
        return Arrays.asList("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class");
    }

    @Override
    protected File getTestClassesDirectory() {
        return testClassesDirectory;
    }

    @Override
    protected File getReportsDirectory() {
        return reportsDirectory;
    }

    @Override
    protected void handleNoTestsFound() throws MojoFailureException {
        String message = "No tests found.";
        if (failIfNoTests) {
            throw new MojoFailureException(message);
        } else {
            getLog().warn(message);
        }

    }

    @Override
    protected void handleSuccess() {
        getLog().info("All tests passed!");
    }

    @Override
    protected void handleTestFailures() throws MojoFailureException {
        String errorMessage = "There are test failures.\n\nPlease refer to " + reportsDirectory
                + " for the individual test results.";
        if (testFailureIgnore) {
            getLog().error(errorMessage);
        } else {
            throw new MojoFailureException(errorMessage);
        }

    }

}
