/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich -  [Bug 572416] Tycho does not understand "additional.bundles" directive in build.properties
 *                          [Bug 572416] Compile all source folders contained in .classpath
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.dotClasspath.ClasspathParser;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.UpdateSite;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_PLUGIN)
public class OsgiBundleProject extends AbstractTychoProject implements BundleProject {

    private static final String CTX_ARTIFACT_KEY = TychoConstants.CTX_BASENAME + "/osgiBundle/artifactKey";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    private ClasspathParser classpathParser;

    @Requirement
    private EquinoxResolver resolver;

    @Requirement
    private Logger logger;

    @Requirement
    private ToolchainManager toolchainManager;

    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        return getDependencyWalker(project);
    }

    @Override
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project) {
        final DependencyArtifacts artifacts = getDependencyArtifacts(project);

        final List<ClasspathEntry> cp = getClasspath(project);

        return new ArtifactDependencyWalker() {
            @Override
            public void walk(ArtifactDependencyVisitor visitor) {
                for (ClasspathEntry entry : cp) {
                    ArtifactDescriptor artifact = artifacts.getArtifact(entry.getArtifactKey());
                    ArtifactKey key = artifact.getKey();
                    File location = artifact.getLocation(true);
                    ReactorProject project = artifact.getMavenProject();
                    String classifier = artifact.getClassifier();
                    Set<Object> installableUnits = artifact.getInstallableUnits();

                    PluginDescription plugin = new DefaultPluginDescription(key, location, project, classifier, null,
                            installableUnits);

                    visitor.visitPlugin(plugin);
                }
            }

            @Override
            public void traverseFeature(File location, Feature feature, ArtifactDependencyVisitor visitor) {
            }

            @Override
            public void traverseUpdateSite(UpdateSite site, ArtifactDependencyVisitor artifactDependencyVisitor) {
            }

            @Override
            public void traverseProduct(ProductConfiguration productConfiguration, ArtifactDependencyVisitor visitor) {
            }
        };
    }

    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        ArtifactKey key = (ArtifactKey) project.getContextValue(CTX_ARTIFACT_KEY);
        if (key == null) {
            throw new IllegalStateException("Project has not been setup yet " + project.toString());
        }

        return key;
    }

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        ArtifactKey key = readArtifactKey(project.getBasedir());
        DefaultReactorProject.adapt(project).setContextValue(CTX_ARTIFACT_KEY, key);
    }

    public ArtifactKey readArtifactKey(File location) {
        OsgiManifest mf = bundleReader.loadManifest(location);
        return mf.toArtifactKey();
    }

    @Override
    public String getManifestValue(String key, MavenProject project) {
        return getManifest(DefaultReactorProject.adapt(project)).getValue(key);
    }

    private OsgiManifest getManifest(ReactorProject project) {
        return bundleReader.loadManifest(project.getBasedir());
    }

    @Override
    public void resolveClassPath(MavenSession session, MavenProject project) {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        DependencyArtifacts artifacts = getDependencyArtifacts(reactorProject);

        ModuleContainer state = getResolverState(reactorProject, artifacts, session);

//        if (getLogger().isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
//            getLogger().debug(resolver.toDebugString(state));
//        }

        Module module = state.getModule(project.getBasedir().getAbsolutePath());
        if (module == null) {
            Module systemModule = state.getModule(Constants.SYSTEM_BUNDLE_LOCATION);
            if (project.getBasedir().equals(systemModule.getCurrentRevision().getRevisionInfo())) {
                module = systemModule;
            }
        }
        ModuleRevision bundleDescription = module.getCurrentRevision();

        List<ClasspathEntry> classpath = new ArrayList<>();

        // dependencies
        List<AccessRule> strictBootClasspathAccessRules = new ArrayList<>();
        strictBootClasspathAccessRules.add(new DefaultAccessRule("java/**", false));
        DependencyComputer dependencyComputer = new DependencyComputer(state);
        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(bundleDescription);
        for (DependencyEntry entry : dependencies) {
            if (Constants.SYSTEM_BUNDLE_ID == entry.module.getRevisions().getModule().getId()) {
                if (entry.rules != null) {
                    strictBootClasspathAccessRules.addAll(entry.rules);
                }
            }
            File location = (File) entry.module.getRevisionInfo();
            if (location != null && location.exists()) {
                ArtifactDescriptor otherArtifact = getArtifact(artifacts, location, entry.module.getSymbolicName());
                ReactorProject otherProject = otherArtifact.getMavenProject();
                List<File> locations;
                if (otherProject != null) {
                    locations = getOtherProjectClasspath(otherArtifact, otherProject, null);
                } else {
                    locations = getBundleClasspath(otherArtifact);
                }

                if (locations.isEmpty() && !entry.rules.isEmpty()) {
                    getLogger().warn("Empty classpath of required bundle " + otherArtifact);
                }

                classpath.add(new DefaultClasspathEntry(otherProject, otherArtifact.getKey(), locations, entry.rules));
            }
        }

        // build.properties/jars.extra.classpath
        addExtraClasspathEntries(classpath, reactorProject, artifacts);

        // project itself
        ArtifactDescriptor artifact = getArtifact(artifacts, project.getBasedir(), bundleDescription.getSymbolicName());
        List<File> projectClasspath = getThisProjectClasspath(artifact, reactorProject);
        classpath.add(new DefaultClasspathEntry(reactorProject, artifact.getKey(), projectClasspath, null));

        reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH, classpath);
        // Tests
        List<ClasspathEntry> testClasspath = classpath; // TODO: Currently, there is no specific test "scope" -dependencies, classpath- so we just set the regular
        // deps and classpath. But in further changes, those scopes will be set according to extra dependencies
        // eg from .classpath
        reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH, testClasspath);

        reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_STRICT_BOOTCLASSPATH_ACCESSRULES,
                strictBootClasspathAccessRules);
        reactorProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_BOOTCLASSPATH_EXTRA_ACCESSRULES,
                dependencyComputer.computeBootClasspathExtraAccessRules(state));

        addPDESourceRoots(project);
    }

    protected ArtifactDescriptor getArtifact(DependencyArtifacts artifacts, File location, String id) {
        Map<String, ArtifactDescriptor> classified = artifacts.getArtifact(location);
        if (classified != null) {
            for (ArtifactDescriptor artifact : classified.values()) {
                if (id.equals(artifact.getKey().getId())) {
                    return artifact;
                }
            }
        }
        return null;
    }

    private void addPDESourceRoots(MavenProject project) {
        EclipsePluginProjectImpl eclipsePluginProject = getEclipsePluginProject(DefaultReactorProject.adapt(project));
        for (BuildOutputJar outputJar : eclipsePluginProject.getOutputJars()) {
            for (File sourceFolder : outputJar.getSourceFolders()) {
                removeDuplicateTestCompileRoot(sourceFolder, project.getTestCompileSourceRoots());
                project.addCompileSourceRoot(sourceFolder.getAbsolutePath());
            }
        }
    }

    private void removeDuplicateTestCompileRoot(File sourceFolder, List<String> testCompileSourceRoots) {
        for (Iterator<String> iterator = testCompileSourceRoots.iterator(); iterator.hasNext();) {
            String testCompileRoot = iterator.next();
            if (sourceFolder.equals(new File(testCompileRoot))) {
                // avoid duplicate source folders (bug 368445)
                iterator.remove();
                getLogger()
                        .debug("Removed duplicate test compile root " + testCompileRoot + " from maven project model");
                return;
            }
        }
    }

    private ModuleContainer getResolverState(ReactorProject project, DependencyArtifacts artifacts,
            MavenSession session) {
        try {
            ExecutionEnvironmentConfiguration eeConfiguration = TychoProjectUtils
                    .getExecutionEnvironmentConfiguration(project);
            ExecutionEnvironment executionEnvironment = eeConfiguration.getFullSpecification();
            return resolver.newResolvedState(project, session,
                    eeConfiguration.isIgnoredByResolver() ? null : executionEnvironment, artifacts);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    public EclipsePluginProjectImpl getEclipsePluginProject(ReactorProject otherProject) {
        EclipsePluginProjectImpl pdeProject = (EclipsePluginProjectImpl) otherProject
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT);
        if (pdeProject == null) {
            try {
                pdeProject = new EclipsePluginProjectImpl(otherProject,
                        buildPropertiesParser.parse(otherProject.getBasedir()),
                        classpathParser.parse(otherProject.getBasedir()));
                if (otherProject instanceof DefaultReactorProject) {
                    populateProperties(((DefaultReactorProject) otherProject).project.getProperties(), pdeProject);
                }
                otherProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, pdeProject);
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
        @SuppressWarnings("unchecked")
        List<ClasspathEntry> classpath = (List<ClasspathEntry>) project
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH);
        if (classpath == null) {
            throw new IllegalStateException();
        }
        return classpath;
    }

    @Override
    public List<ClasspathEntry.AccessRule> getBootClasspathExtraAccessRules(ReactorProject project) {
        @SuppressWarnings("unchecked")
        List<ClasspathEntry.AccessRule> rules = (List<AccessRule>) project
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_BOOTCLASSPATH_EXTRA_ACCESSRULES);
        if (rules == null) {
            throw new IllegalStateException();
        }
        return rules;
    }

    /**
     * Returns project compile classpath entries.
     */
    private List<File> getThisProjectClasspath(ArtifactDescriptor bundle, ReactorProject project) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        EclipsePluginProject pdeProject = getEclipsePluginProject(project);

        Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();

        // unconditionally add all output jars (even if does not exist or not on Bundle-ClassPath)
        for (BuildOutputJar jar : outputJars.values()) {
            classpath.add(jar.getOutputDirectory());
        }

        // Bundle-ClassPath entries that do not have associated output folders
        // => assume it's checked into SCM or will be copied here later during build
        for (String cp : parseBundleClasspath(bundle)) {
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
                        getLogger().warn("Missing extra classpath entry " + entry.trim());
                    }
                } else {
                    entry = entry.trim();
                    File file = new File(project.getBasedir(), entry).getAbsoluteFile();
                    if (file.exists()) {
                        List<File> locations = Collections.singletonList(file);
                        ArtifactKey projectKey = getArtifactKey(project);
                        classpath.add(new DefaultClasspathEntry(project, projectKey, locations, null));
                    } else {
                        getLogger().warn("Missing extra classpath entry " + entry);
                    }
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
        OsgiManifest mf = bundleReader.loadManifest(bundle.getLocation(true));
        return mf.getBundleClasspath();
    }

    private File getNestedJarOrDir(ArtifactDescriptor bundle, String cp) {
        return bundleReader.getEntry(bundle.getLocation(true), cp);
    }

    @Override
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project) {
        String filterStr = getManifestValue(EclipsePlatformNamespace.ECLIPSE_PLATFORM_FILTER_HEADER, project);

        if (filterStr != null) {
            try {
                FilterImpl filter = FilterImpl.newInstance(filterStr);

                String ws = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_WS));
                String os = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_OS));
                String arch = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_ARCH));

                // validate if os/ws/arch are not null and actually match the filter
                if (ws != null && os != null && arch != null) {
                    Map<String, String> properties = new HashMap<>();
                    properties.put(PlatformPropertiesUtils.OSGI_WS, ws);
                    properties.put(PlatformPropertiesUtils.OSGI_OS, os);
                    properties.put(PlatformPropertiesUtils.OSGI_ARCH, arch);

                    if (filter.matches(properties)) {
                        return new TargetEnvironment(os, ws, arch);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // at least we tried...
            }
        }

        return null;
    }

    private static String sn(String str) {
        if (str != null && !str.isBlank()) {
            return str;
        }
        return null;
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
                TargetPlatformConfiguration tpConfiguration = TychoProjectUtils.getTargetPlatformConfiguration(project);
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
        if (configuredProfile != null) {
            // non standard profile, stick to it
            sink.setProfileConfiguration(configuredProfileName, reason);
        }
        StandardExecutionEnvironment currentProfile = ExecutionEnvironmentUtils.getExecutionEnvironment(
                "JavaSE-" + Runtime.version().feature(), toolchainManager, mavenSession, logger);
        if (currentProfile.compareTo(configuredProfile) > 0) {
            sink.setProfileConfiguration(currentProfile.getProfileName(),
                    "Currently running profile, newer than configured profile (" + configuredProfileName + ") from ["
                            + reason + "]");
        } else {
            sink.setProfileConfiguration(configuredProfileName, reason);
        }
    }

    @Override
    public List<ClasspathEntry> getTestClasspath(ReactorProject reactorProject) {
        @SuppressWarnings("unchecked")
        List<ClasspathEntry> classpath = (List<ClasspathEntry>) reactorProject
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_TEST_CLASSPATH);
        if (classpath == null) {
            throw new IllegalStateException();
        }
        return classpath;
    }

    public DependencyArtifacts getTestDependencyArtifacts(ReactorProject project) {
        return TychoProjectUtils.getTestDependencyArtifacts(project);
    }

}
