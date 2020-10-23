/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
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
    private EquinoxResolver resolver;

    @Requirement
    private DependencyComputer dependencyComputer;

    @Requirement
    private Logger logger;

    @Requirement
    private ToolchainManager toolchainManager;

    private MavenSession session;

    @Override
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project, TargetEnvironment environment) {
        return getDependencyWalker(project);
    }

    @Override
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project) {
        final DependencyArtifacts artifacts = getDependencyArtifacts(project);

        final List<ClasspathEntry> cp = getClasspath(project);

        return new ArtifactDependencyWalker() {
            @Override
            public void walk(ArtifactDependencyVisitor visitor) {
                for (ClasspathEntry entry : cp) {
                    ArtifactDescriptor artifact = artifacts.getArtifact(entry.getArtifactKey());

                    ArtifactKey key = artifact.getKey();
                    ReactorProject project = artifact.getMavenProject();
                    String classifier = artifact.getClassifier();
                    Set<Object> installableUnits = artifact.getInstallableUnits();

                    PluginDescription plugin = new DefaultPluginDescription(key, artifact::getLocation, project,
                            classifier, null, installableUnits);

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
        this.session = session;
        ArtifactKey key = readArtifactKey(project.getBasedir());
        project.setContextValue(CTX_ARTIFACT_KEY, key);
    }

    public ArtifactKey readArtifactKey(File location) {
        OsgiManifest mf = bundleReader.loadManifest(location);
        return mf.toArtifactKey();
    }

    @Override
    public String getManifestValue(String key, MavenProject project) {
        return getManifest(project).getValue(key);
    }

    private OsgiManifest getManifest(MavenProject project) {
        return bundleReader.loadManifest(project.getBasedir());
    }

    private void resolveClassPath(MavenSession session, MavenProject project) {
        DependencyArtifacts artifacts = getDependencyArtifacts(project);

        State state = getResolverState(project, artifacts, session);

        if (getLogger().isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            getLogger().debug(resolver.toDebugString(state));
        }

        BundleDescription bundleDescription = state.getBundleByLocation(project.getBasedir().getAbsolutePath());

        List<ClasspathEntry> classpath = new ArrayList<>();

        // dependencies
        List<AccessRule> strictBootClasspathAccessRules = new ArrayList<>();
        strictBootClasspathAccessRules.add(new DefaultAccessRule("java/**", false));
        for (DependencyEntry entry : dependencyComputer.computeDependencies(state.getStateHelper(),
                bundleDescription)) {
            if (Constants.SYSTEM_BUNDLE_ID == entry.desc.getBundleId()) {
                if (entry.rules != null) {
                    strictBootClasspathAccessRules.addAll(entry.rules);
                }
                if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(entry.desc.getSymbolicName())) {
                    // synthetic system.bundle has no filesystem location
                    continue;
                }
            }
            File location = new File(entry.desc.getLocation());
            ArtifactDescriptor otherArtifact = getArtifact(artifacts, location, entry.desc.getSymbolicName());
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

        // build.properties/jars.extra.classpath
        ReactorProject projectProxy = DefaultReactorProject.adapt(project);
        addExtraClasspathEntries(classpath, projectProxy, artifacts);

        // project itself
        ArtifactDescriptor artifact = getArtifact(artifacts, project.getBasedir(), bundleDescription.getSymbolicName());
        List<File> projectClasspath = getThisProjectClasspath(artifact, projectProxy);
        classpath.add(new DefaultClasspathEntry(projectProxy, artifact.getKey(), projectClasspath, null));

        project.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH, classpath);

        project.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_STRICT_BOOTCLASSPATH_ACCESSRULES,
                strictBootClasspathAccessRules);
        project.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_BOOTCLASSPATH_EXTRA_ACCESSRULES,
                dependencyComputer.computeBootClasspathExtraAccessRules(state.getStateHelper(), bundleDescription));

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

    private State getResolverState(MavenProject project, DependencyArtifacts artifacts, MavenSession session) {
        try {
            ExecutionEnvironmentConfiguration eeConfiguration = TychoProjectUtils
                    .getExecutionEnvironmentConfiguration(project);
            ExecutionEnvironment executionEnvironment = eeConfiguration.getFullSpecification();
            return resolver.newResolvedState(project, session, executionEnvironment,
                    eeConfiguration.isIgnoredByResolver(), artifacts);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    public EclipsePluginProjectImpl getEclipsePluginProject(ReactorProject otherProject) {
        EclipsePluginProjectImpl pdeProject = (EclipsePluginProjectImpl) otherProject
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT);
        if (pdeProject == null) {
            try {
                pdeProject = new EclipsePluginProjectImpl(otherProject, buildPropertiesParser);
                otherProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, pdeProject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return pdeProject;
    }

    @Override
    public List<ClasspathEntry> getClasspath(MavenProject project) {
        if (project.getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH) == null) {
            resolveClassPath(session, project);
        }
        @SuppressWarnings("unchecked")
        List<ClasspathEntry> classpath = (List<ClasspathEntry>) project
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH);
        if (classpath == null) {
            throw new IllegalStateException();
        }
        return classpath;
    }

    @Override
    public List<ClasspathEntry.AccessRule> getBootClasspathExtraAccessRules(MavenProject project) {
        if (project.getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_BOOTCLASSPATH_EXTRA_ACCESSRULES) == null) {
            resolveClassPath(session, project);
        }
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

                    if (path != null && path.length() <= 0) {
                        path = null;
                    }

                    ArtifactDescriptor matchingBundle = artifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN,
                            bundleId, null);
                    if (matchingBundle != null) {
                        List<File> locations;
                        if (matchingBundle.getMavenProject() != null) {
                            locations = getOtherProjectClasspath(matchingBundle, matchingBundle.getMavenProject(),
                                    path);
                        } else if (path != null) {
                            locations = getBundleEntry(matchingBundle, path);
                        } else {
                            locations = getBundleClasspath(matchingBundle);
                        }
                        classpath.add(new DefaultClasspathEntry(matchingBundle.getMavenProject(),
                                matchingBundle.getKey(), locations, null));
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

    private List<File> getBundleClasspath(ArtifactDescriptor bundle) {
        LinkedHashSet<File> classpath = new LinkedHashSet<>();

        for (String cp : parseBundleClasspath(bundle)) {
            File entry;
            if (".".equals(cp)) {
                entry = bundle.getLocation();
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
            entry = bundle.getLocation();
        } else {
            entry = getNestedJarOrDir(bundle, nestedPath);
        }

        if (entry != null) {
            classpath.add(entry);
        }

        return new ArrayList<>(classpath);
    }

    private String[] parseBundleClasspath(ArtifactDescriptor bundle) {
        OsgiManifest mf = bundleReader.loadManifest(bundle.getLocation());
        return mf.getBundleClasspath();
    }

    private File getNestedJarOrDir(ArtifactDescriptor bundle, String cp) {
        return bundleReader.getEntry(bundle.getLocation(), cp);
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
        if (str != null && !"".equals(str.trim())) {
            return str;
        }
        return null;
    }

    @Override
    public void readExecutionEnvironmentConfiguration(MavenProject project, MavenSession mavenSession,
            ExecutionEnvironmentConfiguration sink) {
        // read packaging-type independent configuration
        super.readExecutionEnvironmentConfiguration(project, mavenSession, sink);

        // only in plugin projects, the profile may also be ...
        // ... specified in build.properties (for PDE compatibility)
        String pdeProfileName = getEclipsePluginProject(DefaultReactorProject.adapt(project)).getBuildProperties()
                .getJreCompilationProfile();
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
}
