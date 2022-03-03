/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomgenerator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DependencyComputer;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.UpdateSite;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Traverse the current directory to find eclipse-plugin/bundle, feature, update site (site.xml) or
 * p2 repository (category.xml) projects and generate corresponding pom.xml's. This goal is intended
 * to be used by existing projects for generating quick-start pom.xml's when converting their build
 * to Tycho. The generated pom.xml's are only intended as a starting point and will most probably
 * require manual refinement. Note that this goal is not intended for automatic pom.xml generation
 * during build.
 * 
 */
@Mojo(name = "generate-poms", requiresProject = false, threadSafe = true)
public class GeneratePomsMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    private static final class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File fileToTest) {
            return fileToTest.isDirectory() && !METADATA_DIR.equals(fileToTest.getName());
        }
    }

    /** Metadata directory that should be skipped when searching for projects **/
    private static final String METADATA_DIR = ".metadata";

    /** reference to real pom.xml in aggregator poma.xml */
    private static final String THIS_MODULE = ".";

    /**
     * Tycho version to be used in the generated pom.xml files.
     */
    @Parameter(property = "plugin.version", readonly = true)
    private String tychoVersion;

    /**
     * The base directory which will be traversed recursively when searching for projects.
     */
    @Parameter(property = "baseDir", defaultValue = "${basedir}", required = true)
    private File baseDir;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    /**
     * Additional directories to be traversed recursively when searching for projects.
     */
    @Parameter(property = "extraDirs")
    private String extraDirs;

    /**
     * Maven groupId to be used in the generated pom.xml files.
     */
    @Parameter(property = "groupId")
    private String groupId;

    /**
     * Maven version to be used in the generated pom.xml files (applies to parent pom and
     * eclipse-repository/eclipse-update-site only).
     */
    @Parameter(property = "version", defaultValue = "0.0.1-SNAPSHOT")
    private String version;

    /**
     * If true (the default), additional aggregator poma.xml pom file will be generated for update
     * site projects. This poma.xml file can be used to build update site and all its dependencies.
     */
    @Parameter(property = "aggregator", defaultValue = "true")
    private boolean aggregator;

    /**
     * Suffix used to determine test bundles to add to update site aggregator pom.
     */
    @Parameter(property = "testSuffix", defaultValue = ".tests")
    private String testSuffix;

    /**
     * Bundle-SymbolicName of the test suite, a special bundle that knows how to locate and execute
     * all relevant tests.
     */
    @Parameter(property = "testSuite")
    private String testSuite;

    /**
     * URL to p2 repository to add in the aggregator pom.
     */
    @Parameter(property = "repoURL")
    private String repoURL;

    /**
     * ID of the p2 repository to add in the aggregator pom.
     */
    @Parameter(property = "repoID", defaultValue = "injected-repository")
    private String repoID;

    /**
     * Location of directory with template pom.xml file. pom.xml templates will be looked at this
     * directory first, default templates will be used if template directory and the template itself
     * does not exist.
     * 
     * See src/main/resources/templates for the list of supported template files.
     */
    @Parameter(property = "templatesDir", defaultValue = "${basedir}/pom-templates")
    private File templatesDir;

    /**
     * Comma separated list of root project folders. If specified, generated pom.xml files will only
     * include root projects and projects directly and indirectly referenced by the root projects.
     */
    @Parameter(property = "rootProjects")
    private String rootProjects;

    // TODO what is the effect of this parameter?
    @Parameter(defaultValue = "J2SE-1.5")
    private String executionEnvironment;

    @Component(role = BundleReader.class)
    private BundleReader bundleReader;

    @Component(role = EquinoxResolver.class)
    private EquinoxResolver resolver;

    @Component(role = DependencyComputer.class)
    private DependencyComputer dependencyComputer;

    MavenXpp3Reader modelReader = new MavenXpp3Reader();
    MavenXpp3Writer modelWriter = new MavenXpp3Writer();

    private Map<File, Model> updateSites = new LinkedHashMap<>();

    private DefaultDependencyArtifacts platform = new DefaultDependencyArtifacts();

    @Component
    private ToolchainManager toolchainManager;

    @Component
    private Logger logger;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            List<File> baseDirs = getBaseDirs();
            if (getLog().isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("baseDir=").append(toString(baseDir)).append('\n');
                sb.append("extraDirs=").append(extraDirs).append('\n');
                for (int i = 0; i < baseDirs.size(); i++) {
                    sb.append("dir[").append(i).append("]=").append(toString(baseDirs.get(i))).append('\n');
                }
                getLog().debug(sb.toString());
            }

            // find all candidate folders
            List<File> candidateDirs = new ArrayList<>();
            for (File basedir : baseDirs) {
                findAndAddCandidates(candidateDirs, basedir);
            }

            // find all root projects
            List<File> rootProjects = getRootProjects();
            if (getLog().isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("rootProjects=").append(this.rootProjects);
                for (int i = 0; i < rootProjects.size(); i++) {
                    sb.append("rootProject[").append(i).append("]=").append(toString(rootProjects.get(i))).append('\n');
                }
                getLog().debug(sb.toString());
            }

            for (File dir : candidateDirs) {
                if (isPluginProject(dir)) {
                    OsgiManifest mf = bundleReader.loadManifest(dir);
                    platform.addArtifactFile(mf.toArtifactKey(), dir, null);
                }
            }

            // testSuite
            File testSuiteLocation = null;
            if (testSuite != null) {
                ArtifactDescriptor bundle = platform.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, testSuite, null);
                if (bundle != null) {
                    testSuiteLocation = bundle.getLocation(true);
                }
            }

            Set<File> projects = new LinkedHashSet<>();

            // always add baseDir
            projects.add(baseDirs.get(0));

            if (!rootProjects.isEmpty()) {
                if (testSuiteLocation != null) {
                    rootProjects.add(testSuiteLocation);
                }
                for (File rootProject : rootProjects) {
                    getLog().info("Resolving root project " + toString(rootProject));
                    if (isUpdateSiteProject(rootProject)) {
                        projects.addAll(getSiteFeaturesAndPlugins(rootProject));
                        projects.add(rootProject);
                    } else if (isFeatureProject(rootProject)) {
                        projects.addAll(getFeatureFeaturesAndPlugins(rootProject));
                        projects.add(rootProject);
                    } else if (isPluginProject(rootProject)) {
                        addPluginImpl(projects, rootProject); // TODO getPluginAndDependencies
                        projects.add(rootProject);
                    } else {
                        getLog().warn("Unsupported root project " + toString(rootProject));
                    }
                }
            } else {
                projects.addAll(candidateDirs);
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug("Collected " + projects.size() + " projects");
                for (File dir : projects) {
                    getLog().debug("\t" + toString(dir));
                }
            }

            // write poms
            Iterator<File> projectIter = projects.iterator();
            File parentDir = projectIter.next();
            if (!projectIter.hasNext()) {
                if (isProjectDir(parentDir)) {
                    generatePom(null, parentDir);
                } else {
                    throw new MojoExecutionException("Could not find any valid projects");
                }
            } else {
                Model parent = readPomTemplate("parent-pom.xml");
                parent.setGroupId(groupId);
                parent.setArtifactId(parentDir.getName());
                parent.setVersion(version);
                while (projectIter.hasNext()) {
                    File projectDir = projectIter.next();
                    generatePom(parent, projectDir);
                    parent.addModule(getModuleName(parentDir, projectDir));
                }
                reorderModules(parent, parentDir, testSuiteLocation);
                addTychoExtension(parent);
                writePom(parentDir, parent);
                generateAggregatorPoms(testSuiteLocation);
            }
        }
    }

    private void findAndAddCandidates(List<File> candidateDirs, File basedir) {
        getLog().info("Scanning " + toString(basedir) + " basedir");
        if (isProjectDir(basedir)) {
            candidateDirs.add(basedir);
        } else {
            File[] listFiles = basedir.listFiles(new DirectoryFilter());
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (isProjectDir(file)) {
                        candidateDirs.add(file);
                    } else {
                        findAndAddCandidates(candidateDirs, file);
                    }
                }
            }

        }
    }

    private List<File> getRootProjects() {
        return toFileList(rootProjects);
    }

    private boolean isProjectDir(File dir) {
        return isPluginProject(dir) || isFeatureProject(dir) || isUpdateSiteProject(dir)
                || isEclipseRepositoryProject(dir);
    }

    private void reorderModules(Model parent, File basedir, File testSuiteLocation) throws MojoExecutionException {
        List<String> modules = parent.getModules();
        Collections.sort(modules);
        if (testSuiteLocation != null) {
            String moduleName = getModuleName(basedir, testSuiteLocation);
            modules.remove(moduleName);
            modules.add(moduleName);
        }
        if (modules.contains(THIS_MODULE)) {
            modules.remove(THIS_MODULE);
            modules.add(THIS_MODULE);
        }
    }

    private void addTychoExtension(Model model) {
        Build build = model.getBuild();

        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        Plugin tychoPlugin = new Plugin();
        tychoPlugin.setGroupId("org.eclipse.tycho");
        tychoPlugin.setArtifactId("tycho-maven-plugin");
        tychoPlugin.setVersion(tychoVersion);
        tychoPlugin.setExtensions(true);

        build.addPlugin(tychoPlugin);
        if (repoURL != null) {
            Repository repo = new Repository();
            repo.setLayout("p2");
            repo.setUrl(repoURL);
            repo.setId(repoID);
            List<Repository> repositories = new ArrayList<>();
            repositories.add(repo);
            model.setRepositories(repositories);
        }
    }

    private String toString(File file) {
        return file.getAbsolutePath();
    }

    private List<File> getBaseDirs() {
        ArrayList<File> dirs = new ArrayList<>();
        dirs.add(baseDir);
        if (extraDirs != null) {
            dirs.addAll(toFileList(extraDirs));
        }
        return dirs;
    }

    private List<File> toFileList(String str) {
        ArrayList<File> dirs = new ArrayList<>();
        if (str != null) {
            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                File dir = new File(st.nextToken()).getAbsoluteFile();
                if (dir.exists() && dir.isDirectory()) {
                    dirs.add(dir);
                } else {
                    getLog().warn("Not a directory " + dir.getAbsolutePath());
                }
            }
        }
        return dirs;
    }

    private String getModuleName(File basedir, File dir) throws MojoExecutionException {
        File relative = new File(getRelativePath(basedir, dir));
        return relative.getPath().replace('\\', '/');
    }

    private String getRelativePath(File basedir, File dir) {
        // adding extra dependency for a single method is not nice
        // but I don't want to reimplement this tedious logic
        return new FilePath(basedir).makeRelative(new FilePath(dir));
    }

    private void generateAggregatorPoms(File testSuiteLocation) throws MojoExecutionException {
        for (Entry<File, Model> updateSite : updateSites.entrySet()) {
            File basedir = updateSite.getKey();
            Model parent = updateSite.getValue();
            Set<File> modules = getSiteFeaturesAndPlugins(basedir);
            if (aggregator && !modules.isEmpty()) {
                Model modela = readPomTemplate("update-site-poma.xml");
                setParentOrAddTychoExtension(basedir, modela, parent);
                modela.setGroupId(groupId);
                modela.setArtifactId(basedir.getName() + ".aggregator");
                modela.setVersion(version);
                for (File module : modules) {
                    modela.addModule(getModuleName(basedir, module));
                }
                reorderModules(modela, basedir, testSuiteLocation);
                writePom(basedir, "poma.xml", modela);
            }
        }
    }

    private boolean generatePom(Model parent, File basedir) throws MojoExecutionException {
        if (isPluginProject(basedir)) {
            getLog().debug("Found plugin PDE project " + toString(basedir));
            generatePluginPom(parent, basedir);
        } else if (isFeatureProject(basedir)) {
            getLog().debug("Found feature PDE project " + toString(basedir));
            generateFeaturePom(parent, basedir);
        } else if (isEclipseRepositoryProject(basedir)) {
            getLog().debug("Found eclipse-repository PDE project " + toString(basedir));
            generateEclipseRepositoryPom(parent, basedir);
        } else if (isUpdateSiteProject(basedir)) {
            getLog().debug("Found update site PDE project " + toString(basedir));
            getLog().warn("Old style update site found for " + toString(basedir)
                    + ". It is recommended you rename your site.xml to category.xml "
                    + "and use packaging type eclipse-repository instead.");
            generateUpdateSitePom(parent, basedir);
        } else {
            getLog().debug("Not a PDE project " + toString(basedir));
            return false;
        }
        return true;
    }

    private boolean isUpdateSiteProject(File dir) {
        return new File(dir, "site.xml").canRead();
    }

    private boolean isEclipseRepositoryProject(File dir) {
        return new File(dir, "category.xml").canRead();
    }

    private boolean isFeatureProject(File dir) {
        return new File(dir, "feature.xml").canRead();
    }

    private boolean isPluginProject(File dir) {
        return new File(dir, "META-INF/MANIFEST.MF")
                .canRead() /*
                            * || new File(dir, "plugin.xml").canRead()
                            */;
    }

    private void generateUpdateSitePom(Model parent, File basedir) throws MojoExecutionException {
        if (groupId == null) {
            throw new MojoExecutionException(
                    "groupId parameter is required to generate pom.xml for Update Site project " + basedir.getName());
        }
        if (version == null) {
            throw new MojoExecutionException(
                    "version parameter is required to generate pom.xml for Update Site project " + basedir.getName());
        }

        Model model = readPomTemplate("update-site-pom.xml");
        setParentOrAddTychoExtension(basedir, model, parent);
        model.setGroupId(groupId);
        model.setArtifactId(basedir.getName());
        model.setVersion(version);
        writePom(basedir, model);

        updateSites.put(basedir, parent);
    }

    private void generateEclipseRepositoryPom(Model parent, File basedir) throws MojoExecutionException {
        if (groupId == null) {
            throw new MojoExecutionException(
                    "groupId parameter is required to generate pom.xml for Eclipse Repository project "
                            + basedir.getName());
        }
        if (version == null) {
            throw new MojoExecutionException(
                    "version parameter is required to generate pom.xml for Eclipse Repository project "
                            + basedir.getName());
        }

        Model model = readPomTemplate("repository-pom.xml");
        setParentOrAddTychoExtension(basedir, model, parent);
        model.setGroupId(groupId);
        model.setArtifactId(basedir.getName());
        model.setVersion(version);
        writePom(basedir, model);
    }

    private Set<File> getSiteFeaturesAndPlugins(File basedir) throws MojoExecutionException {
        try {
            Set<File> result = new LinkedHashSet<>();

            UpdateSite site;
            File siteFile = new File(basedir, "site.xml");
            if (siteFile.exists()) {
                site = UpdateSite.read(siteFile);
                for (FeatureRef feature : site.getFeatures()) {
                    addFeature(result, feature.getId());
                }
            }
            return result;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not collect update site features and plugins", e);
        }
    }

    private void addFeature(Set<File> result, String name)
            throws IOException, XmlPullParserException, MojoExecutionException {
        if (name != null) {
            File dir = getModuleDir(name);
            if (dir != null && isFeatureProject(dir)) {
                result.add(dir);
                result.addAll(getFeatureFeaturesAndPlugins(dir));
            } else {
                getLog().warn("Unknown feature reference " + name);
            }
        }
    }

    private File getModuleDir(String name) throws MojoExecutionException {
        ArrayList<File> moduleDirs = new ArrayList<>();
        for (File basedir : getBaseDirs()) {
            File dir = new File(basedir, name);
            if (dir.exists() && dir.isDirectory() && isProjectDir(dir)) {
                moduleDirs.add(dir);
            }
        }
        if (moduleDirs.isEmpty()) {
            return null;
        }
        if (moduleDirs.size() > 1) {
            StringBuilder sb = new StringBuilder("Duplicate module definition ").append(name);
            for (File dir : moduleDirs) {
                sb.append("\n\t").append(dir.getAbsoluteFile());
            }
            throw new MojoExecutionException(sb.toString());
        }
        return moduleDirs.get(0);
    }

    private Set<File> getFeatureFeaturesAndPlugins(File basedir) throws MojoExecutionException {
        try {
            Set<File> result = new LinkedHashSet<>();

            Feature feature = Feature.read(new File(basedir, "feature.xml"));

            for (PluginRef plugin : feature.getPlugins()) {
                addPlugin(result, plugin.getId());
            }

            for (FeatureRef includedFeature : feature.getIncludedFeatures()) {
                addFeature(result, includedFeature.getId());
            }

            for (Feature.RequiresRef require : feature.getRequires()) {
                for (Feature.ImportRef imp : require.getImports()) {
                    addPlugin(result, imp.getPlugin());
                    addFeature(result, imp.getFeature());
                }
            }
            return result;

        } catch (IOException | XmlPullParserException e) {
            throw new MojoExecutionException("Exception processing feature " + toString(basedir), e);
        }

    }

    private void addPlugin(Set<File> result, String name) throws MojoExecutionException {
        if (name != null) {
            addPluginImpl(result, name, true);
            addPluginImpl(result, name + testSuffix, false);
        }
    }

    private void addPluginImpl(Set<File> result, String name, boolean required) throws MojoExecutionException {
        if (name != null) {
            File dir = getModuleDir(name);
            if (dir != null && isPluginProject(dir)) {
                addPluginImpl(result, dir);
            } else {
                if (required) {
                    // not really required, but lets warn anyways
                    getLog().warn("Unknown bundle reference " + name);
                }
            }
        }
    }

    private void addPluginImpl(Set<File> result, File basedir) throws MojoExecutionException {
        if (result.add(basedir)) {
            try {
                StandardExecutionEnvironment ee = ExecutionEnvironmentUtils
                        .getExecutionEnvironment(executionEnvironment, toolchainManager, session, logger);
                ModuleContainer state = resolver.newResolvedState(basedir, session, ee, platform);
                ModuleRevision bundle = state.getModule(basedir.getAbsolutePath()).getCurrentRevision();
                if (bundle != null) {
                    for (DependencyComputer.DependencyEntry entry : dependencyComputer.computeDependencies(bundle)) {
                        ModuleRevision supplier = entry.module;
                        File suppliedDir = (File) supplier.getRevisionInfo();
                        if (((supplier.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) && isModuleDir(suppliedDir)) {
                            addPlugin(result, suppliedDir.getName());
                        }
                    }
                } else {
                    getLog().warn("Not an OSGi bundle " + basedir.toString());
                }
            } catch (BundleException e) {
                warnNoBundleDependencies(e);
            }
        }
    }

    private void warnNoBundleDependencies(Exception e) {
        if (getLog().isDebugEnabled()) {
            getLog().warn("Could not determine bundle dependencies", e);
        } else {
            getLog().warn("Could not determine bundle dependencies: " + e.getMessage());
        }
    }

    private boolean isModuleDir(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        for (File basedir : getBaseDirs()) {
            if (isModule(basedir, dir)) {
                return true;
            }
        }
        return false;
    }

    private boolean isModule(File basedir, File dir) {
        try {
            if (basedir.getCanonicalFile().equals(dir.getParentFile().getCanonicalFile())) {
                return true;
            }
        } catch (IOException e) {
            getLog().warn("Totally unexpected IOException", e);
        }
        return false;
    }

    // sets the parent of the model or if it is the "root" project, add the tycho extension
    private void setParentOrAddTychoExtension(File basedir, Model model, Model parentModel) {
        if (parentModel != null) {
            Parent parent = new Parent();
            parent.setGroupId(parentModel.getGroupId());
            parent.setArtifactId(parentModel.getArtifactId());
            parent.setVersion(parentModel.getVersion());
            String relativePath = getRelativePath(basedir, this.baseDir);
            if (!"../".equals(relativePath)) {
                parent.setRelativePath(relativePath);
            }

            model.setParent(parent);
        } else {
            addTychoExtension(model);
        }
    }

    private void generateFeaturePom(Model parent, File basedir) throws MojoExecutionException {
        Model model = readPomTemplate("feature-pom.xml");
        setParentOrAddTychoExtension(basedir, model, parent);

        try {
            try (FileInputStream is = new FileInputStream(new File(basedir, "feature.xml"))) {
                XmlStreamReader reader = new XmlStreamReader(is);
                Xpp3Dom dom = Xpp3DomBuilder.build(reader);

                String groupId = this.groupId;
                if (groupId == null) {
                    groupId = dom.getAttribute("id");
                }
                model.setGroupId(groupId);
                model.setArtifactId(dom.getAttribute("id"));
                model.setVersion(toMavenVersion(dom.getAttribute("version")));

            }
        } catch (XmlPullParserException | IOException e) {
            throw new MojoExecutionException("Can't create pom.xml file", e);
        }

        writePom(basedir, model);
    }

    private void generatePluginPom(Model parent, File basedir) throws MojoExecutionException {
        ArtifactDescriptor bundle = getArtifact(basedir);
        ArtifactKey key = bundle.getKey();
        Model model;
        if ((testSuffix != null && basedir.getName().endsWith(testSuffix))
                || (testSuite != null && key.getId().equals(testSuite))) {
            model = readPomTemplate("test-plugin-pom.xml");
        } else {
            model = readPomTemplate("plugin-pom.xml");
        }
        String groupId = this.groupId;
        if (groupId == null) {
            groupId = key.getId();
        }
        setParentOrAddTychoExtension(basedir, model, parent);
        model.setGroupId(groupId);
        model.setArtifactId(key.getId());
        model.setVersion(toMavenVersion(key.getVersion().toString()));

        writePom(basedir, model);
    }

    protected ArtifactDescriptor getArtifact(File basedir) {
        Map<String, ArtifactDescriptor> classified = platform.getArtifact(basedir);
        return classified != null ? classified.get(null) : null;
    }

    private static String toMavenVersion(String osgiVersion) {
        if (osgiVersion.endsWith(".qualifier")) {
            return osgiVersion.substring(0, osgiVersion.length() - ".qualifier".length()) + "-"
                    + Artifact.SNAPSHOT_VERSION;
        } else {
            return osgiVersion;
        }
    }

    private void writePom(File dir, Model model) throws MojoExecutionException {
        writePom(dir, "pom.xml", model);
    }

    private void writePom(File dir, String filename, Model model) throws MojoExecutionException {
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, filename)),
                    StandardCharsets.UTF_8)) {
                modelWriter.write(writer, model);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write pom.xml", e);
        }
    }

    private Model readPomTemplate(String name) throws MojoExecutionException {
        try {
            XmlStreamReader reader;

            File file = new File(templatesDir, name);
            if (file.canRead()) {
                // check custom templates dir first
                reader = ReaderFactory.newXmlReader(file);
            } else {
                // fall back to internal templates 
                ClassLoader cl = GeneratePomsMojo.class.getClassLoader();
                InputStream is = cl.getResourceAsStream("templates/" + name);
                reader = is != null ? ReaderFactory.newXmlReader(is) : null;
            }
            if (reader != null) {
                try {
                    return modelReader.read(reader);
                } finally {
                    reader.close();
                }
            } else {
                throw new MojoExecutionException("pom.xml template cannot be found " + name);
            }
        } catch (XmlPullParserException | IOException e) {
            throw new MojoExecutionException("Can't read pom.xml template " + name, e);
        }
    }

}
