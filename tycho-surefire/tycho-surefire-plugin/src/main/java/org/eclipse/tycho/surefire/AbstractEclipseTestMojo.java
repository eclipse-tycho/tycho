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
 *    SAP SE - port to surefire 2.10
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 *    Bachmann electrontic GmbH - 510425 parallel mode requires threadCount>1 or useUnlimitedThreads=true
 *    Christoph Läubrich    - [Bug 529929] improve error message in case of failures
 *                          - [Bug 572420] Tycho-Surefire should be executable for eclipse-plugin package type
 *                          - [Issue 790] Support printing of bundle wirings in tycho-surefire-plugin
 *                          - [Issue 849] JAVA_HOME check is not OS independent
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.LaunchConfiguration;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReproducibleUtils;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.p2tools.RepositoryReferenceTool;
import org.eclipse.tycho.surefire.provider.impl.ProviderHelper;
import org.eclipse.tycho.surefire.provider.impl.ProviderSelection;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.eclipse.tycho.surefire.provisioning.ProvisionedInstallationBuilder;
import org.eclipse.tycho.surefire.provisioning.ProvisionedInstallationBuilderFactory;

public abstract class AbstractEclipseTestMojo extends AbstractTestMojo {

    private static final String[] UNIX_SIGNAL_NAMES = { "not a signal", // padding, signals start with 1
            "SIGHUP", "SIGINT", "SIGQUIT", "SIGILL", "SIGTRAP", "SIGABRT", "SIGBUS", "SIGFPE", "SIGKILL", "SIGUSR1",
            "SIGSEGV", "SIGUSR2", "SIGPIPE", "SIGALRM", "SIGTERM", "SIGSTKFLT", "SIGCHLD", "SIGCONT", "SIGSTOP",
            "SIGTSTP", "SIGTTIN", "SIGTTOU", "SIGURG", "SIGXCPU", "SIGXFSZ", "SIGVTALRM", "SIGPROF", "SIGWINCH",
            "SIGIO", "SIGPWR", "SIGSYS" };

    private static final ConcurrencyLock CONCURRENCY_LOCK = new ConcurrencyLock();

    /**
     * <a href=
     * "https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstancearea"
     * >OSGi data directory</a> (<code>osgi.instance.area</code>, aka the workspace) of the Equinox
     * runtime used to execute tests.
     */
    @Parameter(defaultValue = "${project.build.directory}/work/data/")
    private File osgiDataDirectory;

    /**
     * Whether to recursively delete the directory {@link #osgiDataDirectory} before running the
     * tests.
     */
    @Parameter(defaultValue = "true")
    private boolean deleteOsgiDataDirectory;

    /**
     * The directory where OSGi/PDE metadata files (e.g., <code>MANIFEST.MF</code>) are located.
     * Useful in case said metadata is changed at build time, using filtering.
     * <p>
     * Defaults to the project's base directory.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File metadataDirectory;

    /**
     * Set this parameter to suspend the test JVM waiting for a client to open a remote debug
     * session on the specified port. If further customization of JVM debug parameters is required
     * then {@link #argLine} can be used instead.
     */
    @Parameter(property = "debugPort")
    private int debugPort;

    /**
     * if set to a non-null value, the platform is put in debug mode. If the value is a non-empty
     * string it is interpreted as the location of the .options file. This file indicates what debug
     * points are available for a plug-in and whether or not they are enabled. for a list of
     * available options see <a href=
     * "https://git.eclipse.org/c/equinox/rt.equinox.framework.git/plain/bundles/org.eclipse.osgi/.options">https://git.eclipse.org/c/equinox/rt.equinox.framework.git/plain/bundles/org.eclipse.osgi/.options</a>
     */
    @Parameter(property = "osgi.debug")
    private String debugOptions;

    /**
     * (junit47 provider with JUnit4.8+ only) Groups/categories for this test (comma-separated).
     * Only classes/methods/etc decorated with one of the group/category specified here will be
     * included in test run, if specified. For JUnit, this parameter forces the use of the junit47
     * provider
     */
    @Parameter(property = "groups")
    private String groups;

    /**
     * (junit47 provider with JUnit4.8+ only) Excluded groups/categories (comma-separated). Any
     * methods/classes/etc with one of the groups/categories specified in this list will
     * specifically not be run. For JUnit, this parameter forces the use of the junit47 provider
     */
    @Parameter(property = "excludedGroups")
    private String excludedGroups;

    /**
     * Enables -consolelog for the test OSGi runtime
     */
    @Parameter(property = "tycho.showEclipseLog", defaultValue = "false")
    private boolean showEclipseLog;

    /**
     * prints all loaded bundles
     */
    @Parameter(property = "tycho.printWires", defaultValue = "false")
    private boolean printWires;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     */
    @Parameter(property = "maven.test.redirectTestOutputToFile", defaultValue = "false")
    private boolean redirectTestOutputToFile;

    @Parameter(defaultValue = "${project.build.directory}/surefire.properties")
    private File surefireProperties;

    /**
     * Eclipse application to be run. If not specified, default application
     * org.eclipse.ui.ide.workbench will be used. Application runnable will be invoked from test
     * harness, not directly from Eclipse.
     *
     * Note that you need to ensure that the bundle which defines the configured application is
     * included in the test runtime.
     */
    @Parameter
    private String application;

    /**
     * Eclipse product to be run, i.e. -product parameter passed to test Eclipse runtime.
     */
    @Parameter
    private String product;

    /**
     * Run tests using UI (true) or headless (false) test harness.
     */
    @Parameter(property = "tycho.surefire.useUIHarness", defaultValue = "false")
    private boolean useUIHarness;

