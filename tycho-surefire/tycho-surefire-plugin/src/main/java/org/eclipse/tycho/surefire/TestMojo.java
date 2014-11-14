/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - port to surefire 2.10
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.booter.ProviderParameterNames;
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
import org.osgi.framework.Version;

/**
 * <p>
 * Executes tests in an OSGi runtime.
 * </p>
 * <p>
 * The goal launches an OSGi runtime and executes the project's tests in that runtime. The "test
 * runtime" consists of the bundle built in this project and its transitive dependencies, plus some
 * Equinox and test harness bundles. The bundles are resolved from the target platform of the
 * project. Note that the test runtime does typically <em>not</em> contain the entire target
 * platform. If there are implicitly required bundles (e.g. <tt>org.eclipse.equinox.ds</tt> to make
 * declarative services work), they need to be added manually through an explicit
 * <tt>dependencies</tt> configuration.
 * </p>
 * 
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TestMojo extends AbstractMojo {

    /**
     * Root directory (<a href=
     * "http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstallarea"
     * >osgi.install.area</a>) of the Equinox runtime used to execute tests.
     */
    @Parameter(defaultValue = "${project.build.directory}/work")
    private File work;

    /**
     * <a href=
     * "http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstancearea"
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
    private MavenProject project;

    /**
     * Set this parameter to suspend the test JVM waiting for a client to open a remote debug
     * session on the specified port.
     */
    @Parameter(property = "debugPort")
    private int debugPort;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in
     * testing. When not specified and whent the <code>test</code> parameter is not specified, the
     * default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
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

    /**
     * The directory containing generated test classes of the project being tested.
     */
    @Parameter(property = "project.build.outputDirectory")
    private File testClassesDirectory;

    /**
     * Enables -debug -consolelog for the test OSGi runtime
     */
    @Parameter(property = "tycho.showEclipseLog", defaultValue = "false")
    private boolean showEclipseLog;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     */
    @Parameter(property = "maven.test.redirectTestOutputToFile", defaultValue = "false")
    private boolean redirectTestOutputToFile;

    /**
     * Base directory where all reports are written to.
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
    private File reportsDirectory;

    @Parameter(defaultValue = "${project.build.directory}/surefire.properties")
    private File surefireProperties;

    /**
     * Additional dependencies to be added to the test runtime.
     * 
     * The dependencies specified here are &ndash; together with the dependencies specified in the
     * <tt>MANIFEST.MF</tt> of the project &ndash; resolved against the target platform. The
     * resulting set of bundles is included in the test runtime. Ignored if {@link #testRuntime} is
     * <code>p2Installed</code>.
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
     * Bundle-SymbolicName of the test suite, a special bundle that knows how to locate and execute
     * all relevant tests.
     * 
     * testSuite and testClass identify single test class to run. All other tests will be ignored if
     * both testSuite and testClass are provided. It is an error if provide one of the two
     * parameters but not the other.
     */
    @Parameter(property = "testSuite")
    private String testSuite;

    /**
     * See testSuite
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
     * {@link #testRuntime} is <code>p2Installed</code>.
     */
    @Parameter
    private BundleStartLevel[] bundleStartLevel;

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
     * &quot;junit3&quot;,&quot;junit4&quot;,&quot;junit47&quot;. Note that when specifying a
     * providerHint, you have to make sure the provider is actually available in the dependencies of
     * tycho-surefire-plugin.
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
     *     &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *     &lt;artifactId&gt;tycho-surefire-plugin&lt;/artifactId&gt;
     *     &lt;version&gt;${tycho-version}&lt;/version&gt;
     *     &lt;configuration&gt;
     *         &lt;testRuntime&gt;p2Installed&lt;/testRuntime&gt;
     *     &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * &lt;plugin&gt;
     *     &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *     &lt;artifactId&gt;target-platform-configuration&lt;/artifactId&gt;
     *     &lt;version&gt;${tycho-version}&lt;/version&gt;
     *     &lt;configuration&gt;
     *         &lt;dependency-resolution&gt;
     *             &lt;extraRequirements&gt;
     *                 &lt;!-- product IU under test --&gt;
     *                 &lt;requirement&gt;
     *                     &lt;type&gt;p2-installable-unit&lt;/type&gt;
     *                     &lt;id&gt;example.product.id&lt;/id&gt;
     *                     &lt;versionRange&gt;0.0.0&lt;/versionRange&gt;
     *                 &lt;/requirement&gt;
     *             &lt;/extraRequirements&gt;
     *         &lt;/dependency-resolution&gt;
     *     &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     * 
     * @since 0.19.0
     */
    @Parameter(defaultValue = "default")
    private String testRuntime;

    /**
     * p2 <a href=
     * "http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html"
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
     * <li>SYSTEM: Use the currently running JVM (or from <a
     * href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchain</a> if
     * configured in pom.xml)</li>
     * <li>BREE: use MANIFEST header <code>Bundle-RequiredExecutionEnvironment</code> to lookup the
     * JDK from <a
     * href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchains.xml</a>.
     * The value of BREE will be matched against the id of the toolchain elements in toolchains.xml.
     * </li>
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
     *   &lt;toolchain&gt;
     *      &lt;type&gt;jdk&lt;/type&gt;
     *      &lt;provides&gt;
     *          &lt;id&gt;JavaSE-1.7&lt;/id&gt;
     *      &lt;/provides&gt;
     *      &lt;configuration&gt;
     *         &lt;jdkHome&gt;/path/to/jdk/1.7&lt;/jdkHome&gt;
     *      &lt;/configuration&gt;
     *   &lt;/toolchain&gt;
     * &lt;/toolchains&gt;
     * </pre>
     */
    @Parameter(defaultValue = "SYSTEM")
    private JDKUsage useJDK;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (shouldSkip()) {
            getLog().info("Skipping tests");
            return;
        }

        if (!"eclipse-test-plugin".equals(project.getPackaging())) {
            getLog().warn("Unsupported packaging type " + project.getPackaging());
            return;
        }

        if (testSuite != null || testClass != null) {
            if (testSuite == null || testClass == null) {
                throw new MojoExecutionException("Both testSuite and testClass must be provided or both should be null");
            }

            MavenProject suite = getTestSuite(testSuite);

            if (suite == null) {
                throw new MojoExecutionException("Cannot find test suite project with Bundle-SymbolicName " + testSuite);
            }

            if (!suite.equals(project)) {
                getLog().info("Not executing tests, testSuite=" + testSuite + " and project is not the testSuite");
                return;
            }
        }

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
            TestFrameworkProvider provider = providerHelper.selectProvider(getProjectType().getClasspath(project),
                    getMergedProviderProperties(), providerHint);
            createSurefireProperties(provider);

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
        List<String> iusToInstall = new ArrayList<String>();
        // 1. test bundle
        iusToInstall.add(getTestBundleSymbolicName());
        // 2. test harness bundles
        iusToInstall.addAll(providerHelper.getSymbolicNames(testHarnessArtifacts));
        // 3. extra dependencies
        for (Dependency extraDependency : TychoProjectUtils.getTargetPlatformConfiguration(project)
                .getDependencyResolverConfiguration().getExtraRequirements()) {
            String type = extraDependency.getType();
            if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
                iusToInstall.add(extraDependency.getArtifactId());
            } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
                iusToInstall.add(extraDependency.getArtifactId() + ".feature.group");
            }
        }
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
            throw new MojoExecutionException("Cannot determinate build target platform location -- not executing tests");
        }

        work.mkdirs();

        EquinoxInstallationDescription testRuntime = new DefaultEquinoxInstallationDescription();
        testRuntime.addBundlesToExplode(getBundlesToExplode());
        testRuntime.addFrameworkExtensions(getFrameworkExtensions());
        if (bundleStartLevel != null) {
            for (BundleStartLevel level : bundleStartLevel) {
                testRuntime.addBundleStartLevel(level);
            }
        }

        TestFrameworkProvider provider = providerHelper.selectProvider(getProjectType().getClasspath(project),
                getMergedProviderProperties(), providerHint);
        createSurefireProperties(provider);
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

        reportsDirectory.mkdirs();
        return installationFactory.createInstallation(testRuntime, work);
    }

    private List<Dependency> getExtraDependencies() {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        if (this.dependencies != null) {
            dependencies.addAll(Arrays.asList(this.dependencies));
        }
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
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

    private MavenProject getTestSuite(String symbolicName) {
        for (MavenProject otherProject : session.getProjects()) {
            TychoProject projectType = projectTypes.get(otherProject.getPackaging());
            if (projectType != null
                    && projectType.getArtifactKey(DefaultReactorProject.adapt(otherProject)).getId()
                            .equals(symbolicName)) {
                return otherProject;
            }
        }
        return null;
    }

    private List<Dependency> getTestDependencies() {
        ArrayList<Dependency> result = new ArrayList<Dependency>();

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

    private void createSurefireProperties(TestFrameworkProvider provider) throws MojoExecutionException {
        Properties p = new Properties();
        p.put("testpluginname", getTestBundleSymbolicName());
        p.put("testclassesdirectory", testClassesDirectory.getAbsolutePath());
        p.put("reportsdirectory", reportsDirectory.getAbsolutePath());
        p.put("redirectTestOutputToFile", String.valueOf(redirectTestOutputToFile));

        p.put("failifnotests", String.valueOf(failIfNoTests));
        p.put("runOrder", runOrder);
        Properties mergedProviderProperties = getMergedProviderProperties();
        ScanResult scanResult = scanForTests();
        scanResult.writeTo(mergedProviderProperties);
        for (Map.Entry<?, ?> entry : mergedProviderProperties.entrySet()) {
            p.put("__provider." + entry.getKey(), entry.getValue());
        }
        p.setProperty("testprovider", provider.getSurefireProviderClassName());
        getLog().debug("Using test framework provider " + provider.getClass().getName());
        storeProperties(p, surefireProperties);
    }

    private Properties getMergedProviderProperties() {
        Properties result = new Properties();
        result.putAll(providerProperties);
        if (parallel != null) {
            result.put(ProviderParameterNames.PARALLEL_PROP, parallel.name());
            if (threadCount > 0) {
                result.put(ProviderParameterNames.THREADCOUNT_PROP, String.valueOf(threadCount));
            }
            result.put(/* JUnitCoreParameters.PERCORETHREADCOUNT_KEY */"perCoreThreadCount",
                    String.valueOf(perCoreThreadCount));
            result.put(/* JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY */"useUnlimitedThreads",
                    String.valueOf(useUnlimitedThreads));
        }
        return result;
    }

    private ScanResult scanForTests() {
        List<String> defaultIncludes = Arrays.asList("**/Test*.class", "**/*Test.class", "**/*TestCase.class");
        List<String> defaultExcludes = Arrays.asList("**/*$*");
        List<String> includeList;
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
        } else {
            includeList = defaultIncludes;
        }
        DirectoryScanner scanner = new DirectoryScanner(testClassesDirectory, includeList, excludes != null ? excludes
                : defaultExcludes, Collections.<String> emptyList());
        DefaultScanResult scanResult = scanner.scan();
        return scanResult;
    }

    private void storeProperties(Properties p, File file) throws MojoExecutionException {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            try {
                p.store(out, null);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write test launcher properties file", e);
        }
    }

    private void runTest(EquinoxInstallation testRuntime) throws MojoExecutionException, MojoFailureException {
        int result;
        try {
            if (deleteOsgiDataDirectory) {
                FileUtils.deleteDirectory(osgiDataDirectory);
            }
            LaunchConfiguration cli = createCommandLine(testRuntime);
            getLog().info(
                    "Expected eclipse log file: " + new File(osgiDataDirectory, ".metadata/.log").getAbsolutePath());
            result = launcher.execute(cli, forkedProcessTimeoutInSeconds);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while executing platform", e);
        }
        switch (result) {
        case 0:
            getLog().info("All tests passed!");
            break;

        case 200: /* see AbstractUITestApplication */
            if (application == null) {
                // the extra test dependencies should prevent this case
                throw new MojoExecutionException(
                        "Could not find the default application \"org.eclipse.ui.ide.workbench\" in the test runtime.");
            } else {
                throw new MojoFailureException("Could not find application \"" + application + "\" in test runtime. "
                        + "Make sure that the test runtime includes the bundle which defines this application.");
            }

        case 254/* RunResult.NO_TESTS */:
            String message = "No tests found.";
            if (failIfNoTests) {
                throw new MojoFailureException(message);
            } else {
                getLog().warn(message);
            }
            break;

        case 255/* RunResult.FAILURE */:
            String errorMessage = "There are test failures.\n\nPlease refer to " + reportsDirectory
                    + " for the individual test results.";
            if (testFailureIgnore) {
                getLog().error(errorMessage);
            } else {
                throw new MojoFailureException(errorMessage);
            }
            break;

        default:
            throw new MojoFailureException("An unexpected error occured while launching the test runtime (return code "
                    + result + "). See log for details.");
        }
    }

    protected Toolchain getToolchain() throws MojoExecutionException {
        if (JDKUsage.SYSTEM.equals(useJDK)) {
            if (toolchainManager != null) {
                return toolchainManager.getToolchainFromBuildContext("jdk", session);
            }
            return null;
        }
        String profileName = TychoProjectUtils.getExecutionEnvironmentConfiguration(project).getProfileName();
        Toolchain toolChain = toolchainProvider.findMatchingJavaToolChain(session, profileName);
        if (toolChain == null) {
            throw new MojoExecutionException("useJDK = BREE configured, but no toolchain of type 'jdk' with id '"
                    + profileName + "' found. See http://maven.apache.org/guides/mini/guide-using-toolchains.html");
        }
        return toolChain;
    }

    LaunchConfiguration createCommandLine(EquinoxInstallation testRuntime) throws MalformedURLException,
            MojoExecutionException {
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

        Properties properties = (Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
        cli.addVMArguments("-Dosgi.os=" + PlatformPropertiesUtils.getOS(properties), //
                "-Dosgi.ws=" + PlatformPropertiesUtils.getWS(properties), //
                "-Dosgi.arch=" + PlatformPropertiesUtils.getArch(properties));
        addCustomProfileArg(cli);
        cli.addVMArguments(splitArgLine(argLine));

        for (Map.Entry<String, String> entry : getMergedSystemProperties().entrySet()) {
            cli.addVMArguments("-D" + entry.getKey() + "=" + entry.getValue());
        }

        if (getLog().isDebugEnabled() || showEclipseLog) {
            cli.addProgramArguments("-debug", "-consolelog");
        }

        addProgramArgs(cli, "-data", osgiDataDirectory.getAbsolutePath(), //
                "-install", testRuntime.getLocation().getAbsolutePath(), //
                "-configuration", new File(work, "configuration").getAbsolutePath(), //
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
        return cli;
    }

    private Map<String, String> getMergedSystemProperties() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        // bug 415489: use osgi.clean=true by default
        result.put("osgi.clean", "true");
        if (systemProperties != null) {
            result.putAll(systemProperties);
        }
        return result;
    }

    private void addCustomProfileArg(EquinoxLaunchConfiguration cli) throws MojoExecutionException {
        ExecutionEnvironmentConfiguration eeConfig = TychoProjectUtils.getExecutionEnvironmentConfiguration(project);
        if (eeConfig.isCustomProfile()) {
            Properties customProfileProps = eeConfig.getFullSpecification().getProfileProperties();
            File profileFile = new File(new File(project.getBuild().getDirectory()), "custom.profile");
            storeProperties(customProfileProps, profileFile);
            cli.addVMArguments("-D" + EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE + "=" + profileFile.toURI());
        }
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
            ArtifactDescriptor systemBundle = testRuntime.getSystemBundle();
            Version osgiVersion = Version.parseVersion(systemBundle.getKey().getVersion());
            if (osgiVersion.compareTo(EquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
                return "org.eclipse.tycho.surefire.osgibooter.uitest32";
            } else {
                return "org.eclipse.tycho.surefire.osgibooter.uitest";
            }
        } else {
            return "org.eclipse.tycho.surefire.osgibooter.headlesstest";
        }
    }

    private String getBuildOutputDirectories() {
        StringBuilder sb = new StringBuilder();
        ReactorProject reactorProject = getReactorProject();
        sb.append(reactorProject.getOutputDirectory());
        sb.append(',').append(reactorProject.getTestOutputDirectory());
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
        List<String> bundles = new ArrayList<String>();

        if (explodedBundles != null) {
            bundles.addAll(Arrays.asList(explodedBundles));
        }

        return bundles;
    }

    private List<File> getFrameworkExtensions() throws MojoExecutionException {
        List<File> files = new ArrayList<File>();

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
                    throw new MojoExecutionException("Failed to resolve framework extension "
                            + frameworkExtension.getManagementKey(), e);
                }
                files.add(artifact.getFile());
            }
        }

        return files;
    }

}
