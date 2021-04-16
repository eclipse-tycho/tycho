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
 *    Christoph Läubrich    - [Bug 529929] improve error message in case of failures
 *                          - [Bug 572420] Tycho-Surefire should be executable for eclipse-plugin package type
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.maven.ToolchainProvider.JDKUsage;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.dev.DevBundleInfo;
import org.eclipse.tycho.dev.DevWorkspaceResolver;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.surefire.provider.impl.ProviderHelper;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.eclipse.tycho.surefire.provisioning.ProvisionedInstallationBuilder;
import org.eclipse.tycho.surefire.provisioning.ProvisionedInstallationBuilderFactory;

public abstract class AbstractTestMojo extends AbstractMojo {

    private static String[] UNIX_SIGNAL_NAMES = { "not a signal", // padding, singles start with 1
            "SIGHUP", "SIGINT", "SIGQUIT", "SIGILL", "SIGTRAP", "SIGABRT", "SIGBUS", "SIGFPE", "SIGKILL", "SIGUSR1",
            "SIGSEGV", "SIGUSR2", "SIGPIPE", "SIGALRM", "SIGTERM", "SIGSTKFLT", "SIGCHLD", "SIGCONT", "SIGSTOP",
            "SIGTSTP", "SIGTTIN", "SIGTTOU", "SIGURG", "SIGXCPU", "SIGXFSZ", "SIGVTALRM", "SIGPROF", "SIGWINCH",
            "SIGIO", "SIGPWR", "SIGSYS" };

    private static final Object LOCK = new Object();

    /**
     * Root directory (<a href=
     * "https://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstallarea"
     * >osgi.install.area</a>) of the Equinox runtime used to execute tests.
     */
    @Parameter(defaultValue = "${project.build.directory}/work")
    private File work;
    /**
     * <a href=
     * "https://help.eclipse.org/juno/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstancearea"
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

    @Parameter(property = "project", readonly = true)
    protected MavenProject project;

    /**
     * Set this parameter to suspend the test JVM waiting for a client to open a remote debug
     * session on the specified port.
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
     * List of patterns (separated by commas) used to specify the tests that should be included in
     * testing. When not specified and when the <code>test</code> parameter is not specified, the
     * default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*Tests.java   **&#47;*TestCase.java</code>
     */
    @Parameter
    private List<String> includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in
     * testing. When not specified and when the <code>test</code> parameter is not specified, the
     * default excludes will be <code>**&#47;*$*</code> (which excludes all inner classes).
     */
    @Parameter
    private List<String> excludes;

    /**
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern
     * matching, to select tests to run. The Ant pattern will be used to create an include pattern
     * formatted like <code>**&#47;${test}.java</code> When used, the <code>includes</code> and
     * <code>excludes</code> patterns parameters are ignored
     */
    @Parameter(property = "test")
    private String test;

    /**
     * @deprecated Use skipTests instead.
     */
    @Deprecated
    @Parameter(property = "maven.test.skipExec")
    private boolean skipExec;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED,
     * but quite convenient on occasion. Default: <code>false</code>
     */
    @Parameter(property = "skipTests")
    private Boolean skipTests;

    /**
     * Same as {@link #skipTests}
     */
    @Parameter(property = "maven.test.skip")
    private Boolean skip;

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
    @Parameter(property = "tycho.printBundles", defaultValue = "false")
    private boolean printBundles;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     */
    @Parameter(property = "maven.test.redirectTestOutputToFile", defaultValue = "false")
    private boolean redirectTestOutputToFile;

    @Parameter(defaultValue = "${project.build.directory}/surefire.properties")
    private File surefireProperties;

    /**
     * Additional dependencies to be added to the test runtime. Ignored if {@link #testRuntime} is
     * <code>p2Installed</code>.
     *
     * Note: This parameter has only limited support for dependencies to artifacts within the
     * reactor. Therefore it is recommended to specify <tt>extraRequirements</tt> on the
     * <tt>target-platform-configuration</tt> plugin instead. Example:
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
     * The dependencies specified as <tt>extraRequirements</tt> are &ndash; together with the
     * dependencies specified in the <tt>MANIFEST.MF</tt> of the project &ndash; transitively
     * resolved against the target platform. The resulting set of bundles is included in the test
     * runtime.
     */
    @Parameter
    private Dependency[] dependencies;

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