    /**
     * Run tests in UI (true) or background (false) thread. Only applies to UI test harness.
     */
    @Parameter(property = "tycho.surefire.useUIThread", defaultValue = "true")
    private boolean useUIThread;

    /**
     * By default, Tycho Surefire disables JVM assertions for the execution of your test cases. To
     * enable the assertions, set this flag to "true".
     *
     * @since 1.5.0
     */
    @Parameter(property = "enableAssertions", defaultValue = "false")
    private boolean enableAssertions;

    /**
     * Arbitrary JVM options to set on the command line.
     */
    @Parameter(property = "tycho.testArgLine")
    private String argLine;

    /**
     * Arbitrary applications arguments to set on the command line.
     */
    @Parameter
    private String appArgLine;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     */
    @Parameter(property = "surefire.timeout")
    private int forkedProcessTimeoutInSeconds;

    /**
     * Additional environments to set for the forked test JVM.
     */
    @Parameter
    private Map<String, String> environmentVariables;

    /**
     * Additional system properties to set for the forked test JVM.
     */
    @Parameter
    private Map<String, String> systemProperties;

    /**
     * List of bundles that must be expanded in order to execute the tests. Ignored if
     * {@link #testRuntime} is <code>p2Installed</code>.
     */
    @Parameter
    private String[] explodedBundles;

    /**
     * List of framework extension bundles to add. Note: The goal does not automatically detect
     * which bundles in the test runtime are framework extensions, but they have to be explicitly
     * specified using this parameter. Ignored if {@link #testRuntime} is <code>p2Installed</code>.
     */
    @Parameter
    private Dependency[] frameworkExtensions;

    /**
     * Bundle start level and auto start configuration used by the test runtime. Ignored if
     * {@link #testRuntime} is <code>p2Installed</code>. Example:
     *
     * <pre>
     * &lt;bundleStartLevel&gt;
     *   &lt;bundle&gt;
     *     &lt;id&gt;foo.bar.myplugin&lt;/id&gt;
     *     &lt;level&gt;6&lt;/level&gt;
     *     &lt;autoStart&gt;true&lt;/autoStart&gt;
     *   &lt;/bundle&gt;
     * &lt;/bundleStartLevel&gt;
     * </pre>
     */
    @Parameter
    private BundleStartLevel[] bundleStartLevel;

    /**
     * The default bundle start level and auto start configuration used by the test runtime for
     * bundles where the start level/auto start is not configured in {@link #bundleStartLevel}.
     * Ignored if {@link #testRuntime} is <code>p2Installed</code>. Example:
     *
     * <pre>
     *   &lt;defaultStartLevel&gt;
     *     &lt;level&gt;6&lt;/level&gt;
     *     &lt;autoStart&gt;true&lt;/autoStart&gt;
     *   &lt;/defaultStartLevel&gt;
     * </pre>
     */
    @Parameter
    private BundleStartLevel defaultStartLevel;

    /**
     * Flaky tests will re-run until they pass or the number of reruns has been exhausted. See
     * surefire documentation for details.
     * <p>
     * Note: This feature is supported only for JUnit 4.x
     * </p>
     */
    @Parameter(property = "surefire.rerunFailingTestsCount", defaultValue = "0")
    private Integer rerunFailingTestsCount;

    /**
     * Skips the remaining tests after the Nth failure or error. See surefire documentation for
     * details.
     */
    @Parameter(property = "surefire.skipAfterFailureCount", defaultValue = "0")
    private Integer skipAfterFailureCount;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    private ResolutionErrorHandler resolutionErrorHandler;

    @Inject
    private Map<String, TychoProject> projectTypes;

    @Inject
    private EquinoxInstallationFactory installationFactory;

    @Inject
    private ProvisionedInstallationBuilderFactory provisionedInstallationBuilderFactory;

    @Inject
    private EquinoxLauncher launcher;

    @Inject
    @Named("p2")
    protected DependencyResolver dependencyResolver;

    /**
     * Normally, Tycho will automatically determine the test framework provider based on the test
     * project's classpath. This options forces the use of a test framework provider implementation
     * with the given role hint. Tycho comes with providers such as
     * &quot;junit3&quot;,&quot;junit4&quot;,&quot;junit47&quot;,&quot;junit5&quot;, or
     * &quot;junit59&quot;. Note that when specifying a providerHint, you have to make sure the
     * provider is actually available in the dependencies of tycho-surefire-plugin.
     *
     * @since 0.16.0
     */
    @Parameter
    private String providerHint;

    /**
     * Defines the order the tests will be run in. Supported values are "alphabetical",
     * "reversealphabetical", "random", "hourly" (alphabetical on even hours, reverse alphabetical
     * on odd hours) and "filesystem".
     *
     * @since 0.19.0
     */
    @Parameter(defaultValue = "filesystem")
    private String runOrder;

    /**
     * When {@code true}, stack traces are trimmed to only show lines within the test.
     *
     * @since 1.3.0
     */
    @Parameter(property = "trimStackTrace", defaultValue = "false")
    private boolean trimStackTrace;

    /**
     * (JUnit 4.7 provider) Supports values "classes"/"methods"/"both" to run in separate threads,
     * as controlled by threadCount.
     *
     * @since 0.16.0
     */
    @Parameter(property = "parallel")
    private ParallelMode parallel;

    /**
     * (JUnit 4.7 provider) Indicates that threadCount is per cpu core.
     *
     * @since 0.16.0
     */
    @Parameter(property = "perCoreThreadCount", defaultValue = "true")
    private boolean perCoreThreadCount;

    /**
     * (JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should
     * be allocated for this execution. Only makes sense to use in conjunction with the parallel
     * parameter.
     *
     * @since 0.16.0
     */
    @Parameter(property = "threadCount")
    private int threadCount = -1;

