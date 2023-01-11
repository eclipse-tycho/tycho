/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.maven.ToolchainProvider.JDKUsage;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public abstract class AbstractTestMojo extends AbstractMojo {

    private static final String SYSTEM_JDK = "jdk";

    private static final String[] JAVA_EXECUTABLES = { "java", "java.exe" };

    protected static final String IMPORT_REQUIRED_PACKAGES = "*";

    protected static final String IMPORT_PACKAGES_OPTIONAL = "*;resolution:=optional";

    /**
     * Root directory (<a href=
     * "https://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html#osgiinstallarea"
     * >osgi.install.area</a>) of the Equinox runtime used to execute tests.
     */
    @Parameter(defaultValue = "${project.build.directory}/work")
    protected File work;

    @Parameter(property = "project", readonly = true)
    protected MavenProject project;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in
     * testing. When not specified and when the <code>test</code> parameter is not specified, the
     * default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*Tests.java   **&#47;*TestCase.java</code>
     */
    @Parameter
    protected List<String> includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in
     * testing. When not specified and when the <code>test</code> parameter is not specified, the
     * default excludes will be <code>**&#47;*$*</code> (which excludes all inner classes).
     */
    @Parameter
    protected List<String> excludes;

    /**
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern
     * matching, to select tests to run. The Ant pattern will be used to create an include pattern
     * formatted like <code>**&#47;${test}.java</code> When used, the <code>includes</code> and
     * <code>excludes</code> patterns parameters are ignored
     */
    @Parameter(property = "test")
    protected String test;

    /**
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED,
     * but quite convenient on occasion. Default: <code>false</code>
     */
    @Parameter(property = "skipTests")
    protected Boolean skipTests;

    /**
     * If set to "false" the test execution will not fail in case there are no tests found.
     */
    @Parameter(property = "failIfNoTests", defaultValue = "true")
    protected boolean failIfNoTests;

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
     * Additional dependencies to be added to the test runtime.
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
     * Which JDK to use for executing tests. Possible values are: <code>SYSTEM</code>,
     * <code>BREE</code> .
     * <p/>
     * <ul>
     * <li>SYSTEM: Use the currently running JVM (or from
     * <a href="https://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchain</a> if
     * configured in pom.xml)</li>
     * <li>BREE: use MANIFEST header <code>Bundle-RequiredExecutionEnvironment</code> to lookup the
     * JDK from <a href=
     * "https://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchains.xml</a>. The
     * value of BREE will be matched against the id of the toolchain elements in
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
     * Same as {@link #skipTests}
     */
    @Parameter(property = "maven.test.skip")
    protected Boolean skip;

    /**
     * prints all loaded bundles
     */
    @Parameter(property = "tycho.printBundles", defaultValue = "false")
    protected boolean printBundles;

    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession session;

    @Component
    protected TychoProjectManager projectManager;

    @Component
    protected InstallableUnitGenerator generator;

    @Component
    protected DefaultDependencyResolverFactory dependencyResolverLocator;

    @Component
    protected ToolchainManager toolchainManager;

    @Component
    protected ToolchainProvider toolchainProvider;

    @Component
    protected BuildPropertiesParser buildPropertiesParser;

    @Component(role = TychoProject.class, hint = "eclipse-plugin")
    protected OsgiBundleProject osgiBundle;

    @Parameter(property = "plugin.artifacts")
    protected List<Artifact> pluginArtifacts;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (!isCompatiblePackagingType(project.getPackaging())) {
            getLog().debug("Execution was skipped because of incompatible packaging type: " + project.getPackaging());
            return;
        }
        if (shouldSkip()) {
            getLog().info("Skipping tests");
            return;
        }
        ScanResult result = scanForTests();
        if (result.size() == 0) {
            handleNoTestsFound();
            return;
        }
        runTests(result);
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
        List<String> classes = scanResult.getClasses();
        for (String clazz : classes) {
            getLog().debug("Class " + clazz + " matches the current filter.");
        }
        if (classes.isEmpty()) {
            getLog().debug("Nothing matches pattern " + includeList + ", excluding " + excludeList + " in "
                    + getTestClassesDirectory());
        }
        return scanResult;
    }

    protected List<String> getDefaultExclude() {
        return Arrays.asList("**/*$*");
    }

    protected List<String> getDefaultInclude() {
        return List.of("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class");
    }

    protected abstract void runTests(ScanResult scanResult) throws MojoExecutionException, MojoFailureException;

    protected abstract boolean isCompatiblePackagingType(String packaging);

    protected boolean shouldSkip() {
        if (skip != null && skipTests != null && !skip.equals(skipTests)) {
            getLog().warn(
                    "Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority");
        }
        if (skipTests != null) {
            return skipTests;
        }
        if (skip != null) {
            return skip;
        }
        return false;
    }

    protected ReactorProject getReactorProject() {
        return DefaultReactorProject.adapt(project);
    }

    protected List<ReactorProject> getReactorProjects() {
        return DefaultReactorProject.adapt(session);
    }

    protected List<ArtifactKey> getExtraDependencies() {
        final List<ArtifactKey> dependencies = new ArrayList<>();
        if (this.dependencies != null) {
            for (Dependency key : this.dependencies) {
                dependencies.add(new DefaultArtifactKey(key.getType(), key.getArtifactId(), key.getVersion()));
            }
        }
        TargetPlatformConfiguration configuration = TychoProjectUtils
                .getTargetPlatformConfiguration(DefaultReactorProject.adapt(project));
        dependencies.addAll(configuration.getDependencyResolverConfiguration().getAdditionalArtifacts());
        return dependencies;
    }

    protected DependencyArtifacts resolveDependencies(Collection<IRequirement> additionalRequirements)
            throws MojoExecutionException {
        List<ArtifactKey> extraDependencies = getExtraDependencies();
        DependencyResolver platformResolver = dependencyResolverLocator.lookupDependencyResolver(project);
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
        DependencyArtifacts testRuntimeArtifacts = platformResolver.resolveDependencies(session, project, null,
                getReactorProjects(), resolverConfiguration, getTestTargetEnvironments());
        if (testRuntimeArtifacts == null) {
            throw new MojoExecutionException(
                    "Cannot determinate build target platform location -- not executing tests");
        }
        return testRuntimeArtifacts;
    }

    /**
     * This generates a bundle that is a fragment to the host that enhances the original bundle by
     * the following items:
     * <ol>
     * <li>any 'additional bundle', even though this is not really meant to be used that way, is
     * added as an optional dependency</li>
     * <li>a <code>DynamicImport-Package: *</code> is added to allow dynamic classloading from the
     * bundle classpath</li>
     * <li>computes package imports based on the generated test classes and add them as optional
     * imports, so that any class is consumed from the OSGi runtime before the inner classes are
     * searched</li>
     * </ol>
     * 
     * @param scanResult
     * @param additionalRequirements
     */
    protected Optional<ResolvedArtifactKey> createTestPluginJar(final ReactorProject reactorProject,
            String packageImport, ScanResult scanResult, Consumer<IRequirement> testPluginRequirementsConsumer)
            throws Exception {
        final var uuid = UUID.randomUUID();
        final var artifactBaseName = FilenameUtils.getBaseName(reactorProject.getArtifact().getName());
        final var testJarName = artifactBaseName + "_test_fragment_" + uuid + ".jar";
        final var fragmentFile = new File(project.getBuild().getDirectory(), testJarName);

        if (fragmentFile.exists()) {
            if (!fragmentFile.delete()) {
                throw new IllegalStateException("Could not delete the existing fragment file " + fragmentFile);
            }
        }
        final var outDir = new File(project.getBuild().getTestOutputDirectory());
        if (!outDir.isDirectory()) {
            return Optional.empty();
        }
        ResolvedArtifactKey result;
        try (final var mainArtifact = new Jar(reactorProject.getArtifact());
                final var jar = new Jar(reactorProject.getName() + " test classes", outDir, null);
                final var analyzer = new Analyzer(jar)) {
            if (jar.getResources().isEmpty()) {
                return Optional.empty();
            }
            final var bundleManifest = mainArtifact.getManifest();
            final var hostVersion = bundleManifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            final var fragmentHost = getFragmentHost(bundleManifest, hostVersion);
            final var bundleName = "Test fragment for %s:%s:%s".formatted(project.getGroupId(), project.getArtifactId(),
                    project.getVersion());

            analyzer.setProperty(Constants.BUNDLE_VERSION, hostVersion);
            String fragmentId = project.getGroupId() + "-" + project.getArtifactId() + "-" + uuid;
            analyzer.setProperty(Constants.BUNDLE_SYMBOLICNAME, fragmentId);
            analyzer.setProperty(Constants.FRAGMENT_HOST, fragmentHost);
            analyzer.setProperty(Constants.BUNDLE_NAME, bundleName);
            analyzer.setProperty(Constants.IMPORT_PACKAGE, packageImport);
            if (scanResult != null) {
                String collect = IntStream.range(0, scanResult.size()).mapToObj(scanResult::getClassName)
                        .collect(Collectors.joining(","));
                if (!collect.isEmpty()) {
                    analyzer.setProperty(TychoConstants.HEADER_TESTCASES, collect);
                }
            }

            final var additionalBundles = buildPropertiesParser.parse(reactorProject).getAdditionalBundles();

            if (!additionalBundles.isEmpty()) {
                final var stringValue = additionalBundles.stream().map(b -> b + ";resolution:=optional")
                        .collect(Collectors.joining(","));
                analyzer.setProperty(Constants.REQUIRE_BUNDLE, stringValue);
            }

            analyzer.setProperty(Constants.DYNAMICIMPORT_PACKAGE, "*");

            final var testClasspath = osgiBundle.getTestClasspath(reactorProject);

            for (final var classpathEntry : testClasspath) {
                for (final var location : classpathEntry.getLocations()) {
                    analyzer.addClasspath(location);
                }
            }

            analyzer.addClasspath(mainArtifact);
            Manifest manifest = analyzer.calcManifest();
            if (testPluginRequirementsConsumer != null) {
                generator.getInstallableUnits(manifest).stream().flatMap(iu -> iu.getRequirements().stream())
                        .forEach(testPluginRequirementsConsumer);
            }
            jar.setManifest(manifest);
            jar.write(fragmentFile);
            result = ResolvedArtifactKey.of(ArtifactType.TYPE_ECLIPSE_PLUGIN, fragmentId, hostVersion, fragmentFile);
        }
        return Optional.of(result);
    }

    private String getFragmentHost(final Manifest manifest, final String hostVersion) {
        Attributes attributes = manifest.getMainAttributes();
        String host = attributes.getValue(Constants.FRAGMENT_HOST);
        if (host != null) {
            return host;
        }
        final var value = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
        final var separatorIndex = value.indexOf(';');
        final var hostSymbolicName = separatorIndex > -1 ? value.substring(0, separatorIndex) : value;
        final var fragmentHost = "%s;%s=\"%s\"".formatted(hostSymbolicName, Constants.BUNDLE_VERSION_ATTRIBUTE,
                hostVersion);
        return fragmentHost;
    }

    protected List<TargetEnvironment> getTestTargetEnvironments() {
        TargetPlatformConfiguration configuration = TychoProjectUtils
                .getTargetPlatformConfiguration(getReactorProject());
        List<TargetEnvironment> targetEnvironments = configuration.getEnvironments();
        TargetEnvironment runningEnvironment = TargetEnvironment.getRunningEnvironment(getReactorProject());
        for (TargetEnvironment targetEnvironment : targetEnvironments) {
            if (targetEnvironment.equals(runningEnvironment)) {
                getLog().debug("Using matching target environment " + targetEnvironment.toFilterProperties()
                        + " to resolve test artifacts...");
                return List.of(targetEnvironment);
            }
        }
        getLog().warn("Your build environment " + runningEnvironment.toFilterProperties()
                + " do not match any of the configured target environments " + targetEnvironments
                + " test execution might vary!");
        return targetEnvironments;
    }

    protected Toolchain getToolchain() throws MojoExecutionException {
        if (JDKUsage.SYSTEM.equals(useJDK)) {
            if (toolchainManager != null) {
                return toolchainManager.getToolchainFromBuildContext(SYSTEM_JDK, session);
            }
            return null;
        }
        String profileName = getTestProfileName();
        Toolchain toolChain = toolchainProvider.findMatchingJavaToolChain(session, profileName);
        if (toolChain == null) {
            throw new MojoExecutionException("useJDK = BREE configured, but no toolchain of type 'jdk' with id '"
                    + profileName + "' found. See https://maven.apache.org/guides/mini/guide-using-toolchains.html");
        }
        return toolChain;
    }

    protected String getTestProfileName() {
        String profileName = TychoProjectUtils
                .getExecutionEnvironmentConfiguration(DefaultReactorProject.adapt(project)).getProfileName();
        return profileName;
    }

    protected String getJavaExecutable() throws MojoExecutionException {
        Toolchain tc = getToolchain();
        if (tc != null) {
            getLog().info("Toolchain in tycho-surefire-plugin: " + tc);
            return tc.findTool("java");
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            File java = getJavaFromJavaHome(javaHome);
            if (java != null) {
                getLog().info("Could not find a java toolchain of type " + SYSTEM_JDK
                        + ", using java from JAVA_HOME instead (" + java.getAbsolutePath() + ")");
                return java.getAbsolutePath();
            }
            getLog().info("Could not find a java toolchain of type " + SYSTEM_JDK
                    + " and JAVA_HOME seem to not point to a valid location, trying java from PATH instead (current JAVA_HOME="
                    + javaHome + ")");
        } else {
            getLog().info("Could not find a java toolchain of type " + SYSTEM_JDK
                    + " and JAVA_HOME is not set, trying java from PATH instead");
        }
        return "java";
    }

    private File getJavaFromJavaHome(String javaHome) {
        File javaBin = new File(javaHome, "bin");
        for (String executable : JAVA_EXECUTABLES) {
            File java = new File(javaBin, executable);
            if (java.isFile()) {
                return java;
            }
        }
        //last resort just in case other extension or case-sensitive file-system...
        File[] listFiles = javaBin.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && FilenameUtils.getBaseName(pathname.getName().toLowerCase()).equals("java");
            }
        });
        if (listFiles != null && listFiles.length > 0) {
            return listFiles[0];
        }
        return null;
    }

    protected void handleNoTestsFound() throws MojoFailureException {
        String message = "No tests found";
        if (failIfNoTests) {
            throw new MojoFailureException(message);
        } else {
            getLog().warn(message);
        }
    }

    protected static IRequirement createBundleRequirement(String id) {
        return MetadataFactory.createRequirement("osgi.bundle", id, VersionRange.emptyRange, null, true, false, true);
    }

    protected abstract File getReportsDirectory();

    protected abstract File getTestClassesDirectory();

}