    @Parameter(property = "session", readonly = true, required = true)
    private MavenSession session;

    /**
     * Run tests using UI (true) or headless (false) test harness.
     */
    @Parameter(defaultValue = "false")
    private boolean useUIHarness;

    /**
     * Run tests in UI (true) or background (false) thread. Only applies to UI test harness.
     */
    @Parameter(defaultValue = "true")
    private boolean useUIThread;

    /**
     * By default, Tycho Surefire disables JVM assertions for the execution of your test cases. To
     * enable the assertions, set this flag to "true".
     *
     * @since 1.5.0
     */
    @Parameter(property = "enableAssertions", defaultValue = "false")
    private boolean enableAssertions;

    @Parameter(property = "plugin.artifacts")
    private List<Artifact> pluginArtifacts;

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
     * Identifies a single test (suite) class to run. This is useful if you have a single JUnit test
     * suite class defining which tests should be executed. Will be ignored if {@link #test} is
     * specified. Example:
     *
     * <pre>
     * &lt;testClass&gt;foo.bar.FooTest&lt;/testClass&gt;
     * </pre>
     */
    @Parameter(property = "testClass")
    private String testClass;

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

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ResolutionErrorHandler resolutionErrorHandler;

    @Component
    private DefaultDependencyResolverFactory dependencyResolverLocator;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Component
    private EquinoxInstallationFactory installationFactory;

    @Component
    private ProvisionedInstallationBuilderFactory provisionedInstallationBuilderFactory;

    @Component
    private EquinoxLauncher launcher;

    @Component(role = TychoProject.class, hint = "eclipse-plugin")
    private OsgiBundleProject osgiBundle;

    /**
     * Normally tycho will automatically determine the test framework provider based on the test
     * project's classpath. Use this to force using a test framework provider implementation with
     * the given role hint. Tycho comes with providers
     * &quot;junit3&quot;,&quot;junit4&quot;,&quot;junit47&quot;,&quot;junit5&quot;. Note that when
     * specifying a providerHint, you have to make sure the provider is actually available in the
     * dependencies of tycho-surefire-plugin.
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
    @Parameter(property = "trimStackTrace", defaultValue = "true")
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
     * <tt>config.ini</tt> are generated by tycho. This installation mode has the advantage that the
     * test runtime is minimal (defined by the transitive dependencies of the test bundle plus and
     * the test harness) and existing bundle jars are referenced rather than copied for the
     * installation</li>
     * <li>In <code>p2Installed</code> mode, use p2 director to install test bundle, test harness
     * bundles and respective dependencies. This installation mode can be used for integration tests
     * that require a fully p2-provisioned installation. To install a product IU, add it as extra
     * requirement to the test bundle (see example below). Note that this installation mode comes
     * with a certain performance overhead for executing the provisioning operations otherwise not
     * required.</li>
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
     * p2 <a href=
     * "https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html"
     * >profile</a> name of the installation under test.
     *
     * Only relevant if {@link #testRuntime} is <code>p2Installed</code>. If tests are installed on
     * top of an already existing installation in {@link #work}, this must match the name of the
     * existing profile.
     *
     * @since 0.19.0
     */
    // default value should be kept the same as DirectorMojo#profile default value
    @Parameter(defaultValue = "DefaultProfile")
    private String profileName;

    @Component
    private ToolchainManager toolchainManager;

    @Component
    private ProviderHelper providerHelper;

    @Component
    private DevWorkspaceResolver workspaceState;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    private ToolchainProvider toolchainProvider;

    /**
     * Which JDK to use for executing tests. Possible values are: <code>SYSTEM</code>,
     * <code>BREE</code> .
     * <p/>
     * <ul>
     * <li>SYSTEM: Use the currently running JVM (or from
     * <a href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchain</a> if
     * configured in pom.xml)</li>
     * <li>BREE: use MANIFEST header <code>Bundle-RequiredExecutionEnvironment</code> to lookup the
     * JDK from
     * <a href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchains.xml</a>.
     * The value of BREE will be matched against the id of the toolchain elements in
     * toolchains.xml.</li>
     * </ul>
     *
     * Example for BREE: <br>
     * In <code>META-INF/MANIFEST.MF</code>:
     *
     * <pre>
     * Bundle-RequiredExecutionEnvironment: JavaSE-1.7
     * </pre>
     *
     * In toolchains.xml:
     *
     * <pre>
     * &lt;toolchains&gt;
     *    &lt;toolchain&gt;
     *       &lt;type&gt;jdk&lt;/type&gt;
     *       &lt;provides&gt;
     *          &lt;id&gt;JavaSE-1.7&lt;/id&gt;
     *       &lt;/provides&gt;
     *       &lt;configuration&gt;
     *          &lt;jdkHome&gt;/path/to/jdk/1.7&lt;/jdkHome&gt;
     *       &lt;/configuration&gt;
     *    &lt;/toolchain&gt;
     * &lt;/toolchains&gt;
     * </pre>
     */
    @Parameter(defaultValue = "SYSTEM")
    private JDKUsage useJDK;

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
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (shouldSkip()) {
            getLog().info("Skipping tests");
            return;
        }
        if (shouldRun()) {
            synchronized (LOCK) {
                EquinoxInstallation equinoxTestRuntime;
                if ("p2Installed".equals(testRuntime)) {
                    equinoxTestRuntime = createProvisionedInstallation();
                } else if ("default".equals(testRuntime)) {
                    equinoxTestRuntime = createEclipseInstallation();
                } else {
                    throw new MojoExecutionException("Configured testRuntime parameter value '" + testRuntime
                            + "' is unkown. Allowed values: 'default', 'p2Installed'.");
                }

                runTest(equinoxTestRuntime);
            }
        }
    }