    /**
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The parallel parameter
     * and the actual number of classes/methods will decide. Setting this to "true" effectively
     * disables perCoreThreadCount and threadCount.
     *
     * @since 0.16.0
     */
    @Parameter(property = "useUnlimitedThreads", defaultValue = "false")
    private boolean useUnlimitedThreads;

    /**
     * Use this to specify surefire provider-specific properties.
     *
     * @since 0.16.0
     */
    @Parameter
    private Properties providerProperties = new Properties();

    /**
     * How to create the OSGi test runtime. Allowed values are <code>default</code> and
     * <code>p2Installed</code>. Mode <code>p2Installed</code> is <b>EXPERIMENTAL</b> - only works
     * when installing products under test (see below).
     * <ul>
     * <li>In <code>default</code> mode, all necessary files to define the test runtime like
     * <code>config.ini</code> are generated by tycho. This installation mode has the advantage that
     * the test runtime is minimal (defined by the transitive dependencies of the test bundle plus
     * and the test harness) and existing bundle jars are referenced rather than copied for the
     * installation</li>
     * <li>In <code>p2Installed</code> mode, use p2 director to install test bundle, test harness
     * bundles and respective dependencies. This installation mode can be used for integration tests
     * that require a fully p2-provisioned installation. To install a product IU, add it as extra
     * requirement to the test bundle (see example below). Note that this installation mode comes
     * with a certain performance overhead for executing the provisioning operations otherwise not
     * required. Also note, that in this mode, in case the primary installation target environment
     * is macOS, {@link #work} is post-processed to ensure a proper macOS layout. That is,
     * <code>Eclipse.app/Contents/Eclipse</code> is automatically appended (or only
     * <code>Contents/Eclipse</code>, if <code>work</code> already ends with
     * <code>.app</code>).</li>
     * </ul>
     *
     * Example configuration which will install product IU under test "example.product.id" using p2:
     *
     * <pre>
     * &lt;plugin&gt;
     *    &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *    &lt;artifactId&gt;tycho-surefire-plugin&lt;/artifactId&gt;
     *    &lt;version&gt;${tycho-version}&lt;/version&gt;
     *    &lt;configuration&gt;
     *       &lt;testRuntime&gt;p2Installed&lt;/testRuntime&gt;
     *    &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * &lt;plugin&gt;
     *    &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *    &lt;artifactId&gt;target-platform-configuration&lt;/artifactId&gt;
     *    &lt;version&gt;${tycho-version}&lt;/version&gt;
     *    &lt;configuration&gt;
     *       &lt;dependency-resolution&gt;
     *          &lt;extraRequirements&gt;
     *             &lt;!-- product IU under test --&gt;
     *             &lt;requirement&gt;
     *                &lt;type&gt;p2-installable-unit&lt;/type&gt;
     *                &lt;id&gt;example.product.id&lt;/id&gt;
     *                &lt;versionRange&gt;0.0.0&lt;/versionRange&gt;
     *             &lt;/requirement&gt;
     *          &lt;/extraRequirements&gt;
     *       &lt;/dependency-resolution&gt;
     *    &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     *
     * @since 0.19.0
     */
    @Parameter(defaultValue = "default")
    private String testRuntime;

    /**
     * Additional dependencies to be added to the test runtime.
     *
     * Note: This parameter has only limited support for dependencies to artifacts within the
     * reactor. Therefore it is recommended to specify <code>extraRequirements</code> on the
     * <code>target-platform-configuration</code> plugin instead. Example:
     *
     * <pre>
     * &lt;plugin&gt;
     *    &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *    &lt;artifactId&gt;target-platform-configuration&lt;/artifactId&gt;
     *    &lt;version&gt;${tycho-version}&lt;/version&gt;
     *    &lt;configuration&gt;
     *       &lt;dependency-resolution&gt;
     *          &lt;extraRequirements&gt;
     *             &lt;requirement&gt;
     *                &lt;type&gt;eclipse-feature&lt;/type&gt;
     *                &lt;id&gt;example.project.feature&lt;/id&gt;
     *                &lt;versionRange&gt;0.0.0&lt;/versionRange&gt;
     *             &lt;/requirement&gt;
     *          &lt;/extraRequirements&gt;
     *       &lt;/dependency-resolution&gt;
     *    &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     *
     * The dependencies specified as <code>extraRequirements</code> are &ndash; together with the
     * dependencies specified in the <code>MANIFEST.MF</code> of the project &ndash; transitively
     * resolved against the target platform. The resulting set of bundles is included in the test
     * runtime.
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * Additional root IUs to install, only relevant if {@link #testRuntime} is
     * <code>p2Installed</code>.
     *
     * <pre>
     * &lt;install&gt;
     *    &lt;iu&gt;
     *       &lt;id&gt;...&lt;/id&gt;
     *       &lt;version&gt;...optional version...&lt;/id&gt;
     *       &lt;feature&gt;true/false&lt;/feature&gt; &lt;!-- optional if true .feature.group is automatically added to the id  --&gt;
     * &lt;/install&gt;
     * </pre>
     */
    @Parameter
    private List<IU> install;

    /**
     * Additional repositories used to install units from, only relevant if {@link #testRuntime} is
     * <code>p2Installed</code>.
     *
     * <pre>
    * &lt;repositories&gt;
    *   &lt;repository&gt;
    *       &lt;url&gt;...another repository...&lt;/url&gt;
    *   &lt;/repository&gt;
    * &lt;/repositories&gt;
     * </pre>
     *
     */
    @Parameter(name = "repositories")
    private List<Repository> repositories;

