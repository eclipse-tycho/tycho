/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.surefire.provider.impl.NoopTestFrameworkProvider;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

/**
 * Executes integration-tests in an OSGi runtime.
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
 * starting a web-server, creating files, etc.)</li>
 * <li><code>integration-test</code> phase does not fail the build if there are test failures but a
 * summary file is written</li>
 * <li><code>post-integration-test</code> could be used to cleanup/tear down any resources from the
 * <code>pre-integration-test</code> phase</li>
 * <li>test outcome is checked in the <code>verify</code> phase that might fail the build if there
 * are test failures</li>
 * </ol>
 * </p>
 * Summary files are generated according to the default maven-surefire-plugin for integration with
 * tools that already work with maven-surefire-plugin (e.g. CI servers).
 */
@Mojo(name = "plugin-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class TychoIntegrationTestMojo extends AbstractTestMojo {
    /**
     * The directory containing generated test classes of the project being tested.
     */
    @Parameter(property = "project.build.testOutputDirectory")
    private File testClassesDirectory;

    @Parameter(property = "tycho.plugin-test.skip")
    private boolean skipITs;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports/failsafe-summary.xml", required = true)
    private File summaryFile;

    @Parameter(defaultValue = "${project.build.directory}/failsafe-reports", required = true)
    private File reportDirectory;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> pluginRemoteRepositories;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
    private List<ArtifactRepository> projectRemoteRepositories;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    @Component
    private BuildPropertiesParser buildPropertiesParser;

    @Override
    protected boolean shouldRun() {
        return scanForTests().size() > 0;
    }

    @Override
    protected boolean isCompatiblePackagingType(final String packaging) {
        return PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging());
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
    protected PropertiesWrapper createSurefireProperties(final TestFrameworkProvider provider,
            final ScanResult scanResult) throws MojoExecutionException {
        final var properties = super.createSurefireProperties(provider, scanResult);
        properties.setProperty("failifnotests", String.valueOf(false));
        properties.setProperty("failsafe", summaryFile.getAbsolutePath());
        return properties;
    }

    @Override
    protected void handleNoTestsFound() {
        getLog().info("No tests found");
    }

    @Override
    protected void handleSuccess() {
        // Nothing to do. Will be handled in the verify phase
    }

    @Override
    protected void handleTestFailures() {
        // Nothing to do. Will be handled in the verify phase
    }

    @Override
    protected void setupTestBundles(final TestFrameworkProvider provider,
            final EquinoxInstallationDescription testRuntime) throws MojoExecutionException {
        final var dependencies = pluginDescriptor.getPlugin().getDependencies();

        if (dependencies.isEmpty()) {
            super.setupTestBundles(provider, testRuntime);
        } else {
            super.setupTestBundles(new NoopTestFrameworkProvider(), testRuntime);

            for (final var dependency : dependencies) {
                final var resolveArtifact = resolveDependency(dependency);

                for (final var artifact : resolveArtifact.getArtifacts()) {
                    final var file = artifact.getFile();

                    if (file != null) {
                        testRuntime.addDevEntries("org.eclipse.tycho.surefire.osgibooter", file.getAbsolutePath());
                    }
                }
            }
        }

        final var reactorProject = DefaultReactorProject.adapt(project);
        final File testPluginJar;

        try {
            testPluginJar = createTestPluginJar(reactorProject);
        } catch (final Exception e) {
            throw new MojoExecutionException("Error assembling test fragment JAR", e);
        }

        final var bundleArtifactKey = getBundleArtifactKey(testPluginJar);
        testRuntime.addBundle(bundleArtifactKey.getId(), bundleArtifactKey.getVersion(), testPluginJar);

        final var bsn = bundleArtifactKey.getId();
        final var testClasspath = osgiBundle.getTestClasspath(reactorProject, false);

        // Here we add the whole (test) classpath as a dev-entry to the runtime fragment.
        // This is required to optionally load any test-scoped class that is not imported
        // and not available as an OSGi bundle
        final var testDevEntries = testClasspath.stream().map(ClasspathEntry::getLocations).flatMap(Collection::stream)
                .map(File::getAbsolutePath).collect(Collectors.joining(","));
        testRuntime.addDevEntries(bsn, testDevEntries);
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
     */
    private File createTestPluginJar(final ReactorProject reactorProject) throws Exception {
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

        try (final var mainArtifact = new Jar(reactorProject.getArtifact());
                final var jar = new Jar(reactorProject.getName() + " test classes", outDir, null);
                final var analyzer = new Analyzer(jar)) {
            final var bundleManifest = mainArtifact.getManifest();
            final var hostVersion = bundleManifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            final var hostSymbolicName = getHostSymbolicName(bundleManifest);
            final var fragmentHost = "%s;%s=\"%s\"".formatted(hostSymbolicName, Constants.BUNDLE_VERSION_ATTRIBUTE,
                    hostVersion);
            final var bundleName = "Test fragment for %s:%s:%s".formatted(project.getGroupId(), project.getArtifactId(),
                    project.getVersion());

            analyzer.setProperty(Constants.BUNDLE_VERSION, hostVersion);
            analyzer.setProperty(Constants.BUNDLE_SYMBOLICNAME, hostSymbolicName + "." + uuid);
            analyzer.setProperty(Constants.FRAGMENT_HOST, fragmentHost);
            analyzer.setProperty(Constants.BUNDLE_NAME, bundleName);
            analyzer.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");

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
            jar.setManifest(analyzer.calcManifest());
            jar.write(fragmentFile);
        }

        return fragmentFile;
    }

    private ArtifactResolutionResult resolveDependency(final Dependency dependency) {
        final var artifact = repositorySystem.createDependencyArtifact(dependency);
        final var remoteRepositories = new ArrayList<ArtifactRepository>(32);
        remoteRepositories.addAll(pluginRemoteRepositories);
        remoteRepositories.addAll(projectRemoteRepositories);

        final var request = new ArtifactResolutionRequest()//
                .setOffline(session.isOffline())//
                .setArtifact(artifact)//
                .setLocalRepository(localRepository)//
                .setResolveTransitively(true)//
                .setCollectionFilter(new ProviderDependencyArtifactFilter())//
                .setRemoteRepositories(remoteRepositories);
        return repositorySystem.resolve(request);
    }

    /**
     * Returns a normalized host bundle Bundle-SymbolicName.
     * <p>
     * This means any metadata apart from the name itself is removed.
     */
    private String getHostSymbolicName(final Manifest manifest) {
        final var value = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        final var separatorIndex = value.indexOf(';');
        return separatorIndex > -1 ? value.substring(0, separatorIndex) : value;
    }

    private static final class ProviderDependencyArtifactFilter implements ArtifactFilter {
        static final Collection<String> SCOPES = List.of(Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE_PLUS_RUNTIME,
                Artifact.SCOPE_RUNTIME);

        @Override
        public boolean include(final Artifact artifact) {
            final var scope = artifact.getScope();
            return !artifact.isOptional() && (scope == null || SCOPES.contains(scope));
        }
    }
}