    protected abstract boolean shouldRun();

    protected boolean shouldSkip() {
        if (skip != null && skipTests != null && !skip.equals(skipTests)) {
            getLog().warn(
                    "Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority!");
        }
        if (skipTests != null) {
            return skipTests;
        }
        if (skip != null) {
            return skip;
        }
        return skipExec;
    }

    private ReactorProject getReactorProject() {
        return DefaultReactorProject.adapt(project);
    }

    private List<ReactorProject> getReactorProjects() {
        return DefaultReactorProject.adapt(session);
    }

    private EquinoxInstallation createProvisionedInstallation() throws MojoExecutionException {
        try {
            TestFrameworkProvider provider = providerHelper.selectProvider(
                    getProjectType().getClasspath(DefaultReactorProject.adapt(project)), getMergedProviderProperties(),
                    providerHint);
            PropertiesWrapper wrapper = createSurefireProperties(provider);
            storeProperties(wrapper.getProperties(), surefireProperties);

            ProvisionedInstallationBuilder installationBuilder = provisionedInstallationBuilderFactory
                    .createInstallationBuilder();
            Set<Artifact> testHarnessArtifacts = providerHelper.filterTestFrameworkBundles(provider, pluginArtifacts);
            for (Artifact testHarnessArtifact : testHarnessArtifacts) {
                installationBuilder.addBundleJar(testHarnessArtifact.getFile());
            }
            RepositoryReferences sources = repositoryReferenceTool.getVisibleRepositories(project, session,
                    RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE);
            installationBuilder.addMetadataRepositories(sources.getMetadataRepositories());
            installationBuilder.addArtifactRepositories(sources.getArtifactRepositories());
            installationBuilder.setProfileName(profileName);
            installationBuilder.addIUsToBeInstalled(getIUsToInstall(testHarnessArtifacts));
            File workingDir = new File(project.getBuild().getDirectory(), "p2temp");
            workingDir.mkdirs();
            installationBuilder.setWorkingDir(workingDir);
            installationBuilder.setDestination(work);
            return installationBuilder.install();
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private List<String> getIUsToInstall(Set<Artifact> testHarnessArtifacts) {
        List<String> iusToInstall = new ArrayList<>();
        // 1. test bundle
        iusToInstall.add(getTestBundleSymbolicName());
        // 2. test harness bundles
        iusToInstall.addAll(providerHelper.getSymbolicNames(testHarnessArtifacts));
        // 3. extra dependencies
        for (Dependency extraDependency : TychoProjectUtils
                .getTargetPlatformConfiguration(DefaultReactorProject.adapt(project))
                .getDependencyResolverConfiguration().getExtraRequirements()) {
            String type = extraDependency.getType();
            if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
                iusToInstall.add(extraDependency.getArtifactId());
            } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
                iusToInstall.add(extraDependency.getArtifactId() + ".feature.group");
            }
        }
        // 4. test dependencies
        osgiBundle.getTestDependencyArtifacts(getReactorProject()).getArtifacts().stream()
                .map(desc -> desc.getKey().getId()).forEach(iusToInstall::add);
        return iusToInstall;
    }