    /**
     * p2 <a href=
     * "https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html"
     * >profile</a> name of the installation under test.
     *
     * Only relevant if {@link #testRuntime} is <code>p2Installed</code>. If tests are installed on
     * top of an already existing installation in {@link #work}, this must match the name of the
     * existing profile.
     *
     * @since 0.19.0
     */
    @Parameter(defaultValue = TychoConstants.DEFAULT_PROFILE)
    private String profileName;

    /**
     * Configures the overall concurrency level. <b>Important Note:</b> If there are multiple mojo
     * configurations this will choose the lowest configured number! So for example if in the same
     * reactor there is one configuration with a concurrency level of 5 and one with a concurrency
     * level of 3 then then only three parallel runs will be possible!
     */
    @Parameter
    private int reactorConcurrencyLevel;

    public enum ClassLoaderOrder {
        booterFirst, testProbeFirst
    }

    /**
     * The test runtime is configured with a composite class loader. This defines the order in which
     * the loaders are searched, and it may need to be configured depending on the resolved
     * classpath of the project.
     * <p>
     * Available values are:
     * <ul>
     * <li><code>booterFirst</code> - the loader of the surefire booter (including bundled test
     * platform) is searched first</li>
     * <li><code>testProbeFirst</code> - the loader of the test class's plugin is searched
     * first</li>
     * </ul>
     * Defaults to <code>booterFirst</code>.
     */
    @Parameter(defaultValue = "booterFirst")
    private ClassLoaderOrder classLoaderOrder;

    @Inject
    private ProviderHelper providerHelper;

    @Inject
    private RepositoryReferenceTool repositoryReferenceTool;

    @Inject
    protected InstallableUnitGenerator generator;

    /**
     * Only supported by the TestNG test provider. The values specified are passed to TestNG as test
     * suite files. The suite files will overwrite the {@link #includes} and {@link #excludes}
     * patterns. The path to the suite file(s) could be relative (test bundle classpath) or an
     * absolute path to xml files outside the test bundle.
     *
     * <pre>
     * &lt;configuration&gt;
     *   &lt;suiteXmlFiles&gt;
     *     &lt;suiteXmlFile&gt;myTestSuite.xml&lt;/suiteXmlFile&gt;
     *   &lt;/suiteXmlFiles&gt;
     * &lt;/configuration&gt;
     * </pre>
     */
    @Parameter
    private List<String> suiteXmlFiles;

    @Override
    protected void runTests(ScanResult scanResult) throws MojoExecutionException, MojoFailureException {
        // Allow constructing the test runtime against filtered OSGi/PDE metadata
        final ReactorProject reactorProject = getReactorProject();
        reactorProject.setContextValue(TychoConstants.CTX_METADATA_ARTIFACT_LOCATION, metadataDirectory);

        EquinoxInstallation equinoxTestRuntime;
        synchronized (AbstractEclipseTestMojo.class) {
            if ("p2Installed".equals(testRuntime)) {
                equinoxTestRuntime = createProvisionedInstallation();
            } else if ("default".equals(testRuntime)) {
                equinoxTestRuntime = createEclipseInstallation();
            } else {
                throw new MojoExecutionException("Configured testRuntime parameter value '" + testRuntime
                        + "' is unknown. Allowed values: 'default', 'p2Installed'.");
            }
        }
        if (equinoxTestRuntime != null) {
            try (AutoCloseable runLock = CONCURRENCY_LOCK.aquire(reactorConcurrencyLevel)) {
                runTest(equinoxTestRuntime);
            } catch (InterruptedException e) {
                return;
            } catch (MojoExecutionException | MojoFailureException e) {
                throw e;
            } catch (Exception e) {
                throw new MojoFailureException(e);
            }
        }

    }

