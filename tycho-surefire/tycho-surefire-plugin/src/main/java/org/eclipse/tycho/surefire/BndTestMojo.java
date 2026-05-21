/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.ScannerFilter;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReproducibleUtils;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.ClasspathReader;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.model.classpath.JUnitBundle;
import org.eclipse.tycho.model.classpath.JUnitClasspathContainerEntry;
import org.eclipse.tycho.surefire.bnd.ArtifactKeyRepository;
import org.eclipse.tycho.surefire.bnd.TargetPlatformRepository;
import org.eclipse.tycho.version.TychoVersion;
import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.conversions.RequirementListConverter;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.strings.Strings;
import biz.aQute.resolve.ResolveProcess;

/**
 * Execute tests using <a href="https://bnd.bndtools.org/chapters/310-testing.html">BND testing</a>
 */
@Mojo(name = BndTestMojo.NAME, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndTestMojo extends AbstractTestMojo {

    public static final String NAME = "bnd-test";
    private static final String ENGINE_VINTAGE_ENGINE = "junit-vintage-engine";
    private static final String ENGINE_JUPITER = "junit-jupiter-engine";
    private static final String ENGINES_DEFAULT = ENGINE_JUPITER + "," + ENGINE_VINTAGE_ENGINE;
    private static final String TESTER_DEFAULT = "biz.aQute.tester";
    private static final String TESTER_JUNIT_PLATFORM = "biz.aQute.tester.junit-platform";
    private static final String FW_FELIX = "org.apache.felix.framework";
    private static final String FW_EQUINOX = "org.eclipse.osgi";
    private static final int ERROR_PREPARE_CONTAINER = -1;
    private static final int ERROR_RESOLVE_CONTAINER = -2;
    private static final MavenArtifactKey FRAMEWORK_EQUINOX = MavenArtifactKey.bundle(FW_EQUINOX, "[3,4)",
            "org.eclipse.platform", "org.eclipse.osgi");
    private static final MavenArtifactKey FRAMEWORK_FELIX = MavenArtifactKey.bundle(FW_FELIX, "1", "org.apache.felix",
            "org.apache.felix.framework");

    private static final String BND_EMBEDDED_REPO_ARTIFACTS_GROUP = "biz.aQute.bnd";
    private static final Set<String> BND_EMBEDDED_REPO_ARTIFACTS = Set.of("biz.aQute.junit", "biz.aQute.launcher",
            "biz.aQute.remote.launcher", "biz.aQute.tester", "biz.aQute.tester.junit-platform");

    @Parameter(property = "tycho.bnd-test.packaging", defaultValue = PackagingType.TYPE_ECLIPSE_PLUGIN)
    private String packaging = PackagingType.TYPE_ECLIPSE_PLUGIN;

    /**
     * If set to true, tracing is enabled for bnd
     */
    @Parameter(property = "tycho.bnd-test.trace", defaultValue = "false")
    private boolean trace;

    /**
     * Enables tracing in the tester
     */
    @Parameter(property = "tycho.bnd-test.testerTrace", defaultValue = "false")
    private boolean testerTrace;

    @Parameter(property = "tycho.bnd-test.printTests", defaultValue = "true")
    private boolean printTests;

    /**
     * Always start bundles with eager activation policy
     */
    @Parameter(property = "tycho.bnd-test.eagerActivation", defaultValue = "false")
    private boolean launchActivationEager;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true)
    private File summaryFile;

    @Parameter()
    private boolean summaryAppend;
    @Parameter()
    private boolean enableSecurity;

    @Parameter
    private List<File> keyStores;

    @Parameter
    private File policyFile;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports", required = true)
    private File reportDirectory;

    /**
     * The directory containing generated test classes of the project being tested.
     */
    @Parameter(property = "project.build.testOutputDirectory")
    private File testClassesDirectory;

    /**
     * Configure the tester to use usually one of
     *
     * <ul>
     * <li>{@value #TESTER_DEFAULT}</li>
     * <li>{@value #TESTER_JUNIT_PLATFORM}</li>
     * </ul>
     */
    @Parameter(defaultValue = TESTER_JUNIT_PLATFORM)
    private String tester = TESTER_JUNIT_PLATFORM;

    /**
     * Configures additional bundles that should be included in the the test-setup, this could be
     * required items to run the test or bundles that declare a <code>Test-Cases</code> header and
     * contain the actual tests to run.
     */
    @Parameter
    private Set<String> bundles;

    /**
     * Configure the run framework to use usually one of
     *
     * <ul>
     * <li>{@value #FW_FELIX}</li>
     * <li>{@value #FW_EQUINOX}</li>
     * </ul>
     */
    @Parameter(defaultValue = FW_EQUINOX)
    private String runfw = FW_EQUINOX;

    @Parameter
    private Map<String, String> properties;

    /**
     * Configures the test engines to use, for example:
     * <ul>
     * <li>{@value #ENGINE_JUPITER} - if your test only contains JUnit5 tests</li>
     * <li>{@value #ENGINE_VINTAGE_ENGINE} - if your test only contains JUnit 3/4</li>
     * <li>{@value #ENGINES_DEFAULT} - if you want to use both engines</li>
     * </ul>
     *
     */
    @Parameter(defaultValue = ENGINES_DEFAULT, required = true)
    private String testEngines;

    @Inject
    private BundleReader bundleReader;

    @Inject
    private ProjectDependenciesResolver resolver;

    @Inject
    @SuppressWarnings("deprecation")
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySession;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private MavenBundleResolver mavenBundleResolver;

    @Override
    protected void runTests(ScanResult scanResult) throws MojoExecutionException, MojoFailureException {
        List<ResolvedArtifactKey> implicitBundles = new ArrayList<>();
        List<String> runrequire = addBundleUnderTest(scanResult, implicitBundles);
        addFramework(implicitBundles);
        addTestFramework(implicitBundles, runrequire);
        addBndEmbeddedRepo(implicitBundles);
        BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
                artifactFactory, repositorySystem).build();
        File runfile = new File(project.getBuild().getDirectory(), "test.bndrun");
        //see https://bnd.bndtools.org/chapters/310-testing.html
        Properties properties = new Properties();
        String testProfileName = getTestProfileName();
        properties.setProperty(Constants.RUNEE, testProfileName);
        properties.setProperty(Constants.TESTER, tester);
        properties.setProperty(Constants.RUNREQUIRES, runrequire.stream().collect(Collectors.joining(",")));
        properties.setProperty(Constants.RUNTRACE, String.valueOf(trace));
        properties.setProperty(Constants.RUNFW, runfw);
        properties.setProperty(Constants.RUNPROPERTIES, buildRunProperties());
        try {
            ReproducibleUtils.storeProperties(properties, runfile.toPath());
            String javaExecutable = getJavaExecutable();
            int returncode = container.execute(runfile, "testing", work, (file, bndrun, run) -> {
                if (new File(javaExecutable).isFile()) {
                    run.setProperty("java", javaExecutable);
                }
                //TODO currently we can't add plugins directly see https://github.com/bndtools/bnd/issues/5514
                Workspace workspace = run.getWorkspace();
                for (RepositoryPlugin rp : workspace.getRepositories()) {
                    workspace.removeBasicPlugin(rp);
                }
                projectManager.getTargetPlatform(project).ifPresent(tp -> {
                    workspace.addBasicPlugin(new TargetPlatformRepository(getReactorProject(), tp));
                });

                if (scanResult instanceof BundleScanResult bundleScanResult) {
                    workspace.addBasicPlugin(new ArtifactKeyRepository(bundleScanResult.pomBundles, "pom-dependencies",
                            project.getFile()));
                }
                workspace.addBasicPlugin(new ArtifactKeyRepository(implicitBundles, "implicit-project-dependencies",
                        project.getBasedir()));
                workspace.refresh(); // required to clear cached plugins...
                try {
                    getLog().info("Resolving test-container");
                    if (printBundles) {
                        if (TESTER_JUNIT_PLATFORM.equals(tester)) {
                            for (String engine : Strings.split(testEngines)) {
                                getLog().info(String.format("-engine: %s", engine));
                            }
                        }
                        getLog().info(String.format("%s: %s", Constants.RUNEE, testProfileName));
                        getLog().info(String.format("%s: %s", Constants.RUNFW, runfw));
                        getLog().info(String.format("%s: %s", Constants.TESTER, tester));
                        for (String require : runrequire) {
                            getLog().info(String.format("%s: %s", Constants.RUNREQUIRES, require));
                        }
                    }
                    String runBundles = run.resolve(false, false);
                    run.getWarnings().forEach(getLog()::warn);
                    if (run.isOk()) {
                        if (printBundles) {
                            for (Requirement bundle : new RequirementListConverter().convert(runBundles)) {
                                getLog().info(String.format("%s: %s", Constants.RUNBUNDLES, bundle));
                            }
                        }
                        run.setProperty(Constants.RUNBUNDLES, runBundles);
                    } else {
                        run.getErrors().forEach(getLog()::error);
                        return ERROR_RESOLVE_CONTAINER;
                    }
                } catch (ResolutionException re) {
                    getLog().error(ResolveProcess.format(re, false));
                    return ERROR_RESOLVE_CONTAINER;
                }
                int numberOfTests = scanResult.size();
                ProjectTester tester = run.getProjectTester();
                tester.setReportDir(getReportsDirectory());
                if (test != null && !test.trim().isEmpty()) {
                    numberOfTests = 0;
                    for (String test : Strings.split(test)) {
                        tester.addTest(test);
                        numberOfTests++;
                    }
                } else {
                    //only run tests from bundles that where activated
                    for (int i = 0; i < numberOfTests; i++) {
                        tester.addTest(scanResult.getClassName(i));
                    }
                }
                tester.prepare();
                run.getWarnings().forEach(getLog()::warn);
                if (!run.isOk()) {
                    run.getErrors().forEach(getLog()::error);
                    return ERROR_PREPARE_CONTAINER;
                }
                getLog().info("Running " + numberOfTests + " test(s)");
                int errors = tester.test();
                //TODO currently we can't get the real run statistic see https://github.com/bndtools/bnd/issues/5513
                FailsafeSummaryXmlUtils.writeSummary(new RunResult(numberOfTests, 0, errors, 0), summaryFile,
                        summaryAppend);
                return 0;
            });
            if (returncode == ERROR_PREPARE_CONTAINER) {
                throw new MojoExecutionException("prepare test container failed!");
            }
            if (returncode == ERROR_RESOLVE_CONTAINER) {
                throw new MojoExecutionException("resolve bundles failed!");
            }
        } catch (Exception e) {
            if (e instanceof MojoExecutionException mee) {
                throw mee;
            }
            if (e instanceof MojoFailureException mfe) {
                throw mfe;
            }
            throw new MojoExecutionException("executing test container failed!", e);
        }

    }

    private String buildRunProperties() {
        Map<String, Object> runProperties = new LinkedHashMap<>();
        runProperties.put("tester.trace", testerTrace);
        runProperties.put("tester.continuous", false);
        runProperties.put(Constants.LAUNCH_ACTIVATION_EAGER, launchActivationEager);
        if (enableSecurity) {
            getLog().info("Enable OSGi security for the framework");
            runProperties.put(org.osgi.framework.Constants.FRAMEWORK_SECURITY,
                    org.osgi.framework.Constants.FRAMEWORK_SECURITY_OSGI);
            if (keyStores != null) {
                runProperties.put(org.osgi.framework.Constants.FRAMEWORK_TRUST_REPOSITORIES,
                        keyStores.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")));
            }
            if (policyFile != null) {
                runProperties.put("java.security.policy", policyFile.getAbsolutePath());
            }
        }
        if (properties != null) {
            for (Entry<String, String> entry : properties.entrySet()) {
                runProperties.put(entry.getKey(), quotedValue(entry.getValue()));
            }
        }
        return runProperties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    private String quotedValue(String value) {
        return "'" + value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ') + "'";
    }

    private void addTestFramework(List<ResolvedArtifactKey> bundles, List<String> runrequire) {
        runrequire.add("bnd.identity; id=" + tester);
        if (TESTER_JUNIT_PLATFORM.equals(tester)) {
            for (String engine : Strings.split(testEngines)) {
                runrequire.add("bnd.identity; id=" + engine);
            }
            for (JUnitBundle key : JUnitClasspathContainerEntry.JUNIT5_PLUGINS) {
                mavenBundleResolver.resolveMavenBundle(project, session, ClasspathReader.toMaven(key)).ifPresentOrElse(
                        bundles::add,
                        () -> getLog().warn("Cannot get JUnit artifact " + key + ". Test run might not resolve"));
            }
            if (printTests || trace) {
                //TODO currently we need to add an extra listener see https://github.com/bndtools/bnd/issues/5507
                //TODO currently we need to print debug infos manually see https://github.com/bndtools/bnd/issues/5520
                mavenBundleResolver
                        .resolveMavenBundle(project, session, "org.eclipse.tycho",
                                "org.eclipse.tycho.bnd.executionlistener", TychoVersion.getTychoVersion())
                        .ifPresentOrElse(t -> {
                            bundles.add(t);
                            runrequire.add("bnd.identity; id=org.eclipse.tycho.bnd.executionlistener");
                        }, () -> {
                            getLog().debug("Cannot resolve execution listener, output will be missing");
                        });
            }
        } else if (TESTER_DEFAULT.equals(tester)) {
            for (JUnitBundle key : JUnitClasspathContainerEntry.JUNIT4_PLUGINS) {
                mavenBundleResolver.resolveMavenBundle(project, session, ClasspathReader.toMaven(key)).ifPresentOrElse(
                        bundles::add,
                        () -> getLog().warn("Cannot get JUnit artifact " + key + ". Test run might not resolve"));
            }
        }

    }

    private void addFramework(List<ResolvedArtifactKey> bundles) {

        getRunFramework().flatMap(key -> mavenBundleResolver.resolveMavenBundle(project, session, key)).ifPresentOrElse(
                bundles::add, () -> getLog().warn("Cannot get artifact for runfw " + runfw + ". Test run might fail"));

    }

    private List<String> addBundleUnderTest(ScanResult scanResult, List<ResolvedArtifactKey> bundles)
            throws MojoExecutionException {
        ArrayList<String> roots = new ArrayList<>();
        ArtifactKey artifactKey = osgiBundle.getArtifactKey(getReactorProject());
        roots.add("bnd.identity; id=" + artifactKey.getId() + ";version=" + artifactKey.getVersion());
        bundles.add(ResolvedArtifactKey.of(artifactKey, project.getArtifact().getFile()));
        try {
            createTestPluginJar(getReactorProject(), IMPORT_REQUIRED_PACKAGES, getPlainScanResult(scanResult))
                    .ifPresent(testBundle -> {
                        bundles.add(testBundle);
                        roots.add("bnd.identity; id=" + testBundle.getId() + ";version=" + testBundle.getVersion());
                    });
        } catch (Exception e) {
            throw new MojoExecutionException("Can't create test jar", e);
        }
        if (this.bundles != null) {
            for (String bundle : this.bundles) {
                roots.add("bnd.identity; id=" + bundle);
            }
        }
        return roots;
    }

    private ScanResult getPlainScanResult(ScanResult scanResult) {
        if (scanResult instanceof BundleScanResult) {
            return ((BundleScanResult) scanResult).plainTestResults;
        }
        return scanResult;
    }

    private void addBndEmbeddedRepo(List<ResolvedArtifactKey> bundles) {
        String bndVersion = TychoVersion.getBndVersion();
        for (String artifactId : BND_EMBEDDED_REPO_ARTIFACTS) {
            mavenBundleResolver
                    .resolveMavenBundle(project, session, BND_EMBEDDED_REPO_ARTIFACTS_GROUP, artifactId, bndVersion)
                    .ifPresent(bundles::add);
        }
    }

    private Optional<MavenArtifactKey> getRunFramework() {
        if (FW_EQUINOX.equals(runfw)) {
            return Optional.of(FRAMEWORK_EQUINOX);
        }
        if (FW_FELIX.equals(runfw)) {
            return Optional.of(FRAMEWORK_FELIX);
        }
        return Optional.empty();
    }

    @Override
    protected boolean isCompatiblePackagingType(String packaging) {
        return this.packaging.equals(packaging);
    }

    @Override
    protected File getReportsDirectory() {
        return reportDirectory;
    }

    @Override
    protected File getTestClassesDirectory() {
        return testClassesDirectory;
    }

    @Override
    protected BundleScanResult scanForTests() {
        ScanResult moduletests = super.scanForTests();
        PomDependencies pomDependencies = projectManager.getTargetPlatformConfiguration(project).getPomDependencies();
        Set<String> bundleTestCases = new HashSet<>();
        List<ResolvedArtifactKey> pomBundles = new ArrayList<>();
        if (pomDependencies != PomDependencies.ignore) {
            for (Artifact artifact : project.getArtifacts()) {
                try {
                    File file = artifact.getFile();
                    OsgiManifest manifest = bundleReader.loadManifest(file);
                    String header = manifest.getValue(TychoConstants.HEADER_TESTCASES);
                    ResolvedArtifactKey key = ResolvedArtifactKey.of(manifest.toArtifactKey(), file);
                    for (String test : getDeclaredTests(header, key)) {
                        bundleTestCases.add(test);
                    }
                    pomBundles.add(key);
                } catch (OsgiManifestParserException e) {
                    //nothing we can use...
                }
            }
        }
        if (bundles != null) {
            TargetPlatform targetPlatform = projectManager.getTargetPlatform(project).orElse(null);
            if (targetPlatform == null) {
                getLog().warn("No target platform, can't resolve additionally specified bundles!");
            } else {
                for (String bundle : bundles) {
                    try {
                        ArtifactKey key = targetPlatform.resolveArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, bundle,
                                Version.emptyVersion.toString());
                        File file = targetPlatform.getArtifactLocation(key);
                        if (file != null) {
                            OsgiManifest manifest = bundleReader.loadManifest(file);
                            String header = manifest.getValue(TychoConstants.HEADER_TESTCASES);
                            for (String test : getDeclaredTests(header, key)) {
                                bundleTestCases.add(test);
                            }
                        }
                    } catch (DependencyResolutionException | IllegalArtifactReferenceException
                            | OsgiManifestParserException e) {
                        //nothing we can use...
                        getLog().debug("Bundle " + bundle + " was not found in target platform: " + e);
                    }
                }
            }
        }
        return new BundleScanResult(moduletests, pomBundles, List.copyOf(bundleTestCases), project.getFile());
    }

    private List<String> getDeclaredTests(String header, ArtifactKey key) {
        if (header != null && !header.isBlank()) {
            if (bundles != null && bundles.contains(key.getId())) {
                //TODO actually we must apply include/exclude filters as well!
                return Strings.split(header);
            }
        }
        return List.of();
    }

    private static final class BundleScanResult implements ScanResult {

        private ScanResult plainTestResults;
        private List<String> bundleTestCases;
        private List<ResolvedArtifactKey> pomBundles;

        public BundleScanResult(ScanResult plainTestResults, List<ResolvedArtifactKey> pomBundles,
                List<String> bundleTestCases, File location) {
            this.plainTestResults = plainTestResults;
            this.pomBundles = pomBundles;
            this.bundleTestCases = bundleTestCases;
        }

        @Override
        public int size() {
            return bundleTestCases.size() + plainTestResults.size();
        }

        @Override
        public String getClassName(int index) {
            int size = plainTestResults.size();
            if (index < size) {
                return plainTestResults.getClassName(index);
            }
            try {
                return bundleTestCases.get(index - size);
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException(index);
            }
        }

        @Override
        public TestsToRun applyFilter(ScannerFilter scannerFilter, ClassLoader testClassLoader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Class<?>> getClassesSkippedByValidation(ScannerFilter scannerFilter, ClassLoader testClassLoader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeTo(Map<String, String> properties) {
            throw new UnsupportedOperationException();
        }

    }

}