    private BundleProject getProjectType() {
        return (BundleProject) projectTypes.get(project.getPackaging());
    }

    private EquinoxInstallation createEclipseInstallation() throws MojoExecutionException {
        DependencyResolver platformResolver = dependencyResolverLocator.lookupDependencyResolver(project);
        final List<Dependency> extraDependencies = getExtraDependencies();
        List<ReactorProject> reactorProjects = getReactorProjects();

        final DependencyResolverConfiguration resolverConfiguration = new DependencyResolverConfiguration() {
            @Override
            public OptionalResolutionAction getOptionalResolutionAction() {
                return OptionalResolutionAction.IGNORE;
            }

            @Override
            public List<Dependency> getExtraRequirements() {
                return extraDependencies;
            }
        };

        DependencyArtifacts testRuntimeArtifacts = platformResolver.resolveDependencies(session, project, null,
                reactorProjects, resolverConfiguration);

        if (testRuntimeArtifacts == null) {
            throw new MojoExecutionException(
                    "Cannot determinate build target platform location -- not executing tests");
        }

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

        TestFrameworkProvider provider = providerHelper.selectProvider(
                getProjectType().getClasspath(DefaultReactorProject.adapt(project)), getMergedProviderProperties(),
                providerHint);
        PropertiesWrapper wrapper = createSurefireProperties(provider);
        storeProperties(wrapper.getProperties(), surefireProperties);
        for (ArtifactDescriptor artifact : testRuntimeArtifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN)) {
            // note that this project is added as directory structure rooted at project basedir.
            // project classes and test-classes are added via dev.properties file (see #createDevProperties())
            // all other projects are added as bundle jars.
            ReactorProject otherProject = artifact.getMavenProject();
            if (otherProject != null) {
                if (otherProject.sameProject(project)) {
                    testRuntime.addBundle(artifact.getKey(), project.getBasedir());
                    continue;
                }
                File file = otherProject.getArtifact(artifact.getClassifier());
                if (file != null) {
                    testRuntime.addBundle(artifact.getKey(), file);
                    continue;
                }
            }
            testRuntime.addBundle(artifact);
        }
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (ArtifactType.TYPE_ECLIPSE_TEST_FRAGMENT.equals(artifact.getClassifier())) {
                DefaultArtifactKey key = new DefaultArtifactKey(artifact.getClassifier(), artifact.getId(),
                        artifact.getVersion());
                testRuntime.addBundle(key, artifact.getFile());
            }
        }

        Set<Artifact> testFrameworkBundles = providerHelper.filterTestFrameworkBundles(provider, pluginArtifacts);
        for (Artifact artifact : testFrameworkBundles) {
            DevBundleInfo devInfo = workspaceState.getBundleInfo(session, artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), project.getPluginArtifactRepositories());
            if (devInfo != null) {
                testRuntime.addBundle(devInfo.getArtifactKey(), devInfo.getLocation(), true);
                testRuntime.addDevEntries(devInfo.getSymbolicName(), devInfo.getDevEntries());
            } else {
                File bundleLocation = artifact.getFile();
                ArtifactKey bundleArtifactKey = getBundleArtifactKey(bundleLocation);
                testRuntime.addBundle(bundleArtifactKey, bundleLocation, true);
            }
        }

        testRuntime.addDevEntries(getTestBundleSymbolicName(), getBuildOutputDirectories());

        getReportsDirectory().mkdirs();
        return installationFactory.createInstallation(testRuntime, work);
    }

    private List<Dependency> getExtraDependencies() {
        final List<Dependency> dependencies = new ArrayList<>();
        if (this.dependencies != null) {
            dependencies.addAll(Arrays.asList(this.dependencies));
        }
        TargetPlatformConfiguration configuration = TychoProjectUtils
                .getTargetPlatformConfiguration(DefaultReactorProject.adapt(project));
        dependencies.addAll(configuration.getDependencyResolverConfiguration().getExtraRequirements());
        dependencies.addAll(getTestDependencies());
        return dependencies;
    }

    private String getTestBundleSymbolicName() {
        return getProjectType().getArtifactKey(getReactorProject()).getId();
    }

    private ArtifactKey getBundleArtifactKey(File file) throws MojoExecutionException {
        ArtifactKey key = osgiBundle.readArtifactKey(file);
        if (key == null) {
            throw new MojoExecutionException("Not an OSGi bundle " + file.getAbsolutePath());
        }
        return key;
    }

    private List<Dependency> getTestDependencies() {
        ArrayList<Dependency> result = new ArrayList<>();

        // see also P2ResolverImpl.addDependenciesForTests()
        result.add(newBundleDependency("org.eclipse.osgi"));
        result.add(newBundleDependency(EquinoxInstallationDescription.EQUINOX_LAUNCHER));
        if (useUIHarness) {
            result.add(newBundleDependency("org.eclipse.ui.ide.application"));
        } else {
            result.add(newBundleDependency("org.eclipse.core.runtime"));
        }

        return result;
    }

    protected Dependency newBundleDependency(String bundleId) {
        Dependency ideapp = new Dependency();
        ideapp.setArtifactId(bundleId);
        ideapp.setType(ArtifactType.TYPE_ECLIPSE_PLUGIN);
        return ideapp;
    }

    protected PropertiesWrapper createSurefireProperties(TestFrameworkProvider provider) throws MojoExecutionException {
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
        Properties mergedProviderProperties = getMergedProviderProperties();
        mergedProviderProperties.putAll(provider.getProviderSpecificProperties());
        ScanResult scanResult = scanForTests();
        Map<String, String> providerPropertiesAsMap = propertiesAsMap(mergedProviderProperties);
        scanResult.writeTo(providerPropertiesAsMap);
        for (Map.Entry<String, String> entry : providerPropertiesAsMap.entrySet()) {
            wrapper.setProperty("__provider." + entry.getKey(), entry.getValue().toString());
        }
        wrapper.setProperty("testprovider", provider.getSurefireProviderClassName());
        getLog().debug("Using test framework provider " + provider.getClass().getName());
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

    protected ScanResult scanForTests() {
        List<String> defaultIncludes = getDefaultInclude();
        List<String> defaultExcludes = getDefaultExclude();
        List<String> includeList;
        List<String> excludeList;
        if (test != null) {
            String test = this.test;
            test = test.replace('.', '/');
            test = test.endsWith(".class") ? test : test + ".class";
            test = test.startsWith("**/") ? test : "**/" + test;
            includeList = Collections.singletonList(test);
        } else if (testClass != null) {
            includeList = Collections.singletonList(testClass.replace('.', '/') + ".class");
        } else if (includes != null) {
            includeList = includes;
            includeList.removeAll(Collections.singleton(null));
        } else {
            includeList = defaultIncludes;
        }
        if (excludes != null) {
            excludeList = excludes;
            excludeList.removeAll(Collections.singleton(null));
        } else {
            excludeList = defaultExcludes;
        }
        // TODO bug 495353 we should we rather let TestListResolver do the work here
        // by passing in the unparsed String or Strings instead of already parsed include/exclude list
        // (this would add support for running single test methods, negation etc.)
        TestListResolver resolver = new TestListResolver(includeList, excludeList);
        DirectoryScanner scanner = new DirectoryScanner(getTestClassesDirectory(), resolver);
        DefaultScanResult scanResult = scanner.scan();
        return scanResult;
    }

    protected List<String> getDefaultExclude() {
        return Arrays.asList("**/*$*");
    }

    protected abstract List<String> getDefaultInclude();

    private void storeProperties(Map<String, String> propertiesMap, File file) throws MojoExecutionException {
        Properties p = new Properties();
        p.putAll(propertiesMap);
        try {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                p.store(out, null);
            }
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
            getLog().info("Executing Test Runtime with timeout " + forkedProcessTimeoutInSeconds
                    + ", logs (if any) will be placed at: " + logFile.getAbsolutePath());
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

    protected abstract void handleNoTestsFound() throws MojoFailureException;

    private String decodeReturnCode(int result) {
        try {
            Properties properties = (Properties) DefaultReactorProject.adapt(project)
                    .getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
            if (PlatformPropertiesUtils.OS_LINUX.equals(PlatformPropertiesUtils.getOS(properties))) {
                int signal = result - 128;
                if (signal > 0 && signal < UNIX_SIGNAL_NAMES.length) {
                    return result + "(" + UNIX_SIGNAL_NAMES[signal] + " received?)";
                }
            } else if (PlatformPropertiesUtils.OS_WIN32.equals(PlatformPropertiesUtils.getOS(properties))) {
                return result + " (HRESULT Code 0x" + Integer.toHexString(result).toUpperCase()
                        + ", check for example https://www.hresult.info/ for further details)";
            }
        } catch (RuntimeException e) {
            getLog().debug("Decoding returncode failed", e);
        }
        return String.valueOf(result);
    }

    protected Toolchain getToolchain() throws MojoExecutionException {
        if (JDKUsage.SYSTEM.equals(useJDK)) {
            if (toolchainManager != null) {
                return toolchainManager.getToolchainFromBuildContext("jdk", session);
            }
            return null;
        }
        String profileName = TychoProjectUtils
                .getExecutionEnvironmentConfiguration(DefaultReactorProject.adapt(project)).getProfileName();
        Toolchain toolChain = toolchainProvider.findMatchingJavaToolChain(session, profileName);
        if (toolChain == null) {
            throw new MojoExecutionException("useJDK = BREE configured, but no toolchain of type 'jdk' with id '"
                    + profileName + "' found. See http://maven.apache.org/guides/mini/guide-using-toolchains.html");
        }
        return toolChain;
    }

    private EquinoxLaunchConfiguration createCommandLine(EquinoxInstallation testRuntime)
            throws MalformedURLException, MojoExecutionException {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(testRuntime);

        String executable = null;
        Toolchain tc = getToolchain();
        if (tc != null) {
            getLog().info("Toolchain in tycho-surefire-plugin: " + tc);
            executable = tc.findTool("java");
        }
        cli.setJvmExecutable(executable);

        cli.setWorkingDirectory(project.getBasedir());

        if (debugPort > 0) {
            cli.addVMArguments("-Xdebug", "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y");
        }

        cli.addVMArguments("-Dosgi.noShutdown=false");

        Properties properties = (Properties) DefaultReactorProject.adapt(project)
                .getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
        cli.addVMArguments("-Dosgi.os=" + PlatformPropertiesUtils.getOS(properties), //
                "-Dosgi.ws=" + PlatformPropertiesUtils.getWS(properties), //
                "-Dosgi.arch=" + PlatformPropertiesUtils.getArch(properties));
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
                "-application", getTestApplication(testRuntime.getInstallationDescription()), //
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
        ExecutionEnvironmentConfiguration eeConfig = TychoProjectUtils
                .getExecutionEnvironmentConfiguration(DefaultReactorProject.adapt(project));
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

    private String getTestApplication(EquinoxInstallationDescription testRuntime) {
        if (useUIHarness) {
            return "org.eclipse.tycho.surefire.osgibooter.uitest";
        } else {
            return "org.eclipse.tycho.surefire.osgibooter.headlesstest";
        }
    }

    private String getBuildOutputDirectories() {
        StringBuilder sb = new StringBuilder();
        ReactorProject reactorProject = getReactorProject();
        BuildDirectory buildDirectory = reactorProject.getBuildDirectory();
        sb.append(buildDirectory.getOutputDirectory());
        sb.append(',').append(buildDirectory.getTestOutputDirectory());
        for (BuildOutputJar outputJar : osgiBundle.getEclipsePluginProject(reactorProject).getOutputJars()) {
            if (".".equals(outputJar.getName())) {
                // handled above
                continue;
            }
            appendCommaSeparated(sb, outputJar.getOutputDirectory().getAbsolutePath());
        }
        return sb.toString();
    }

    private static void appendCommaSeparated(StringBuilder sb, String string) {
        if (sb.length() > 0) {
            sb.append(',');
        }
        sb.append(string);
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

    protected abstract File getTestClassesDirectory();

    protected abstract File getReportsDirectory();

}