    private EquinoxInstallation createProvisionedInstallation() throws MojoExecutionException, MojoFailureException {
        ScanResult scanResult = scanForTests();
        if (scanResult.size() == 0) {
            handleNoTestsFound(); //this might throw an exception...
            //... if not we notify the caller that nothing has to be done here.
            return null;
        }
        TestFrameworkProvider provider = providerHelper
                .selectProvider(project, getProjectType().getClasspath(DefaultReactorProject.adapt(project)),
                        getMergedProviderProperties(), providerHint)
                .provider();
        try {
            PropertiesWrapper wrapper = createSurefireProperties(provider, scanResult);
            storeProperties(wrapper.getProperties(), surefireProperties);

            ProvisionedInstallationBuilder installationBuilder = provisionedInstallationBuilderFactory
                    .createInstallationBuilder();
            Set<Artifact> testHarnessArtifacts = providerHelper.filterTestFrameworkBundles(provider, pluginArtifacts);
            for (Artifact testHarnessArtifact : testHarnessArtifacts) {
                installationBuilder.addBundleJar(testHarnessArtifact.getFile());
            }
            RepositoryReferences sources = repositoryReferenceTool.getVisibleRepositories(project, session,
                    RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE);
            if (repositories != null) {
                for (Repository repository : repositories) {
                    String url = repository.getUrl();
                    if (url == null || url.isBlank()) {
                        throw new MojoExecutionException("Repository url can't be empty!");
                    }
                    URI uri = new URI(url);
                    getLog().info("Adding repository " + uri + "...");
                    sources.addRepository(uri);
                }
            }
            installationBuilder.addMetadataRepositories(sources.getMetadataRepositories());
            installationBuilder.addArtifactRepositories(sources.getArtifactRepositories());
            installationBuilder.setProfileName(profileName);
            installationBuilder.addIUsToBeInstalled(getIUsToInstall(testHarnessArtifacts));
            File workingDir = new File(project.getBuild().getDirectory(), "p2temp");
            workingDir.mkdirs();
            installationBuilder.setWorkingDir(workingDir);
            installationBuilder.setDestination(work);
            List<TargetEnvironment> list = getTestTargetEnvironments();
            TargetEnvironment testEnvironment = list.get(0);
            installationBuilder.setTargetEnvironment(testEnvironment);
            getLog().info("Provisioning with environment " + testEnvironment + "...");
            return installationBuilder.install();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (MojoFailureException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new MojoExecutionException(e.getInput() + " is not a valid URI", e);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<String> getIUsToInstall(Set<Artifact> testHarnessArtifacts) {
        List<String> iusToInstall = new ArrayList<>();
        // 1. test bundle
        iusToInstall.add(getTestBundleSymbolicName());
        // 2. test harness bundles
        iusToInstall.addAll(providerHelper.getSymbolicNames(testHarnessArtifacts));
        // 3. extra dependencies
        LinkedHashSet<ArtifactKey> extraDependencies = new LinkedHashSet<>(
                projectManager.getTargetPlatformConfiguration(project).getAdditionalArtifacts());
        extraDependencies.addAll(osgiBundle.getExtraTestRequirements(getReactorProject()));
        // 4. mojo specified extras
        if (this.install != null) {
            for (IU iu : this.install) {
                extraDependencies.add(new DefaultArtifactKey(
                        iu.feature ? ArtifactType.TYPE_ECLIPSE_FEATURE : ArtifactType.TYPE_ECLIPSE_PLUGIN, iu.id,
                        Objects.requireNonNullElse(iu.version, "0.0.0")));
            }
        }
        for (ArtifactKey extraDependency : extraDependencies) {
            String type = extraDependency.getType();
            if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
                iusToInstall.add(extraDependency.getId());
            } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
                iusToInstall.add(extraDependency.getId() + ".feature.group");
            }
        }
        return iusToInstall;
    }

    private BundleProject getProjectType() {
        return (BundleProject) projectTypes.get(project.getPackaging());
    }

    private EquinoxInstallation createEclipseInstallation() throws MojoExecutionException, MojoFailureException {
        ScanResult scanResult = scanForTests();
        if (scanResult.size() == 0) {
            handleNoTestsFound(); //this might throw an exception...
            //... if not we notify the caller that nothing has to be done here.
            return null;
        }
        ProviderSelection selection = providerHelper.selectProvider(project,
                getProjectType().getTestClasspath(DefaultReactorProject.adapt(project)), getMergedProviderProperties(),
                providerHint);
        TestFrameworkProvider provider = selection.provider();
        getLog().info(String.format("Selected test framework %s (%s) with provider %s %s", provider.getType(),
                provider.getVersion(), selection.hint(), provider.getVersionRange()));
        Collection<IRequirement> testRequiredPackages = new ArrayList<>();
        Set<Artifact> testFrameworkBundles = providerHelper.filterTestFrameworkBundles(provider, pluginArtifacts);
        for (Artifact artifact : testFrameworkBundles) {
            generator.getInstallableUnits(artifact).stream().flatMap(iu -> iu.getRequirements().stream())
                    .filter(req -> {
                        if (req instanceof IRequiredCapability reqcap) {
                            if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(reqcap.getNamespace())) {
                                return true;
                            }
                        }
                        return false;
                    }).forEach(testRequiredPackages::add);
        }
        DependencyArtifacts testRuntimeArtifacts = resolveDependencies(testRequiredPackages);

        work.mkdirs();

        EquinoxInstallationDescription testRuntime = new DefaultEquinoxInstallationDescription();
        testRuntime.setDefaultBundleStartLevel(defaultStartLevel);
        testRuntime.addBundlesToExplode(getBundlesToExplode());
        testRuntime.addFrameworkExtensions(getFrameworkExtensions());
        if (bundleStartLevel != null) {
            for (BundleStartLevel level : bundleStartLevel) {
                testRuntime.addBundleStartLevel(level);
            }
        }
        PropertiesWrapper wrapper = createSurefireProperties(provider, scanResult);
        storeProperties(wrapper.getProperties(), surefireProperties);
        for (ArtifactDescriptor artifact : testRuntimeArtifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN)) {
            // note that this project is added as directory structure rooted at project basedir.
            // project classes and test-classes are added via dev.properties file (see #createDevProperties())
            // all other projects are added as bundle jars.
            ReactorProject otherProject = artifact.getMavenProject();
            if (otherProject != null) {
                // Contrary to what's written above, we use the project's root directory only when
                // we do not need custom metadata. If we need, we load the test bundle as JAR instead
                if (useMetadataDirectory(otherProject)) {
                    addBundle(testRuntime, artifact.getKey(), metadataDirectory);
                    continue;
                }
                File file = otherProject.getArtifact(artifact.getClassifier());
                if (file != null) {
                    addBundle(testRuntime, artifact.getKey(), file);
                    continue;
                }
            }
            try {
                addBundle(testRuntime, artifact.getKey(), artifact.fetchArtifact().get());
            } catch (InterruptedException e) {
                throw new MojoExecutionException("interrupted");
            } catch (ExecutionException e) {
                throw new MojoFailureException("fetching artifact failed", e);
            }
        }

        setupTestBundles(testFrameworkBundles, testRuntime, provider);

