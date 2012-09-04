/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - port to surefire 2.10
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
     * <p>
     * Root directory (<a href=
     * "http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstallarea"
     * >osgi.install.area</a>) of the Equinox runtime used to execute tests.
     * </p>
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
     * <p>
     * Set this parameter to suspend the test JVM waiting for a client to open a remote debug
     * session on the specified port.
     * </p>
     */
    @Parameter(property = "debugPort")
    private int debugPort;

    /**
     * <p>
     * List of patterns (separated by commas) used to specify the tests that should be included in
     * testing. When not specified and whent the <code>test</code> parameter is not specified, the
     * default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
     * </p>
     */
    @Parameter
    private List<String> includes;

    /**
     * <p>
     * List of patterns (separated by commas) used to specify the tests that should be excluded in
     * testing. When not specified and when the <code>test</code> parameter is not specified, the
     * default excludes will be <code>**&#47;*$*</code> (which excludes all inner classes).
     * </p>
     */
    @Parameter
    private List<String> excludes;

    /**
     * <p>
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern
     * matching, to select tests to run. The Ant pattern will be used to create an include pattern
     * formatted like <code>**&#47;${test}.java</code> When used, the <code>includes</code> and
     * <code>excludes</code> patterns parameters are ignored
     * </p>
     */
    @Parameter(property = "test")
    private String test;

    /**
     * @deprecated Use skipTests instead.
     */
    @Parameter(property = "maven.test.skipExec", defaultValue = "false")
    private boolean skipExec;

    /**
     * <p>
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED,
     * but quite convenient on occasion.
     * </p>
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    /**
     * <p>
     * Same as {@link #skipTests}
     * </p>
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    /**
     * <p>
     * If set to "false" the test execution will not fail in case there are no tests found.
     * </p>
     */
    @Parameter(property = "failIfNoTests", defaultValue = "true")
    private boolean failIfNoTests;

    /**
     * <p>
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     * </p>
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    private boolean testFailureIgnore;

    /**
     * <p>
     * The directory containing generated test classes of the project being tested.
     * </p>
     */
    @Parameter(property = "project.build.outputDirectory")
    private File testClassesDirectory;

    /**
     * <p>
     * Enables -debug -consolelog for the test OSGi runtime
     * </p>
     */
    @Parameter(property = "tycho.showEclipseLog", defaultValue = "false")
    private boolean showEclipseLog;

    /**
     * <p>
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     * </p>
     */
    @Parameter(property = "maven.test.redirectTestOutputToFile", defaultValue = "false")
    private boolean redirectTestOutputToFile;

    /**
     * <p>
     * Base directory where all reports are written to.
     * </p>
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
    private File reportsDirectory;

    @Parameter(defaultValue = "${project.build.directory}/surefire.properties")
    private File surefireProperties;

    /**
     * <p>
     * Additional dependencies to be added to the test runtime.
     * </p>
     * <p>
     * The dependencies specified here are &ndash; together with the dependencies specified in the
     * <tt>MANIFEST.MF</tt> of the project &ndash; resolved against the target platform. The
     * resulting set of bundles is included in the test runtime. Ignored if {@link #testRuntime} is
     * <code>p2Installed</code>.
     * </p>
     */
    @Parameter
    private Dependency[] dependencies;

    /**
     * <p>
     * Eclipse application to be run. If not specified, default application
     * org.eclipse.ui.ide.workbench will be used. Application runnable will be invoked from test
     * harness, not directly from Eclipse.
     * </p>
     */
    @Parameter
    private String application;

    /**
     * <p>
     * Eclipse product to be run, i.e. -product parameter passed to test Eclipse runtime.
     * </p>
     */
    @Parameter
    private String product;

    @Parameter(property = "session", readonly = true, required = true)
    private MavenSession session;

    /**
     * <p>
     * Run tests using UI (true) or headless (false) test harness.
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean useUIHarness;

    /**
     * <p>
     * Run tests in UI (true) or background (false) thread. Only applies to UI test harness.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean useUIThread;

    @Parameter(property = "plugin.artifacts")
    private List<Artifact> pluginArtifacts;

    /**
     * <p>
     * Arbitrary JVM options to set on the command line.
     * </p>
     */
    @Parameter(property = "tycho.testArgLine")
    private String argLine;

    /**
     * <p>
     * Arbitrary applications arguments to set on the command line.
     * </p>
     */
    @Parameter
    private String appArgLine;

    /**
     * <p>
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     * </p>
     */
    @Parameter(property = "surefire.timeout")
    private int forkedProcessTimeoutInSeconds;

    /**
     * <p>
     * Bundle-SymbolicName of the test suite, a special bundle that knows how to locate and execute
     * all relevant tests.
     * </p>
     * 
     * <p>
     * testSuite and testClass identify single test class to run. All other tests will be ignored if
     * both testSuite and testClass are provided. It is an error if provide one of the two
     * parameters but not the other.
     * </p>
     */
    @Parameter(property = "testSuite")
    private String testSuite;

    /**
     * <p>
     * See testSuite
     * </p>
     */
    @Parameter(property = "testClass")
    private String testClass;

    /**
     * <p>
     * Additional environments to set for the forked test JVM.
     * </p>
     */
    @Parameter
    private Map<String, String> environmentVariables;

    /**
     * <p>
     * Additional system properties to set for the forked test JVM.
     * </p>
     */
    @Parameter
    private Map<String, String> systemProperties;

    /**
     * <p>
     * List of bundles that must be expanded in order to execute the tests. Ignored if
     * {@link #testRuntime} is <code>p2Installed</code>.
     * </p>
     */
    @Parameter
    private String[] explodedBundles;

    /**
     * <p>
     * List of framework extension bundles to add. Note: The goal does not automatically detect
     * which bundles in the test runtime are framework extensions, but they have to be explicitly
     * specified using this parameter. Ignored if {@link #testRuntime} is <code>p2Installed</code>.
     * </p>
     */
    @Parameter
    private Dependency[] frameworkExtensions;

    /**
     * <p>
     * Bundle start level and auto start configuration used by the test runtime. Ignored if
     * {@link #testRuntime} is <code>p2Installed</code>.
     * </p>
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
     * <p>
     * Normally tycho will automatically determine the test framework provider based on the test
     * project's classpath. Use this to force using a test framework provider implementation with
     * the given role hint. Tycho comes with providers
     * &quot;junit3&quot;,&quot;junit4&quot;,&quot;junit47&quot;. Note that when specifying a
     * providerHint, you have to make sure the provider is actually available in the dependencies of
     * tycho-surefire-plugin.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter
    private String providerHint;

    /**
     * <p>
     * Defines the order the tests will be run in. Supported values are "alphabetical",
     * "reversealphabetical", "random", "hourly" (alphabetical on even hours, reverse alphabetical
     * on odd hours) and "filesystem".
     * </p>
     * 
     * @since 0.19.0
     */
    @Parameter(defaultValue = "filesystem")
    private String runOrder;

    /**
     * <p>
     * (JUnit 4.7 provider) Supports values "classes"/"methods"/"both" to run in separate threads,
     * as controlled by threadCount.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter(property = "parallel")
    private ParallelMode parallel;

    /**
     * <p>
     * (JUnit 4.7 provider) Indicates that threadCount is per cpu core.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter(property = "perCoreThreadCount", defaultValue = "true")
    private boolean perCoreThreadCount;

    /**
     * <p>
     * (JUnit 4.7 provider) The attribute thread-count allows you to specify how many threads should
     * be allocated for this execution. Only makes sense to use in conjunction with the parallel
     * parameter.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter(property = "threadCount")
    private int threadCount = -1;

    /**
     * <p>
     * (JUnit 4.7 provider) Indicates that the thread pool will be unlimited. The parallel parameter
     * and the actual number of classes/methods will decide. Setting this to "true" effectively
     * disables perCoreThreadCount and threadCount.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter(property = "useUnlimitedThreads", defaultValue = "false")
    private boolean useUnlimitedThreads;

    /**
     * <p>
     * Use this to specify surefire provider-specific properties.
     * </p>
     * 
     * @since 0.16.0
     */
    @Parameter
    private Properties providerProperties = new Properties();

    /**
     * <p>
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
     * </p>
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipExec || skipTests) {
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
            public OptionalResolutionAction getOptionalResolutionAction() {
                return OptionalResolutionAction.IGNORE;
            }

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
        for (Map.Entry entry : getMergedProviderProperties().entrySet()) {
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
        scanResult.writeTo(result);
        return result;
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

    private String getIncludesExcludes(List<String> patterns) {
        StringBuilder sb = new StringBuilder();
        for (String pattern : patterns) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pattern);
        }
        return sb.toString();
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
            throw new MojoFailureException("An unexpected error occured (return code " + result
                    + "). See log for details.");
        }
    }

    private Toolchain getToolchain() {
        Toolchain tc = null;
        if (toolchainManager != null) {
            tc = toolchainManager.getToolchainFromBuildContext("jdk", session);
        }
        return tc;
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
            try {
                cli.addVMArguments("-D" + EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE + "=" + profileFile.toURL());
            } catch (MalformedURLException e) {
                // should not happen
                throw new RuntimeException(e);
            }
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
