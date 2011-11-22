/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.UnknownEnvironmentException;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.UpdateSite;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

@Component(role = TychoProject.class, hint = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN)
public class OsgiBundleProject extends AbstractTychoProject implements BundleProject {

    private static final String CTX_ARTIFACT_KEY = TychoConstants.CTX_BASENAME + "/osgiBundle/artifactKey";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private EquinoxResolver resolver;

    @Requirement
    private DependencyComputer dependencyComputer;

    public ArtifactDependencyWalker getDependencyWalker(MavenProject project, TargetEnvironment environment) {
        return getDependencyWalker(project);
    }

    public ArtifactDependencyWalker getDependencyWalker(MavenProject project) {
        final TargetPlatform platform = getTargetPlatform(project);

        final List<ClasspathEntry> cp = getClasspath(project);

        return new ArtifactDependencyWalker() {
            public void walk(ArtifactDependencyVisitor visitor) {
                for (ClasspathEntry entry : cp) {
                    ArtifactDescriptor artifact = platform.getArtifact(entry.getArtifactKey());

                    ArtifactKey key = artifact.getKey();
                    File location = artifact.getLocation();
                    ReactorProject project = artifact.getMavenProject();
                    String classifier = artifact.getClassifier();
                    Set<Object> installableUnits = artifact.getInstallableUnits();

                    PluginDescription plugin = new DefaultPluginDescription(key, location, project, classifier, null,
                            installableUnits);

                    visitor.visitPlugin(plugin);
                }
            }

            public void traverseFeature(File location, Feature feature, ArtifactDependencyVisitor visitor) {
            }

            public void traverseUpdateSite(UpdateSite site, ArtifactDependencyVisitor artifactDependencyVisitor) {
            }

            public void traverseProduct(ProductConfiguration productConfiguration, ArtifactDependencyVisitor visitor) {
            }
        };
    }

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

        if (key == null) {
            throw new IllegalArgumentException("Missing bundle symbolic name or version for project "
                    + project.toString());
        }