        getReportsDirectory().mkdirs();
        return installationFactory.createInstallation(testRuntime, work);
    }

    protected boolean useMetadataDirectory(ReactorProject otherProject) {
        return otherProject.sameProject(project) && project.getBasedir().equals(metadataDirectory);
    }

    private void addBundle(EquinoxInstallationDescription runtime, ArtifactKey artifact, File file) {
        if (file == null) {
            throw new IllegalArgumentException("File for artifact " + artifact + " is null");
        }
        runtime.addBundle(artifact.getId(), artifact.getVersion(), file);
    }

    protected DependencyArtifacts resolveDependencies(Collection<IRequirement> additionalRequirements)
            throws MojoExecutionException {
        List<ArtifactKey> extraDependencies = getExtraDependencies();
        final DependencyResolverConfiguration resolverConfiguration = new DependencyResolverConfiguration() {
            @Override
            public OptionalResolutionAction getOptionalResolutionAction() {
                return OptionalResolutionAction.IGNORE;
            }

            @Override
            public List<ArtifactKey> getAdditionalArtifacts() {
                return extraDependencies;
            }

            @Override
            public Collection<IRequirement> getAdditionalRequirements() {
                return additionalRequirements;
            }

        };
        DependencyArtifacts testRuntimeArtifacts = dependencyResolver.resolveDependencies(session, project,
                projectManager.getTargetPlatform(project)
                        .orElseThrow(() -> new MojoExecutionException(TychoConstants.TYCHO_NOT_CONFIGURED + project)),
                resolverConfiguration, getTestTargetEnvironments());
        if (testRuntimeArtifacts == null) {
            throw new MojoExecutionException(
                    "Cannot determinate build target platform location -- not executing tests");
        }
        return testRuntimeArtifacts;
    }

    protected void setupTestBundles(Set<Artifact> testFrameworkBundles, EquinoxInstallationDescription testRuntime,
            TestFrameworkProvider provider) throws MojoExecutionException {
        for (Artifact artifact : testFrameworkBundles) {
            File bundleLocation = artifact.getFile();
            ArtifactKey bundleArtifactKey = getBundleArtifactKey(bundleLocation);
            addBundle(testRuntime, bundleArtifactKey, bundleLocation);
        }

        testRuntime.addDevEntries(getTestBundleSymbolicName(), getBuildOutputDirectories());
    }

    private String getTestBundleSymbolicName() {
        return getProjectType().getArtifactKey(getReactorProject()).getId();
    }

    protected ArtifactKey getBundleArtifactKey(File file) throws MojoExecutionException {
        ArtifactKey key = osgiBundle.readArtifactKey(file);
        if (key == null) {
            throw new MojoExecutionException("Not an OSGi bundle " + file.getAbsolutePath());
        }
        return key;
    }

    protected List<ArtifactKey> getExtraDependencies() {
        final List<ArtifactKey> dependencies = new ArrayList<>();
        if (this.dependencies != null) {
            for (Dependency key : this.dependencies) {
                dependencies.add(new DefaultArtifactKey(key.getType(), key.getArtifactId(), key.getVersion()));
            }
        }
        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        dependencies.addAll(configuration.getDependencyResolverConfiguration().getAdditionalArtifacts());
        dependencies.addAll(osgiBundle.getExtraTestRequirements(getReactorProject()));
        dependencies.addAll(getTestDependencies());
        return dependencies;
    }

    private List<ArtifactKey> getTestDependencies() {
        ArrayList<ArtifactKey> result = new ArrayList<>();

        // The test harness dependencies must be satisfiable from the external target platform
        // See also https://github.com/eclipse-tycho/tycho/issues/5349 for a special situation
        // where the harness bundles are part of the same reactor (e.g. when building eclipse.platform.ui)
        result.add(newBundleDependency("org.eclipse.osgi"));
        result.add(newBundleDependency(DefaultEquinoxInstallationDescription.EQUINOX_LAUNCHER));
        if (useUIHarness) {
            result.add(newBundleDependency("org.eclipse.ui.ide.application"));
        } else {
            result.add(newBundleDependency("org.eclipse.core.runtime"));
        }

        return result;
    }

    protected ArtifactKey newBundleDependency(String bundleId) {
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, bundleId);
    }

    protected PropertiesWrapper createSurefireProperties(TestFrameworkProvider provider, ScanResult scanResult)
            throws MojoExecutionException {
        PropertiesWrapper wrapper = new PropertiesWrapper(new HashMap<>());
        wrapper.setProperty("testpluginname", getTestBundleSymbolicName());
        wrapper.setProperty("testclassesdirectory", getTestClassesDirectory().getAbsolutePath());
        wrapper.setProperty("reportsdirectory", getReportsDirectory().getAbsolutePath());
        wrapper.setProperty("redirectTestOutputToFile", String.valueOf(redirectTestOutputToFile));

        wrapper.setProperty("runOrder", runOrder);
        wrapper.setProperty("trimStackTrace", String.valueOf(trimStackTrace));
        wrapper.setProperty("skipAfterFailureCount", String.valueOf(skipAfterFailureCount));
        wrapper.setProperty("rerunFailingTestsCount", String.valueOf(rerunFailingTestsCount));
        wrapper.setProperty("printBundles", String.valueOf(printBundles));
        wrapper.setProperty("printWires", String.valueOf(printWires));
        wrapper.setProperty("classLoaderOrder", classLoaderOrder.toString());
        Properties mergedProviderProperties = getMergedProviderProperties();
        mergedProviderProperties.putAll(provider.getProviderSpecificProperties());
        Map<String, String> providerPropertiesAsMap = propertiesAsMap(mergedProviderProperties);
        scanResult.writeTo(providerPropertiesAsMap);
        for (Map.Entry<String, String> entry : providerPropertiesAsMap.entrySet()) {
            wrapper.setProperty("__provider." + entry.getKey(), entry.getValue().toString());
        }
        wrapper.setProperty("testprovider", provider.getSurefireProviderClassName());
        getLog().debug("Using test framework provider: " + provider.getClass().getName());
        wrapper.addList(suiteXmlFiles, BooterConstants.TEST_SUITE_XML_FILES);
        return wrapper;
    }

    protected Properties getMergedProviderProperties() throws MojoExecutionException {
        Properties result = new Properties();
        result.putAll(providerProperties);
        if (parallel != null) {
            result.put(ProviderParameterNames.PARALLEL_PROP, parallel.name());
            if (!useUnlimitedThreads) {
                if (perCoreThreadCount && threadCount < 1) {
                    throw new MojoExecutionException(
                            "Parallel mode with perCoreThreadCount=true requires threadCount>=1");
                }
                if (!perCoreThreadCount && threadCount <= 1) {
                    throw new MojoExecutionException(
                            "Parallel mode requires threadCount>1 or useUnlimitedThreads=true");
                }
            }
            if (threadCount > 0) {
                result.put(ProviderParameterNames.THREADCOUNT_PROP, String.valueOf(threadCount));
            }
            result.put(/* JUnitCoreParameters.PERCORETHREADCOUNT_KEY */"perCoreThreadCount",
                    String.valueOf(perCoreThreadCount));
            result.put(/* JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY */"useUnlimitedThreads",
                    String.valueOf(useUnlimitedThreads));
        }
        if (groups != null) {
            result.put(ProviderParameterNames.TESTNG_GROUPS_PROP, groups);
        }
        if (excludedGroups != null) {
            result.put(ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP, excludedGroups);
        }
        return result;
    }

    private void storeProperties(Map<String, String> propertiesMap, File file) throws MojoExecutionException {
        Properties p = new Properties();
        p.putAll(propertiesMap);
        try {
            ReproducibleUtils.storeProperties(p, file.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write test launcher properties file", e);
        }
    }

    private void runTest(EquinoxInstallation testRuntime) throws MojoExecutionException, MojoFailureException {
        int result;
        File logFile = new File(osgiDataDirectory, ".metadata/.log");
        LaunchConfiguration cli;
        try {
            if (deleteOsgiDataDirectory) {
                FileUtils.deleteDirectory(osgiDataDirectory);
            }
            cli = createCommandLine(testRuntime);
            getLog().info("Executing test runtime with timeout (seconds): " + forkedProcessTimeoutInSeconds
                    + ", logs, if any, will be placed at: " + logFile.getAbsolutePath());
            result = launcher.execute(cli, forkedProcessTimeoutInSeconds);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while executing platform", e);
        }
        switch (result) {
        case 0:
            handleSuccess();
            break;

        case 200: /* see AbstractUITestApplication */
            if (application == null) {
                // the extra test dependencies should prevent this case
                throw new MojoExecutionException(
                        "Could not find the default application \"org.eclipse.ui.ide.workbench\" in the test runtime.");
            } else {
                throw new MojoFailureException("Could not find application \"" + application
                        + "\" in the test runtime. Make sure that the test runtime includes the bundle "
                        + "which defines this application.");
            }

        case 254/* RunResult.NO_TESTS */:
            handleNoTestsFound();
            break;

        case 255/* RunResult.FAILURE */:
            handleTestFailures();
            break;

        default:
            StringBuilder defaultMessage = new StringBuilder(
                    "An unexpected error occurred while launching the test runtime (process returned error code ");
            defaultMessage.append(decodeReturnCode(result));
            defaultMessage.append(").");
            if (logFile.exists()) {
                defaultMessage.append(" The process logfile ");
                defaultMessage.append(logFile.getAbsolutePath());
                defaultMessage.append(" might contain further details.");
            }
            defaultMessage.append(" Command-line used to launch the sub-process was ");
            defaultMessage.append(cli.getJvmExecutable());
            String[] vmArguments = cli.getVMArguments();
            if (vmArguments != null && vmArguments.length > 0) {
                defaultMessage.append(" ");
                defaultMessage.append(String.join(" ", vmArguments));
            }
            defaultMessage.append(" -jar ");
            defaultMessage.append(cli.getLauncherJar());
            String[] programArguments = cli.getProgramArguments();
            if (programArguments != null && programArguments.length > 0) {
                defaultMessage.append(" ");
                defaultMessage.append(String.join(" ", programArguments));
            }
            defaultMessage.append(" in working directory ");
            defaultMessage.append(cli.getWorkingDirectory());
            throw new MojoFailureException(defaultMessage.toString());
        }
    }

    protected abstract void handleTestFailures() throws MojoFailureException;

    protected abstract void handleSuccess();

    private String decodeReturnCode(int result) {
        try {
            if (PlatformPropertiesUtils.OS_LINUX.equals(PlatformPropertiesUtils.getOS(System.getProperties()))) {
                int signal = result - 128;
                if (signal > 0 && signal < UNIX_SIGNAL_NAMES.length) {
                    return result + "(" + UNIX_SIGNAL_NAMES[signal] + " received?)";
                }
            } else if (PlatformPropertiesUtils.OS_WIN32.equals(PlatformPropertiesUtils.getOS(System.getProperties()))) {
                return result + " (HRESULT Code 0x" + Integer.toHexString(result).toUpperCase()
                        + ", check for example https://www.hresult.info/ for further details)";
            }
        } catch (RuntimeException e) {
            getLog().debug("Decoding return code failed", e);
        }
        return String.valueOf(result);
    }

    private EquinoxLaunchConfiguration createCommandLine(EquinoxInstallation testRuntime)
            throws MalformedURLException, MojoExecutionException {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(testRuntime);

        String executable = getJavaExecutable();
        cli.setJvmExecutable(executable);

        cli.setWorkingDirectory(project.getBasedir());

        if (debugPort > 0) {
            cli.addVMArguments("-Xdebug", "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y");
        }

        cli.addVMArguments("-Dosgi.noShutdown=false");
        TargetEnvironment environment = TargetEnvironment.getRunningEnvironment();
        cli.addVMArguments("-Dosgi.os=" + environment.getOs(), //
                "-Dosgi.ws=" + environment.getWs(), //
                "-Dosgi.arch=" + environment.getArch());
        addCustomProfileArg(cli);
        cli.addVMArguments(splitArgLine(argLine));

        for (Map.Entry<String, String> entry : getMergedSystemProperties().entrySet()) {
            cli.addVMArguments("-D" + entry.getKey() + "=" + entry.getValue());
        }
        if (debugOptions != null || getLog().isDebugEnabled()) {
            if (debugOptions == null || debugOptions.isBlank()) {
                cli.addProgramArguments("-debug");
            } else {
                cli.addProgramArguments("-debug", new File(debugOptions).getAbsolutePath());
            }
        }
        if (getLog().isDebugEnabled() || showEclipseLog) {
            cli.addProgramArguments("-consolelog");
        }
        addProgramArgs(cli, "-data", osgiDataDirectory.getAbsolutePath(), //
                "-install", testRuntime.getLocation().getAbsolutePath(), //
                "-configuration", testRuntime.getConfigurationLocation().getAbsolutePath(), //
                "-application", getTestApplication(), //
                "-testproperties", surefireProperties.getAbsolutePath());
        if (application != null) {
            cli.addProgramArguments("-testApplication", application);
        }
        if (product != null) {
            cli.addProgramArguments("-product", product);
        }
        if (useUIHarness && !useUIThread) {
            cli.addProgramArguments("-nouithread");
        }
        cli.addProgramArguments(splitArgLine(appArgLine));
        if (environmentVariables != null) {
            cli.addEnvironmentVariables(environmentVariables);
        }
        if (enableAssertions) {
            cli.addVMArguments("-ea");
        }
        return cli;
    }

    private Map<String, String> getMergedSystemProperties() {
        Map<String, String> result = new LinkedHashMap<>();
        // bug 415489: use osgi.clean=true by default
        result.put("osgi.clean", "true");
        if (systemProperties != null) {
            result.putAll(systemProperties);
        }
        return result;
    }

    private void addCustomProfileArg(EquinoxLaunchConfiguration cli) throws MojoExecutionException {

        ExecutionEnvironmentConfiguration eeConfig = projectManager.getExecutionEnvironmentConfiguration(project);
        if (eeConfig.isCustomProfile()) {
            Properties customProfileProps = eeConfig.getFullSpecification().getProfileProperties();
            File profileFile = new File(new File(project.getBuild().getDirectory()), "custom.profile");
            storeProperties(propertiesAsMap(customProfileProps), profileFile);
            cli.addVMArguments("-D" + EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE + "=" + profileFile.toURI());
        }
    }

    private Map<String, String> propertiesAsMap(Properties p) {
        Map<String, String> result = new HashMap<>();
        for (String entry : p.stringPropertyNames()) {
            result.put(entry, p.getProperty(entry));
        }
        return result;
    }

    void addProgramArgs(EquinoxLaunchConfiguration cli, String... arguments) {
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument != null) {
                    cli.addProgramArguments(argument);
                }
            }
        }
    }

    String[] splitArgLine(String argLine) throws MojoExecutionException {
        try {
            return CommandLineUtils.translateCommandline(argLine);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while parsing commandline: " + e.getMessage(), e);
        }
    }

    private String getTestApplication() {
        if (useUIHarness) {
            return "org.eclipse.tycho.surefire.osgibooter.uitest";
        } else {
            return "org.eclipse.tycho.surefire.osgibooter.headlesstest";
        }
    }

    private String getBuildOutputDirectories() {
        StringJoiner sb = new StringJoiner(",");
        ReactorProject reactorProject = getReactorProject();
        BuildDirectory buildDirectory = reactorProject.getBuildDirectory();
        sb.add(buildDirectory.getOutputDirectory().toString());
        sb.add(buildDirectory.getTestOutputDirectory().toString());
        for (BuildOutputJar outputJar : osgiBundle.getEclipsePluginProject(reactorProject).getOutputJars()) {
            if (".".equals(outputJar.getName())) {
                // handled above
                continue;
            }
            sb.add(outputJar.getOutputDirectory().getAbsolutePath());
        }
        return sb.toString();
    }

    private List<String> getBundlesToExplode() {
        List<String> bundles = new ArrayList<>();

        if (explodedBundles != null) {
            bundles.addAll(Arrays.asList(explodedBundles));
        }

        return bundles;
    }

    private List<File> getFrameworkExtensions() throws MojoExecutionException {
        List<File> files = new ArrayList<>();

        if (frameworkExtensions != null) {
            for (Dependency frameworkExtension : frameworkExtensions) {
                Artifact artifact = repositorySystem.createDependencyArtifact(frameworkExtension);
                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setResolveRoot(true).setResolveTransitively(false);
                request.setLocalRepository(session.getLocalRepository());
                // XXX wrong repositories -- these are user artifacts, not plugin artifacts
                request.setRemoteRepositories(project.getPluginArtifactRepositories());
                request.setOffline(session.isOffline());
                request.setForceUpdate(session.getRequest().isUpdateSnapshots());
                ArtifactResolutionResult result = repositorySystem.resolve(request);
                try {
                    resolutionErrorHandler.throwErrors(request, result);
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException(
                            "Failed to resolve framework extension " + frameworkExtension.getManagementKey(), e);
                }
                files.add(artifact.getFile());
            }
        }

        return files;
    }

}
