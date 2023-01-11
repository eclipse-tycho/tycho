package org.eclipse.tycho.surefire;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.plugins.annotations.Component;
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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;

import aQute.bnd.build.ProjectTester;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.strings.Strings;

/**
 * Execute tests using <a href="https://bnd.bndtools.org/chapters/310-testing.html">BND testing</a>
 */
@Mojo(name = "bnd-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndTestMojo extends AbstractTestMojo {

    private static final String ENGINE_VINTAGE_ENGINE = "org.junit.vintage.engine";
    private static final String ENGINE_JUPITER = "junit-jupiter-engine";
    private static final String ENGINES_DEFAULT = ENGINE_JUPITER + "," + ENGINE_VINTAGE_ENGINE;
    private static final String TESTER_DEFAULT = "biz.aQute.tester";
    private static final String TESTER_JUNIT_PLATFORM = "biz.aQute.tester.junit-platform";
    private static final String FW_FELIX = "org.apache.felix.framework";
    private static final String FW_EQUINOX = "org.eclipse.osgi";
    private static final int ERROR_PREPARE_CONTAINER = -1;

    private static final Set<String> EMBEDDED_REPO_ARTIFACTS = Set.of("biz.aQute.junit", "biz.aQute.launcher",
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

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true)
    private File summaryFile;

    @Parameter()
    private boolean summaryAppend;

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
     * Configure the run framework to use usually one of
     * 
     * <ul>
     * <li>{@value #FW_FELIX}</li>
     * <li>{@value #FW_EQUINOX}</li>
     * </ul>
     */
    @Parameter(defaultValue = FW_EQUINOX)
    private String runfw = FW_EQUINOX;

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

    @Component
    private BundleReader bundleReader;

    @Component
    private ProjectDependenciesResolver resolver;

    @Component
    @SuppressWarnings("deprecation")
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySession;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private MavenBundleResolver mavenBundleResolver;

    @Override
    protected void runTests(ScanResult scanResult) throws MojoExecutionException, MojoFailureException {
        List<ResolvedArtifactKey> bundles = new ArrayList<>();
        Collection<IRequirement> additionalRequirements = new ArrayList<>();
        if (scanResult instanceof BundleScanResult bundleResult) {
            addTestArtifactRequirements(bundleResult, bundles, additionalRequirements);
        }
        RepositoryPlugin pluginRepository = addTestRunnerRequirements(bundles, additionalRequirements);
        addFramework(bundles);
        addTestProbe(scanResult, bundles, additionalRequirements);
        DependencyArtifacts resolvedDependencies = resolveDependencies(additionalRequirements);
        for (ArtifactDescriptor artifact : resolvedDependencies.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN)) {
            try {
                bundles.add(ResolvedArtifactKey.of(artifact.getKey(), getArtifactFile(artifact)));
            } catch (InterruptedException e) {
                return;
            } catch (ExecutionException e) {
                throw new MojoExecutionException("Can't fetch artifact file for " + artifact, e);
            }
        }
        List<File> bundleFiles = new ArrayList<>();
        List<String> runbundles = new ArrayList<>();
        for (ResolvedArtifactKey key : bundles) {
            if (printBundles) {
                getLog().info("Adding " + key.getId() + " " + key.getVersion() + " @ " + key.getLocation());
            }
            bundleFiles.add(key.getLocation());
            if (key.getId().equals(runfw)) {
                continue;
            }
            runbundles.add(key.getId());
        }
        BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
                artifactFactory, repositorySystem).setBundles(bundleFiles).build();
        File runfile = new File(project.getBuild().getDirectory(), "test.bndrun");
        //see https://bnd.bndtools.org/chapters/310-testing.html
        Properties properties = new Properties();
        properties.setProperty("-runee", getTestProfileName());
        properties.setProperty("-tester", tester);
        properties.setProperty("-runbundles", runbundles.stream().collect(Collectors.joining(",")));
        properties.setProperty("-runtrace", String.valueOf(trace));
        properties.setProperty("-runfw", runfw);
        properties.setProperty("-runproperties",
                "tester.trace=" + String.valueOf(testerTrace) + ",tester.continuous=false");
        try {
            try (FileOutputStream out = new FileOutputStream(runfile)) {
                properties.store(out, null);
            }
            String javaExecutable = getJavaExecutable();
            int returncode = container.execute(runfile, "testing", work, (file, bndrun, run) -> {
                if (new File(javaExecutable).isFile()) {
                    run.setProperty("java", javaExecutable);
                }
                //TODO currently we can't add plugins directly see https://github.com/bndtools/bnd/issues/5514
                run.getWorkspace().addBasicPlugin(pluginRepository);
                run.getWorkspace().refresh();
                int numberOfTests = scanResult.size();
                ProjectTester tester = run.getProjectTester();
                tester.setReportDir(getReportsDirectory());
                if (test != null && !test.trim().isEmpty()) {
                    numberOfTests = 0;
                    for (String test : Strings.split(test)) {
                        tester.addTest(test);
                        numberOfTests++;
                    }
                }
                tester.prepare();
                if (!run.isOk()) {
                    run.getErrors().forEach(getLog()::error);
                    return ERROR_PREPARE_CONTAINER;
                }
                int errors = tester.test();
                //TODO currently we can't get the real run statistic see https://github.com/bndtools/bnd/issues/5513
                FailsafeSummaryXmlUtils.writeSummary(new RunResult(numberOfTests, 0, errors, 0), summaryFile,
                        summaryAppend);
                return 0;
            });
            if (returncode == ERROR_PREPARE_CONTAINER) {
                throw new MojoExecutionException("prepare test container failed!");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("executing test container failed!", e);
        }

    }

    private void addFramework(List<ResolvedArtifactKey> bundles) {
        //TODO we should provide the framework as an IU to the resolver so it is not resolved from the target twice...
        mavenBundleResolver.resolveMavenBundle(project, session, getRunFramework()).ifPresentOrElse(bundles::add,
                () -> getLog().warn("Can't get artifact for runfw " + runfw + " test run might fail!"));
    }

    private void addTestProbe(ScanResult scanResult, List<ResolvedArtifactKey> bundles,
            Collection<IRequirement> additionalRequirements) throws MojoExecutionException {
        if (!testEngines.isBlank()) {
            for (String testEngine : testEngines.split(",")) {
                additionalRequirements.add(createBundleRequirement(testEngine.trim()));
            }
        }
        try {
            createTestPluginJar(getReactorProject(), IMPORT_REQUIRED_PACKAGES, getPlainScanResult(scanResult),
                    additionalRequirements::add).ifPresent(testBundle -> {
                        bundles.add(testBundle);
                    });
        } catch (Exception e) {
            throw new MojoExecutionException("Can't create test jar", e);
        }
    }

    private ScanResult getPlainScanResult(ScanResult scanResult) {
        if (scanResult instanceof BundleScanResult) {
            return ((BundleScanResult) scanResult).plainTestResults;
        }
        return scanResult;
    }

    private void addTestArtifactRequirements(BundleScanResult bundleResult, List<ResolvedArtifactKey> bundles,
            Collection<IRequirement> additionalRequirements) {
        for (var entry : bundleResult.testBundles.entrySet()) {
            generator.getInstallableUnits(entry.getKey()).stream().flatMap(iu -> iu.getRequirements().stream())
                    .forEach(additionalRequirements::add);
            bundles.add(entry.getValue());
        }
    }

    private RepositoryPlugin addTestRunnerRequirements(List<ResolvedArtifactKey> bundles,
            Collection<IRequirement> additionalRequirements) throws MojoExecutionException {
        List<File> pluginArtifactFiles = new ArrayList<>();
        for (Artifact artifact : pluginArtifacts) {
            if (artifact.getGroupId().equals("biz.aQute.bnd")) {
                if (EMBEDDED_REPO_ARTIFACTS.contains(artifact.getArtifactId())) {
                    pluginArtifactFiles.add(artifact.getFile());
                }
                if (artifact.getArtifactId().equals(tester)) {
                    generator.getInstallableUnits(artifact).stream().flatMap(iu -> iu.getRequirements().stream())
                            .filter(req -> {
                                if (req instanceof IRequiredCapability reqcap) {
                                    if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(reqcap.getNamespace())) {
                                        return true;
                                    }
                                }
                                return false;
                            }).forEach(additionalRequirements::add);
                    bundles.add(ResolvedArtifactKey.of(ArtifactType.TYPE_ECLIPSE_PLUGIN, artifact.getArtifactId(),
                            artifact.getVersion(), artifact.getFile()));
                }
            }
        }
        try {
            return new FileSetRepository(repositorySession.getLocalRepository().getBasedir().getAbsolutePath(),
                    pluginArtifactFiles);
        } catch (Exception e) {
            throw new MojoExecutionException("Can't create plugin artifact repository!", e);
        }
    }

    private MavenArtifactKey getRunFramework() {
        if (FW_EQUINOX.equals(runfw)) {
            return MavenArtifactKey.of(ArtifactType.TYPE_ECLIPSE_PLUGIN, runfw, "[3,4)", "org.eclipse.platform",
                    "org.eclipse.osgi");
        }
        if (FW_FELIX.equals(runfw)) {
            return MavenArtifactKey.of(ArtifactType.TYPE_ECLIPSE_PLUGIN, runfw, "1", "org.apache.felix",
                    "org.apache.felix.framework");
        }
        return null;
    }

    private File getArtifactFile(ArtifactDescriptor artifact) throws InterruptedException, ExecutionException {
        ReactorProject mavenProject = artifact.getMavenProject();
        if (mavenProject != null) {
            return mavenProject.getArtifact(artifact.getClassifier());
        }
        return artifact.fetchArtifact().get();
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
    protected ScanResult scanForTests() {
        ScanResult moduletests = super.scanForTests();
        Map<Artifact, ResolvedArtifactKey> bundles = new HashMap<>();
        for (Artifact artifact : project.getArtifacts()) {
            try {
                File file = artifact.getFile();
                OsgiManifest manifest = bundleReader.loadManifest(file);
                String header = manifest.getValue(TychoConstants.HEADER_TESTCASES);
                if (header != null && !header.isBlank()) {
                    bundles.put(artifact, ResolvedArtifactKey.of(manifest.toArtifactKey(), file));
                }
            } catch (OsgiManifestParserException e) {
                //nothing we can use...
            }

        }
        return new BundleScanResult(moduletests, bundles);
    }

    private static final class BundleScanResult implements ScanResult {

        private ScanResult plainTestResults;
        private Map<Artifact, ResolvedArtifactKey> testBundles;

        public BundleScanResult(ScanResult plainTestResults, Map<Artifact, ResolvedArtifactKey> bundles) {
            this.plainTestResults = plainTestResults;
            this.testBundles = bundles;
        }

        @Override
        public int size() {
            return testBundles.size() + plainTestResults.size();
        }

        @Override
        public String getClassName(int index) {
            throw new UnsupportedOperationException();
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
