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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.util.ScanResult;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.ArtifactKey;
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

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> pluginRemoteRepositories;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
    private List<ArtifactRepository> projectRemoteRepositories;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

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

    @Override
    protected void setupTestBundles(TestFrameworkProvider provider, EquinoxInstallationDescription testRuntime)
            throws MojoExecutionException {
        List<Dependency> dependencies = pluginDescriptor.getPlugin().getDependencies();
        if (dependencies.isEmpty()) {
            super.setupTestBundles(provider, testRuntime);
        } else {
            super.setupTestBundles(new NoopTestFrameworkProvider(), testRuntime);
            for (Dependency dependency : dependencies) {
                ArtifactResolutionResult resolveArtifact = resolveDependency(dependency);
                for (Artifact artifact : resolveArtifact.getArtifacts()) {
                    File file = artifact.getFile();
                    if (file != null) {
                        testRuntime.addDevEntries("org.eclipse.tycho.surefire.osgibooter", file.getAbsolutePath());
                    }
                }
            }
        }
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        File testPluginJar = createTestPluginJar(reactorProject);
        ArtifactKey bundleArtifactKey = getBundleArtifactKey(testPluginJar);
        testRuntime.addBundle(bundleArtifactKey, testPluginJar, true);
        String bsn = bundleArtifactKey.getId();
        List<ClasspathEntry> testClasspath = osgiBundle.getTestClasspath(reactorProject, false);
        //here we add the whole (test) classpath as a dev-entry to the runtime fragment, this is required to optionally load any test-scoped class that is not imported and not available as an OSGi bundle
        String testDevEntries = testClasspath.stream().map(ClasspathEntry::getLocations).flatMap(Collection::stream)
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
     * searched.</li>
     * </ol>
     * 
     * @param reactorProject
     * @return
     * @throws MojoExecutionException
     */
    private File createTestPluginJar(ReactorProject reactorProject) throws MojoExecutionException {
        try {
            UUID uuid = UUID.randomUUID();
            File fragmentFile = new File(project.getBuild().getDirectory(),
                    FilenameUtils.getBaseName(reactorProject.getArtifact().getName()) + "_test_fragment_" + uuid
                            + ".jar");
            if (fragmentFile.exists()) {
                fragmentFile.delete();
            }
            try (Jar mainArtifact = new Jar(reactorProject.getArtifact());
                    Jar jar = new Jar(reactorProject.getName() + " test classes",
                            new File(project.getBuild().getTestOutputDirectory()), null);
                    Analyzer analyzer = new Analyzer(jar)) {
                Manifest bundleManifest = mainArtifact.getManifest();
                String hostVersion = bundleManifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                String hostSymbolicName = bundleManifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                analyzer.setProperty(Constants.BUNDLE_VERSION, hostVersion);
                analyzer.setProperty(Constants.BUNDLE_SYMBOLICNAME, hostSymbolicName + "." + uuid);
                analyzer.setProperty(Constants.FRAGMENT_HOST,
                        hostSymbolicName + ";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + hostVersion + "\"");
                analyzer.setProperty(Constants.BUNDLE_NAME, "Test Fragment for " + project.getGroupId() + ":"
                        + project.getArtifactId() + ":" + project.getVersion());
                analyzer.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");
                Collection<String> additionalBundles = reactorProject.getBuildProperties().getAdditionalBundles();
                if (!additionalBundles.isEmpty()) {
                    analyzer.setProperty(Constants.REQUIRE_BUNDLE, additionalBundles.stream()
                            .map(b -> b + ";resolution:=optional").collect(Collectors.joining(",")));
                }
                analyzer.setProperty(Constants.DYNAMICIMPORT_PACKAGE, "*");
                List<ClasspathEntry> testClasspath = osgiBundle.getTestClasspath(reactorProject);
                for (ClasspathEntry classpathEntry : testClasspath) {
                    for (File loc : classpathEntry.getLocations()) {
                        analyzer.addClasspath(loc);
                    }
                }
                analyzer.addClasspath(mainArtifact);
                jar.setManifest(analyzer.calcManifest());
                jar.write(fragmentFile);
            }
            return fragmentFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling test fragment jar", e);
        }

    }

    private ArtifactResolutionResult resolveDependency(Dependency dependency) {
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()//
                .setOffline(session.isOffline())//
                .setArtifact(artifact)//
                .setLocalRepository(localRepository)//
                .setResolveTransitively(true)//
                .setCollectionFilter(new ProviderDependencyArtifactFilter())//
                .setRemoteRepositories(
                        Stream.concat(pluginRemoteRepositories.stream(), projectRemoteRepositories.stream())
                                .collect(Collectors.toList()));
        return repositorySystem.resolve(request);
    }

    private static final class ProviderDependencyArtifactFilter implements ArtifactFilter {
        private static final Collection<String> SCOPES = List.of(Artifact.SCOPE_COMPILE,
                Artifact.SCOPE_COMPILE_PLUS_RUNTIME, Artifact.SCOPE_RUNTIME);

        @Override
        public boolean include(Artifact artifact) {
            String scope = artifact.getScope();
            return !artifact.isOptional() && (scope == null || SCOPES.contains(scope));
        }
    }

}
