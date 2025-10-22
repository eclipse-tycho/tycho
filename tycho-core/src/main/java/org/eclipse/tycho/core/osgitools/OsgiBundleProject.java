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
 *    Christoph Läubrich -  [Bug 572416] Tycho does not understand "additional.bundles" directive in build.properties
 *                          [Bug 572416] Compile all source folders contained in .classpath
 *                          [Issue #460] Delay classpath resolution to the compile phase 
 *                          [Issue #626] Classpath computation must take fragments into account 
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.model.classpath.JUnitBundle;
import org.eclipse.tycho.model.classpath.JUnitClasspathContainerEntry;
import org.eclipse.tycho.model.classpath.LibraryClasspathEntry;
import org.eclipse.tycho.model.classpath.ProjectClasspathEntry;
import org.osgi.framework.Filter;

@Named(PackagingType.TYPE_ECLIPSE_PLUGIN)
@Singleton
public class OsgiBundleProject extends AbstractTychoProject implements BundleProject {

    private static final String CTX_OSGI_BUNDLE_BASENAME = TychoConstants.CTX_BASENAME + "/osgiBundle";
    private static final String CTX_ARTIFACT_KEY = CTX_OSGI_BUNDLE_BASENAME + "/artifactKey";
    private static final String CTX_CLASSPATH = CTX_OSGI_BUNDLE_BASENAME + "/classPath";
    static final String CTX_ECLIPSE_PLUGIN_PROJECT = CTX_OSGI_BUNDLE_BASENAME + "/eclipsePluginProject";

    @Inject
    private BundleReader bundleReader;

    @Inject
    private ClasspathReader classpathParser;

    @Inject
    @Named(EquinoxResolver.HINT)
    private DependenciesResolver resolver;

    @Inject
    private ToolchainManager toolchainManager;

    @Inject
    P2ResolverFactory resolverFactory;

    @Inject
    private BuildPropertiesParser buildPropertiesParser;

    @Inject
    private MavenBundleResolver mavenBundleResolver;

    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        return project.computeContextValue(CTX_ARTIFACT_KEY, () -> readArtifactKey(project.getBasedir()));
    }

    public ArtifactKey readArtifactKey(File location) {
        OsgiManifest mf = bundleReader.loadManifest(location);
        return mf.toArtifactKey();
    }

    public ArtifactKey readOrCreateArtifactKey(File location, Supplier<ArtifactKey> supplier) {
        try {
            return readArtifactKey(location);
        } catch (OsgiManifestParserException e) {
            return supplier.get();
        }
    }

    @Override
    public String getManifestValue(String key, MavenProject project) {
        return getManifest(DefaultReactorProject.adapt(project)).getValue(key);
    }

    private OsgiManifest getManifest(ReactorProject project) {
        MavenProject mavenProject = project.adapt(MavenProject.class);
        if (mavenProject == null) {
            return bundleReader.loadManifest(project.getBasedir());
        }
        return bundleReader.loadManifest(mavenProject);
    }

    private BundleClassPath resolveClassPath(MavenSession session, MavenProject project) {
        logger.info("Resolving class path of " + project.getName());
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        List<AccessRule> strictBootClasspathAccessRules = new ArrayList<>();
        strictBootClasspathAccessRules.add(new DefaultAccessRule("java/**", false));
        DependencyArtifacts artifacts = getDependencyArtifacts(reactorProject);
        List<ClasspathEntry> classpath = new ArrayList<>();
        File bndFile = new File(project.getBasedir(), TychoConstants.PDE_BND);
        List<AccessRule> bootClasspathExtraAccessRules;
        ArtifactKey artifactKey = projectManager.getArtifactKey(project).get();
        if (bndFile.exists()) {
            bootClasspathExtraAccessRules = List.of();
            classpath.add(new DefaultClasspathEntry(reactorProject, artifactKey,
                    List.of(new File(project.getBuild().getOutputDirectory())), null));
            List<ArtifactDescriptor> bundles = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
            for (ArtifactDescriptor bundle : bundles) {
                //TODO we might want to compute the access rules based on the manifest exported packages
                Collection<AccessRule> rules = null;
                classpath.add(new DefaultClasspathEntry(bundle.getMavenProject(), bundle.getKey(),
                        List.of(bundle.fetchArtifact().join()), rules));
            }
        } else {
            DependenciesInfo dependenciesInfo = resolver.computeDependencies(project, artifacts, session);
            for (DependencyEntry entry : dependenciesInfo.getDependencyEntries()) {
                if (entry.isSystemBundle()) {
                    if (entry.getRules() != null) {
                        strictBootClasspathAccessRules.addAll(entry.getRules());
                    }
                }
                File location = entry.getLocation();
                if (location != null && location.exists()) {
                    ArtifactDescriptor otherArtifact = entry.getArtifactDescriptor();
                    if (otherArtifact != null) {
                        ReactorProject otherProject = otherArtifact.getMavenProject();
                        List<File> locations;
                        if (otherProject != null) {
                            locations = getOtherProjectClasspath(otherArtifact, otherProject, null);
                        } else {
                            locations = getBundleClasspath(otherArtifact);
                        }

                        if (locations.isEmpty() && !entry.getRules().isEmpty()) {
                            logger.warn("Empty classpath of required bundle " + otherArtifact);
                        }

                        classpath.add(new DefaultClasspathEntry(otherProject, otherArtifact.getKey(), locations,
                                entry.getRules()));
                    } else {
                        logger.debug("Cannot fetch artifact info for " + entry.getSymbolicName() + " and location "
                                + location + ", using raw jar item for classpath");
                        classpath.add(new DefaultClasspathEntry(null,
                                new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, entry.getSymbolicName(),
                                        entry.getVersion().toString()),
                                Collections.singletonList(location), entry.getRules()));
                    }
                }
            }

            // build.properties/jars.extra.classpath
            addExtraClasspathEntries(classpath, reactorProject, artifacts);

            // project itself 
            List<File> projectClasspath = getThisProjectClasspath(reactorProject, project);
            classpath.add(new DefaultClasspathEntry(reactorProject, artifactKey, projectClasspath, null));

            bootClasspathExtraAccessRules = dependenciesInfo.getBootClasspathExtraAccessRules();
        }
        addPDESourceRoots(project);
        return new BundleClassPath(classpath, strictBootClasspathAccessRules, bootClasspathExtraAccessRules);
    }

    private Collection<ClasspathEntry> computeExtraTestClasspath(ReactorProject reactorProject) {

        List<ClasspathEntry> list = new ArrayList<>();
        Collection<ProjectClasspathEntry> entries = getEclipsePluginProject(reactorProject).getClasspathEntries();
        for (ProjectClasspathEntry cpe : entries) {
            if (cpe instanceof JUnitClasspathContainerEntry junit) {
                logger.info("Resolving JUnit " + junit.getJUnitSegment() + " classpath container");

                for (JUnitBundle junitBundle : junit.getArtifacts().toList()) {
                    Optional<ResolvedArtifactKey> mavenBundle = mavenBundleResolver.resolveMavenBundle(
                            reactorProject.adapt(MavenProject.class), reactorProject.adapt(MavenSession.class),
                            ClasspathReader.toMaven(junitBundle));
                    mavenBundle.ifPresent(key -> {
                        list.add(new DefaultClasspathEntry(key,
                                Collections.singletonList(new DefaultAccessRule("**/*", false))));
                    });
                }
            }
        }
        return list;
    }

    private void addPDESourceRoots(MavenProject project) {
        EclipsePluginProject eclipsePluginProject = getEclipsePluginProject(DefaultReactorProject.adapt(project));
        for (BuildOutputJar outputJar : eclipsePluginProject.getOutputJars()) {
            for (File sourceFolder : outputJar.getSourceFolders()) {
                removeDuplicateTestCompileRoot(sourceFolder, project);
                project.addCompileSourceRoot(sourceFolder.getAbsolutePath());
            }
        }
    }

    private void removeDuplicateTestCompileRoot(File sourceFolder, MavenProject project) {
        for (String testCompileRoot : project.getTestCompileSourceRoots()) {
            if (sourceFolder.equals(new File(testCompileRoot))) {
                // avoid duplicate source folders (bug 368445)
                logger
                .debug("Removed duplicate test compile root " + testCompileRoot + " from maven project model");
                project.removeTestCompileSourceRoot(testCompileRoot);
                return;
            }
        }
    }

    @Override
    public EclipsePluginProject getEclipsePluginProject(ReactorProject otherProject) {
        if (otherProject == null) {
            return null;
        }
        EclipsePluginProjectImpl pdeProject = (EclipsePluginProjectImpl) otherProject
                .getContextValue(CTX_ECLIPSE_PLUGIN_PROJECT);
        if (pdeProject == null) {
            try {
                pdeProject = new EclipsePluginProjectImpl(otherProject, buildPropertiesParser.parse(otherProject),
                        classpathParser.parse(otherProject.getBasedir()));
                if (otherProject instanceof DefaultReactorProject defaultReactorProject) {
                    populateProperties(defaultReactorProject.project.getProperties(), pdeProject);
                }
                otherProject.setContextValue(CTX_ECLIPSE_PLUGIN_PROJECT, pdeProject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return pdeProject;
    }

    /**
     * Add to maven project some properties storing info about the plugin project model, for easier
     * reuse in further mojos (eg combining with eclipse-run and PDE API Tools).
     * 
     * @param mavenProjectProperties
     * @param pdeProject
     */
    private void populateProperties(Properties mavenProjectProperties, EclipsePluginProjectImpl pdeProject) {
        // properties are retained and must not be too big, at the risk of consuming too much memory
        mavenProjectProperties.put("tychoProject.build.outputDirectories",
                pdeProject.getOutputJars().stream().map(BuildOutputJar::getOutputDirectory).map(File::getAbsolutePath)
                        .collect(Collectors.joining(File.pathSeparator)));
    }

    @Override
    public List<ClasspathEntry> getClasspath(ReactorProject project) {
        return getBundleClassPath(project).getClasspathEntries();
    }

    @Override
    public List<ClasspathEntry.AccessRule> getBootClasspathExtraAccessRules(ReactorProject project) {
        return getBundleClassPath(project).getExtraBootClasspathAccessRules();
    }

    public BundleClassPath getBundleClassPath(ReactorProject project) {
        return project.computeContextValue(CTX_CLASSPATH,
                () -> resolveClassPath(getMavenSession(project), getMavenProject(project)));
    }

    /**
     * Returns project compile classpath entries.
     */
    private List<File> getThisProjectClasspath(ReactorProject project, MavenProject mavenProject) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        EclipsePluginProject pdeProject = getEclipsePluginProject(project);

        Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();

        // unconditionally add all output jars (even if does not exist or not on Bundle-ClassPath)
        for (BuildOutputJar jar : outputJars.values()) {
            classpath.add(jar.getOutputDirectory());
        }

        // Bundle-ClassPath entries that do not have associated output folders
        // => assume it's checked into SCM or will be copied here later during build
        OsgiManifest mf = bundleReader.loadManifest(mavenProject);
        for (String cp : mf.getBundleClasspath()) {
            if (!outputJars.containsKey(cp)) {
                classpath.add(new File(project.getBasedir(), cp));
            }
        }
        return new ArrayList<>(classpath);
    }

    /**
     * Returns bundle classpath entries. If <code>nestedPath</code> is not <code>null</code>,
     * returns single class folder that corresponds specified nestedPath. If <code>nestedPath</code>
     * is <code>null</code>, returns entries specified in Bundle-ClassPath.
     */
    private List<File> getOtherProjectClasspath(ArtifactDescriptor bundle, ReactorProject otherProject,
            String nestedPath) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        EclipsePluginProject pdeProject = getEclipsePluginProject(otherProject);

        Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();
        String[] bundleClassPath;
        if (nestedPath == null) {
            bundleClassPath = parseBundleClasspath(bundle);
        } else {
            bundleClassPath = new String[] { nestedPath };
        }
        for (String cp : bundleClassPath) {
            if (outputJars.containsKey(cp)) {
                // add output folder even if it does not exist (yet)
                classpath.add(outputJars.get(cp).getOutputDirectory());
            } else {
                // no associated output folder 
                // => assume it's checked into SCM or will be copied here later during build
                classpath.add(new File(otherProject.getBasedir(), cp));
            }
        }
        return new ArrayList<>(classpath);
    }

    private void addExtraClasspathEntries(List<ClasspathEntry> classpath, ReactorProject project,
            DependencyArtifacts artifacts) {
        EclipsePluginProject pdeProject = getEclipsePluginProject(project);
        Collection<BuildOutputJar> outputJars = pdeProject.getOutputJarMap().values();
        for (BuildOutputJar buildOutputJar : outputJars) {
            List<String> entries = buildOutputJar.getExtraClasspathEntries();
            for (String entry : entries) {
                Pattern platformURL = Pattern.compile("platform:/(plugin|fragment)/([^/]*)/*(.*)");
                Matcher m = platformURL.matcher(entry.trim());
                String bundleId = null;
                String path = null;
                if (m.matches()) {
                    bundleId = m.group(2).trim();
                    path = m.group(3).trim();

                    if (path != null && path.isEmpty()) {
                        path = null;
                    }

                    ArtifactDescriptor matchingBundle = artifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN,
                            bundleId, null);
                    if (matchingBundle != null) {
                        classpath.add(addBundleToClasspath(matchingBundle, path));
                    } else {
                        logger.warn("Missing extra classpath entry " + entry.trim());
                    }
                } else {
                    entry = entry.trim();
                    File file = new File(project.getBasedir(), entry).getAbsoluteFile();
                    if (file.exists()) {
                        List<File> locations = Collections.singletonList(file);
                        ArtifactKey projectKey = getArtifactKey(project);
                        classpath.add(new DefaultClasspathEntry(project, projectKey, locations, null));
                    } else {
                        logger.warn("Missing extra classpath entry " + entry);
                    }
                }
            }
        }
        for (ProjectClasspathEntry entry : pdeProject.getClasspathEntries()) {
            if (entry instanceof LibraryClasspathEntry libraryClasspathEntry) {
                File path = libraryClasspathEntry.getLibraryPath();
                if (path.exists()) {
                    classpath.add(new DefaultClasspathEntry(null,
                            readOrCreateArtifactKey(path, () -> new DefaultArtifactKey("jar", path.getAbsolutePath())),
                            Collections.singletonList(path), null));
                }
            }
        }
        Collection<TargetEnvironment> environments = projectManager
                .getTargetEnvironments(project.adapt(MavenProject.class));
        //Fragments are like embedded dependencies...
        for (ArtifactDescriptor fragment : artifacts.getFragments()) {
            File location = fragment.getLocation(true);
            if (location != null) {
                OsgiManifest manifest = bundleReader.loadManifest(location);
                Filter filter = manifest.getTargetEnvironmentFilter();
                if (filter == null || environments.stream().anyMatch(env -> env.match(filter))) {
                    classpath.add(new DefaultClasspathEntry(null, readArtifactKey(location),
                            Collections.singletonList(location), null));
                }
            }
        }
    }

    protected DefaultClasspathEntry addBundleToClasspath(ArtifactDescriptor matchingBundle, String path) {
        List<File> locations;
        if (matchingBundle.getMavenProject() != null) {
            locations = getOtherProjectClasspath(matchingBundle, matchingBundle.getMavenProject(), path);
        } else if (path != null) {
            locations = getBundleEntry(matchingBundle, path);
        } else {
            locations = getBundleClasspath(matchingBundle);
        }
        return new DefaultClasspathEntry(matchingBundle.getMavenProject(), matchingBundle.getKey(), locations, null);
    }

    private List<File> getBundleClasspath(ArtifactDescriptor bundle) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        for (String cp : parseBundleClasspath(bundle)) {
            File entry;
            if (".".equals(cp)) {
                entry = bundle.getLocation(true);
            } else {
                entry = getNestedJarOrDir(bundle, cp);
            }

            if (entry != null) {
                classpath.add(entry);
            }
        }

        return new ArrayList<>(classpath);
    }

    private List<File> getBundleEntry(ArtifactDescriptor bundle, String nestedPath) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        File entry;
        if (".".equals(nestedPath)) {
            entry = bundle.getLocation(true);
        } else {
            entry = getNestedJarOrDir(bundle, nestedPath);
        }

        if (entry != null) {
            classpath.add(entry);
        }

        return new ArrayList<>(classpath);
    }

    private String[] parseBundleClasspath(ArtifactDescriptor bundle) {
        File location = bundle.getLocation(true);
        OsgiManifest mf = bundleReader.loadManifest(location);
        return mf.getBundleClasspath();
    }

    private File getNestedJarOrDir(ArtifactDescriptor bundle, String cp) {
        return bundleReader.getEntry(bundle.getLocation(true), cp);
    }

    @Override
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project) {
        return getManifest(DefaultReactorProject.adapt(project)).getImplicitTargetEnvironment();
    }

    @Override
    public Filter getTargetEnvironmentFilter(MavenProject project) {
        return getManifest(DefaultReactorProject.adapt(project)).getTargetEnvironmentFilter();
    }

    @Override
    public void readExecutionEnvironmentConfiguration(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        // read packaging-type independent configuration
        super.readExecutionEnvironmentConfiguration(project, mavenSession, sink);

        // only in plugin projects, the profile may also be ...
        // ... specified in build.properties (for PDE compatibility)
        String pdeProfileName = getEclipsePluginProject(project).getBuildProperties().getJreCompilationProfile();
        if (pdeProfileName != null) {
            sink.setProfileConfiguration(pdeProfileName.trim(), "build.properties");
        } else {
            // ... derived from BREE in bundle manifest
            String[] manifestBREEs = getManifest(project).getExecutionEnvironments();
            if (manifestBREEs.length == 1) {
                applyBestOfCurrentOrConfiguredProfile(manifestBREEs[0],
                        "Bundle-RequiredExecutionEnvironment (unique entry)", mavenSession, sink);
            } else if (manifestBREEs.length > 1) {
                TargetPlatformConfiguration tpConfiguration = projectManager.getTargetPlatformConfiguration(project);
                switch (tpConfiguration.getBREEHeaderSelectionPolicy()) {
                case first:
                    applyBestOfCurrentOrConfiguredProfile(manifestBREEs[0],
                            "Bundle-RequiredExecutionEnvironment (first entry)", mavenSession, sink);
                    break;
                case minimal:
                    Arrays.stream(manifestBREEs) //
                            .map(ee -> ExecutionEnvironmentUtils.getExecutionEnvironment(ee, toolchainManager,
                                    mavenSession, logger)) //
                            .sorted() //
                            .findFirst() //
                            .ifPresent(ee -> applyBestOfCurrentOrConfiguredProfile(ee.getProfileName(),
                                    "Bundle-RequiredExecutionEnvironment (minimal entry)", mavenSession, sink));
                }
            }
        }
    }

    private void applyBestOfCurrentOrConfiguredProfile(String configuredProfileName, String reason,
            MavenSession mavenSession, ExecutionEnvironmentConfiguration sink) {
        StandardExecutionEnvironment configuredProfile = ExecutionEnvironmentUtils
                .getExecutionEnvironment(configuredProfileName, toolchainManager, mavenSession, logger);
        if (configuredProfile == null) {
            //should never be the case as Tycho delegates to other profiles, but if we need to stick to the defaults...
            return;
        }
        if (sink.isResolveWithEEConstraints()) {
            sink.setProfileConfiguration(configuredProfileName, reason);
        } else {
            StandardExecutionEnvironment currentProfile = ExecutionEnvironmentUtils.getExecutionEnvironment(
                    "JavaSE-" + Runtime.version().feature(), toolchainManager, mavenSession, logger);
            if (currentProfile != null && currentProfile.compareTo(configuredProfile) > 0) {
                sink.setProfileConfiguration(currentProfile.getProfileName(),
                        "Currently running profile, newer than configured profile (" + configuredProfileName
                                + ") from [" + reason + "]");
            } else {
                sink.setProfileConfiguration(configuredProfileName, reason);
            }
        }
    }

    @Override
    public List<ClasspathEntry> getTestClasspath(ReactorProject reactorProject) {
        return getTestClasspath(reactorProject, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClasspathEntry> getTestClasspath(ReactorProject reactorProject, boolean complete) {
        List<ClasspathEntry> classpath = (List<ClasspathEntry>) reactorProject
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH);
        if (classpath == null) {
            List<ClasspathEntry> testClasspath = new ArrayList<>(getClasspath(reactorProject));
            Collection<ClasspathEntry> extraTestClasspath = computeExtraTestClasspath(reactorProject);
            reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_EXTRA_CLASSPATH, extraTestClasspath);
            testClasspath.addAll(extraTestClasspath);
            reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH, testClasspath);
            return testClasspath;
        }
        if (complete) {
            return classpath;
        } else {
            return (List<ClasspathEntry>) reactorProject
                    .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_EXTRA_CLASSPATH);
        }
    }

    @Override
    public DependencyArtifacts getTestDependencyArtifacts(ReactorProject reactorProject) {
        return reactorProject.computeContextValue(TychoConstants.CTX_TEST_DEPENDENCY_ARTIFACTS, () -> {
            List<ArtifactKey> testDependencies = getExtraTestRequirements(reactorProject);
            if (testDependencies.isEmpty()) {
                return new DefaultDependencyArtifacts();
            }
            logger.info("Resolving test dependencies of " + reactorProject);
            MavenSession mavenSession = getMavenSession(reactorProject);
            MavenProject mavenProject = getMavenProject(reactorProject);
            TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(mavenProject);
            DependencyResolverConfiguration resolverConfiguration = configuration.getDependencyResolverConfiguration();
            DependencyResolverConfiguration testResolverConfiguration = new DependencyResolverConfiguration() {
                @Override
                public OptionalResolutionAction getOptionalResolutionAction() {
                    return resolverConfiguration.getOptionalResolutionAction();
                }

                @Override
                public List<ArtifactKey> getAdditionalArtifacts() {
                    ArrayList<ArtifactKey> res = new ArrayList<>(resolverConfiguration.getAdditionalArtifacts());
                    res.addAll(testDependencies);
                    return res;
                }

                @Override
                public Collection<IRequirement> getAdditionalRequirements() {
                    return resolverConfiguration.getAdditionalRequirements();
                }
            };
            TargetPlatform preliminaryTargetPlatform = dependencyResolver.getPreliminaryTargetPlatform(mavenSession,
                    mavenProject);
            return dependencyResolver.resolveDependencies(mavenSession, mavenProject, preliminaryTargetPlatform,
                    testResolverConfiguration, configuration.getEnvironments());
        });
    }

    @Override
    public List<ArtifactKey> getExtraTestRequirements(ReactorProject project) {
        List<ArtifactKey> list = new ArrayList<>();
        Collection<ProjectClasspathEntry> entries = getEclipsePluginProject(project).getClasspathEntries();
        for (ProjectClasspathEntry cpe : entries) {
            if (cpe instanceof JUnitClasspathContainerEntry junitEntry) {
                list.addAll(ClasspathReader.asMaven(junitEntry.getArtifacts().toList()));
            }
        }
        return list;
    }

}