        project.setContextValue(CTX_ARTIFACT_KEY, key);
    }

    public ArtifactKey readArtifactKey(File location) {
        OsgiManifest mf = bundleReader.loadManifest(location);
        return DefaultArtifactKey.fromManifest(mf);
    }

    public String getManifestValue(String key, MavenProject project) {
        return getManifest(project).getValue(key);
    }

    private OsgiManifest getManifest(MavenProject project) {
        return bundleReader.loadManifest(project.getBasedir());
    }

    @Override
    public void resolveClassPath(MavenSession session, MavenProject project) {
        TargetPlatform platform = getTargetPlatform(project);

        State state = getResolverState(project, platform);

        if (getLogger().isDebugEnabled() && DebugUtils.isDebugEnabled(session, project)) {
            getLogger().debug(resolver.toDebugString(state));
        }

        BundleDescription bundleDescription;
        try {
            bundleDescription = state.getBundleByLocation(project.getBasedir().getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<ClasspathEntry> classpath = new ArrayList<ClasspathEntry>();

        // project itself
        ArtifactDescriptor artifact = platform.getArtifact(project.getBasedir());
        ReactorProject projectProxy = DefaultReactorProject.adapt(project);
        List<File> projectClasspath = getThisProjectClasspath(artifact, projectProxy);
        classpath.add(new DefaultClasspathEntry(projectProxy, artifact.getKey(), projectClasspath, null));

        // build.properties/jars.extra.classpath
        addExtraClasspathEntries(classpath, projectProxy, platform);

        // dependencies
        for (DependencyEntry entry : dependencyComputer.computeDependencies(state.getStateHelper(), bundleDescription)) {
            File location = new File(entry.desc.getLocation());
            ArtifactDescriptor otherArtifact = platform.getArtifact(location);
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
        project.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH, classpath);
        addPDESourceRoots(project);
    }

    private void addPDESourceRoots(MavenProject project) {
        EclipsePluginProjectImpl eclipsePluginProject = getEclipsePluginProject(DefaultReactorProject.adapt(project));
        for (BuildOutputJar outputJar : eclipsePluginProject.getOutputJars()) {
            for (File sourceFolder : outputJar.getSourceFolders()) {
                project.addCompileSourceRoot(sourceFolder.getAbsolutePath());
            }
        }
    }

    public State getResolverState(MavenProject project) {
        TargetPlatform platform = getTargetPlatform(project);
        return getResolverState(project, platform);
    }

    protected State getResolverState(MavenProject project, TargetPlatform platform) {
        try {
            return resolver.newResolvedState(project, platform);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    public EclipsePluginProjectImpl getEclipsePluginProject(ReactorProject otherProject) {
        EclipsePluginProjectImpl pdeProject = (EclipsePluginProjectImpl) otherProject
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT);
        if (pdeProject == null) {
            try {
                pdeProject = new EclipsePluginProjectImpl(otherProject);
                otherProject.setContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, pdeProject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return pdeProject;
    }

    public List<ClasspathEntry> getClasspath(MavenProject project) {
        @SuppressWarnings("unchecked")
        List<ClasspathEntry> classpath = (List<ClasspathEntry>) project
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH);
        if (classpath == null) {
            throw new IllegalStateException();
        }
        return classpath;
    }

    /**
     * Returns project compile classpath entries.
     */
    private List<File> getThisProjectClasspath(ArtifactDescriptor bundle, ReactorProject project) {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

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

        return new ArrayList<File>(classpath);
    }

    /**
     * Returns bundle classpath entries. If <code>nestedPath</code> is not <code>null</code>,
     * returns single class folder that corresponds specified nestedPath. If <code>nestedPath</code>
     * is <code>null</code>, returns entries specified in Bundle-ClassPath.
     */
    private List<File> getOtherProjectClasspath(ArtifactDescriptor bundle, ReactorProject otherProject,
            String nestedPath) {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

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
        return new ArrayList<File>(classpath);
    }

    private void addExtraClasspathEntries(List<ClasspathEntry> classpath, ReactorProject project,
            TargetPlatform platform) {
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

                    ArtifactDescriptor matchingBundle = platform.getArtifact(
                            org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, bundleId, null);
                    if (matchingBundle != null) {
                        List<File> locations;
                        if (matchingBundle.getMavenProject() != null) {
                            locations = getOtherProjectClasspath(matchingBundle, matchingBundle.getMavenProject(), path);
                        } else if (path != null) {
                            locations = getBundleEntry(matchingBundle, path);
                        } else {
                            locations = getBundleClasspath(matchingBundle);
                        }
                        classpath.add(new DefaultClasspathEntry(matchingBundle.getMavenProject(), matchingBundle
                                .getKey(), locations, null));
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
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

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

        return new ArrayList<File>(classpath);
    }

    private List<File> getBundleEntry(ArtifactDescriptor bundle, String nestedPath) {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

        File entry;
        if (".".equals(nestedPath)) {
            entry = bundle.getLocation();
        } else {
            entry = getNestedJarOrDir(bundle, nestedPath);
        }

        if (entry != null) {
            classpath.add(entry);
        }

        return new ArrayList<File>(classpath);
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
        String filterStr = getManifestValue(Constants.ECLIPSE_PLATFORMFILTER, project);

        if (filterStr != null) {
            try {
                FilterImpl filter = FilterImpl.newInstance(filterStr);

                String ws = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_WS));
                String os = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_OS));
                String arch = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_ARCH));

                // validate if os/ws/arch are not null and actually match the filter
                if (ws != null && os != null && arch != null) {
                    Map<String, String> properties = new HashMap<String, String>();
                    properties.put(PlatformPropertiesUtils.OSGI_WS, ws);
                    properties.put(PlatformPropertiesUtils.OSGI_OS, os);
                    properties.put(PlatformPropertiesUtils.OSGI_ARCH, arch);

                    if (filter.matches(properties)) {
                        return new TargetEnvironment(os, ws, arch, null);
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

    public ExecutionEnvironment getExecutionEnvironment(MavenProject project) {
        String profile = TychoProjectUtils.getTargetPlatformConfiguration(project).getExecutionEnvironment();

        if (profile != null && !profile.startsWith("?")) {
            // hard profile name in pom.xml
            return getExecutionEnvironment(project, profile);
        } else {
            // PDE compatibility (I really feel generous today)
            String pdeProfile = getEclipsePluginProject(DefaultReactorProject.adapt(project)).getBuildProperties()
                    .getProperty("jre.compilation.profile");
            if (pdeProfile != null) {
                return getExecutionEnvironment(project, pdeProfile.trim());
            }
        }

        ExecutionEnvironment buildMinimalEE = null;

        if (profile != null) {
            buildMinimalEE = getExecutionEnvironment(project, profile.substring(1));
        }

        List<ExecutionEnvironment> envs = new ArrayList<ExecutionEnvironment>(Arrays.asList(getManifest(project)
                .getExecutionEnvironments()));
        if (envs.isEmpty()) {
            return buildMinimalEE;
        }

        ExecutionEnvironment manifestMinimalEE = Collections.min(envs);

        if (buildMinimalEE == null) {
            return manifestMinimalEE;
        }

        return manifestMinimalEE.compareTo(buildMinimalEE) < 0 ? buildMinimalEE : manifestMinimalEE;
    }

    protected ExecutionEnvironment getExecutionEnvironment(MavenProject project, String profile) {
        try {
            return ExecutionEnvironmentUtils.getExecutionEnvironment(profile);
        } catch (UnknownEnvironmentException e) {
            throw new RuntimeException("Unknown execution environment specified in build.properties of project "
                    + project, e);
        }
    }
}
