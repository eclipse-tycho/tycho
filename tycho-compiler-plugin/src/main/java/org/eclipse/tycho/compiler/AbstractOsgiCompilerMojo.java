/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.tycho.compiler;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.classpath.JavaCompilerConfiguration;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.utils.MavenArtifactRef;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.runtime.Adaptable;
import org.osgi.framework.Constants;

import copied.org.apache.maven.plugin.AbstractCompilerMojo;
import copied.org.apache.maven.plugin.CompilationFailureException;

public abstract class AbstractOsgiCompilerMojo extends AbstractCompilerMojo implements JavaCompilerConfiguration,
        Adaptable {

    private static enum JDKUsage {
        SYSTEM, BREE;
    }

    public static final String RULE_SEPARATOR = File.pathSeparator;

    /**
     * Exclude all but keep looking for other another match
     */
    public static final String RULE_EXCLUDE_ALL = "?**/*";

    private static final Set<String> MATCH_ALL = Collections.singleton("**/*");

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Transitively add specified maven artifacts to compile classpath in addition to elements
     * calculated according to OSGi rules. All packages from additional entries will be accessible
     * at compile time.
     * 
     * Useful when OSGi runtime classpath contains elements not defined using normal dependency
     * mechanisms. For example, when Eclipse Equinox is started from application server with
     * -Dosgi.parentClassloader=fwk parameter.
     * 
     * DO NOT USE. This is a stopgap solution to allow refactoring of tycho-p2 code to a separate
     * set of components.
     * 
     * @parameter
     */
    private MavenArtifactRef[] extraClasspathElements;

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /** @component */
    private RepositorySystem repositorySystem;

    /**
     * Which JDK to use for compilation. Default value is SYSTEM which means the currently running
     * JDK. If BREE is specified, MANIFEST header <code>Bundle-RequiredExecutionEnvironment</code>
     * is used to define the JDK to compile against. In this case, you need to provide a <a
     * href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchains.xml</a>
     * configuration file. The value of BREE will be matched against the id of the toolchain
     * elements in toolchains.xml. Example:
     * 
     * <pre>
     * &lt;toolchains&gt;
     *   &lt;toolchain&gt;
     *      &lt;type&gt;jdk&lt;/type&gt;
     *      &lt;provides&gt;
     *          &lt;id&gt;J2SE-1.5&lt;/id&gt;
     *      &lt;/provides&gt;
     *      &lt;configuration&gt;
     *         &lt;jdkHome&gt;/path/to/jdk/1.5&lt;/jdkHome&gt;
     *      &lt;/configuration&gt;
     *   &lt;/toolchain&gt;
     * &lt;/toolchains&gt;
     * </pre>
     * 
     * The default value of the bootclasspath used for compilation is
     * <tt>&lt;jdkHome&gt;/lib/*;&lt;jdkHome&gt;/lib/ext/*;&lt;jdkHome&gt;/lib/endorsed/*</tt> .
     * 
     * For JDKs with different filesystem layouts, the bootclasspath can be specified explicitly in
     * the configuration section.
     * 
     * Example:
     * 
     * <pre>
     * &lt;configuration&gt;
     *   &lt;jdkHome&gt;/path/to/jdk/1.5&lt;/jdkHome&gt;
     *   &lt;bootClassPath&gt;
     *     &lt;includes&gt;
     *       &lt;include&gt;jre/lib/amd64/default/jclSC160/*.jar&lt;/include&gt;
     *     &lt;/includes&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;&#42;&#42;/alt-*.jar&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;/bootClassPath&gt;
     * &lt;/configuration&gt;
     * </pre>
     * 
     * 
     * 
     * @parameter default-value="SYSTEM"
     */
    private JDKUsage useJDK;

    /** @component */
    private ToolchainManagerPrivate toolChainManager;

    /**
     * A list of inclusion filters for the compiler.
     * 
     * @parameter
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.
     * 
     * @parameter
     */
    private Set<String> excludes = new HashSet<String>();

    /**
     * A list of exclusion filters for non-java resource files which should not be copied to the
     * output directory.
     * 
     * @parameter
     */
    private Set<String> excludeResources = new HashSet<String>();

    /**
     * Whether a bundle is required to explicitly import non-java.* packages from the JDK. This is
     * the design-time equivalent to the equinox runtime option <a
     * href="http://wiki.eclipse.org/Equinox_Boot_Delegation#The_solution"
     * >osgi.compatibility.bootdelegation</a>.
     * 
     * @parameter default-value="false"
     */
    private boolean requireJREPackageImports;

    /**
     * If set to <code>false</code> (the default) issue a warning if effective compiler target level
     * is incompatible with bundle minimal execution environment. If set to <code>true</code> will
     * fail the build if effective compiler target and minimal BREE are incompatible.
     * 
     * @parameter default-value="false"
     */
    private boolean strictCompilerTarget;

    /**
     * @parameter
     * @since 0.16.0
     */
    private String extraBootclasspath;

    /**
     * Current build output jar
     */
    private BuildOutputJar outputJar;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    public void execute() throws MojoExecutionException, CompilationFailureException {
        ExecutionEnvironment minimalBREE = getBundleProject().getManifestMinimalEE(project);
        ExecutionEnvironment bree = getTargetExecutionEnvironment();
        getLog().debug("Manifest minimal BREE: " + (minimalBREE != null ? minimalBREE.toString() : "<null>"));
        getLog().debug("Effective BREE: " + (bree != null ? bree.toString() : "<null>"));
        String effectiveTargetLevel = getTargetLevel();
        getLog().debug("Effective source/target: " + getSourceLevel() + "/" + effectiveTargetLevel);

        if (minimalBREE != null && !minimalBREE.isCompatibleCompilerTargetLevel(effectiveTargetLevel)) {
            String message = "Effective compiler target " + effectiveTargetLevel + " is incompatible with "
                    + minimalBREE + " @ " + project;
            if (strictCompilerTarget) {
                throw new MojoExecutionException(message);
            }
            getLog().warn(message);
        }

        for (BuildOutputJar jar : getEclipsePluginProject().getOutputJars()) {
            this.outputJar = jar;
            this.outputJar.getOutputDirectory().mkdirs();
            super.execute();
            copyResources();
        }

        // this does not include classes from nested jars
        BuildOutputJar dotOutputJar = getEclipsePluginProject().getDotOutputJar();
        if (dotOutputJar != null) {
            project.getArtifact().setFile(dotOutputJar.getOutputDirectory());
        }
    }

    /*
     * mimics the behavior of the PDE incremental builder which by default copies all (non-java)
     * resource files in source directories into the target folder
     */
    private void copyResources() throws MojoExecutionException {
        for (String sourceRoot : getCompileSourceRoots()) {
            // StaleSourceScanner.getIncludedSources throws IllegalStateException
            // if directory doesnt't exist
            File sourceRootFile = new File(sourceRoot);
            if (!sourceRootFile.isDirectory()) {
                getLog().warn("Source directory " + sourceRoot + " does not exist");
                continue;
            }

            Set<String> excludes = new HashSet<String>();
            excludes.addAll(excludeResources);
            excludes.addAll(getEclipsePluginProject().getBuildProperties().getBinExcludes());
            excludes.add("**/*.java");
            StaleSourceScanner scanner = new StaleSourceScanner(0L, MATCH_ALL, excludes);
            CopyMapping copyMapping = new CopyMapping();
            scanner.addSourceMapping(copyMapping);
            try {
                scanner.getIncludedSources(sourceRootFile, this.outputJar.getOutputDirectory());
                for (CopyMapping.SourceTargetPair sourceTargetPair : copyMapping.getSourceTargetPairs()) {
                    FileUtils.copyFile(new File(sourceRoot, sourceTargetPair.source), sourceTargetPair.target);
                }
            } catch (InclusionScanException e) {
                throw new MojoExecutionException("Exception while scanning for resource files in " + sourceRoot, e);
            } catch (IOException e) {
                throw new MojoExecutionException("Exception copying resource files from " + sourceRoot + " to "
                        + this.outputJar.getOutputDirectory(), e);
            }
        }
    }

    /** public for testing purposes */
    public EclipsePluginProject getEclipsePluginProject() throws MojoExecutionException {
        return ((OsgiBundleProject) getBundleProject()).getEclipsePluginProject(DefaultReactorProject.adapt(project));
    }

    @Override
    protected File getOutputDirectory() {
        return outputJar.getOutputDirectory();
    }

    public List<String> getClasspathElements() throws MojoExecutionException {
        final List<String> classpath = new ArrayList<String>();
        for (ClasspathEntry cpe : getClasspath()) {
            for (File location : cpe.getLocations()) {
                classpath.add(location.getAbsolutePath() + toString(cpe.getAccessRules()));
            }
        }
        return classpath;
    }

    private BundleProject getBundleProject() throws MojoExecutionException {
        TychoProject projectType = projectTypes.get(project.getPackaging());
        if (!(projectType instanceof BundleProject)) {
            throw new MojoExecutionException("Not a bundle project " + project.toString());
        }
        return (BundleProject) projectType;
    }

    private String toString(List<AccessRule> rules) {
        StringBuilder result = new StringBuilder(); // include all
        if (rules != null) {
            result.append("[");
            for (AccessRule rule : rules) {
                if (result.length() > 1)
                    result.append(RULE_SEPARATOR);
                result.append(rule.isDiscouraged() ? "~" : "+");
                result.append(rule.getPattern());
            }
            if (result.length() > 1)
                result.append(RULE_SEPARATOR);
            result.append(RULE_EXCLUDE_ALL);
            result.append("]");
        } else {
            // include everything, not strictly necessary, but lets make this obvious
            //result.append("[+**/*]");
        }
        return result.toString();
    }

    protected final List<String> getCompileSourceRoots() throws MojoExecutionException {
        ArrayList<String> roots = new ArrayList<String>();
        for (File folder : outputJar.getSourceFolders()) {
            try {
                roots.add(folder.getCanonicalPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Unexpected IOException", e);
            }
        }
        return roots;
    }

    public List<SourcepathEntry> getSourcepath() throws MojoExecutionException {
        ArrayList<SourcepathEntry> entries = new ArrayList<SourcepathEntry>();
        for (BuildOutputJar jar : getEclipsePluginProject().getOutputJars()) {
            final File outputDirectory = jar.getOutputDirectory();
            for (final File sourcesRoot : jar.getSourceFolders()) {
                SourcepathEntry entry = new SourcepathEntry() {
                    public File getSourcesRoot() {
                        return sourcesRoot;
                    }

                    public File getOutputDirectory() {
                        return outputDirectory;
                    }

                    public List<String> getIncludes() {
                        return null;
                    }

                    public List<String> getExcludes() {
                        return null;
                    }
                };
                entries.add(entry);
            }
        }
        return entries;
    }

    protected SourceInclusionScanner getSourceInclusionScanner(int staleMillis) {
        SourceInclusionScanner scanner = null;

        if (includes.isEmpty() && excludes.isEmpty()) {
            scanner = new StaleSourceScanner(staleMillis);
        } else {
            if (includes.isEmpty()) {
                includes.add("**/*.java");
            }
            scanner = new StaleSourceScanner(staleMillis, includes, excludes);
        }
        return scanner;
    }

    protected SourceInclusionScanner getSourceInclusionScanner(String inputFileEnding) {
        SourceInclusionScanner scanner = null;

        if (includes.isEmpty() && excludes.isEmpty()) {
            includes = Collections.singleton("**/*." + inputFileEnding);
            scanner = new SimpleSourceInclusionScanner(includes, Collections.EMPTY_SET);
        } else {
            if (includes.isEmpty()) {
                includes.add("**/*." + inputFileEnding);
            }
            scanner = new SimpleSourceInclusionScanner(includes, excludes);
        }

        return scanner;
    }

    @Override
    protected CompilerConfiguration getCompilerConfiguration(List<String> compileSourceRoots)
            throws MojoExecutionException {
        CompilerConfiguration compilerConfiguration = super.getCompilerConfiguration(compileSourceRoots);
        String encoding = getEclipsePluginProject().getBuildProperties().getJarToJavacDefaultEncodingMap()
                .get(outputJar.getName());
        if (encoding != null) {
            compilerConfiguration.setSourceEncoding(encoding);
        }
        configureSourceAndTargetLevel(compilerConfiguration);
        configureJavaHome(compilerConfiguration);
        configureBootclasspathAccessRules(compilerConfiguration);
        return compilerConfiguration;
    }

    private void configureBootclasspathAccessRules(CompilerConfiguration compilerConfiguration)
            throws MojoExecutionException {
        List<AccessRule> accessRules = new ArrayList<ClasspathEntry.AccessRule>();

        if (requireJREPackageImports) {
            accessRules.add(new DefaultAccessRule("java/**", false));
            accessRules.addAll(getStrictSystemBundleAccessRules());
        } else {
            ExecutionEnvironment environment = getTargetExecutionEnvironment();
            if (environment != null) {
                accessRules.add(new DefaultAccessRule("java/**", false));
                for (String pkg : environment.getSystemPackages()) {
                    accessRules.add(new DefaultAccessRule(pkg.trim().replace('.', '/') + "/*", false));
                }
                // now add packages exported by framework extension bundles 
                accessRules.addAll(((BundleProject) getBundleProject()).getBootClasspathExtraAccessRules(project));
            }
        }
        if (accessRules.size() > 0) {
            compilerConfiguration
                    .addCompilerCustomArgument("org.osgi.framework.system.packages", toString(accessRules));
        }
    }

    private List<AccessRule> getStrictSystemBundleAccessRules() throws MojoExecutionException {
        for (ClasspathEntry entry : getClasspath()) {
            String id = entry.getArtifactKey().getId();
            if (EquinoxResolver.SYSTEM_BUNDLE_SYMBOLIC_NAME.equals(id)) {
                return entry.getAccessRules();
            }
        }
        return emptyList();
    }

    private void configureJavaHome(CompilerConfiguration compilerConfiguration) throws MojoExecutionException {
        if (useJDK != JDKUsage.BREE) {
            return;
        }
        ExecutionEnvironment environment = getTargetExecutionEnvironment();
        if (environment == null) {
            getLog().warn(
                    "useJDK = BREE configured, but bundle has no " + Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT
                            + " header. Compiling with current JDK.");
        } else {
            DefaultJavaToolChain toolChain = findMatchingJavaToolChain(environment);
            compilerConfiguration.addCompilerCustomArgument("use.java.home", toolChain.getJavaHome());
            configureBootClassPath(compilerConfiguration, toolChain);
        }
    }

    private void configureBootClassPath(CompilerConfiguration compilerConfiguration, DefaultJavaToolChain javaToolChain) {
        Xpp3Dom config = (Xpp3Dom) javaToolChain.getModel().getConfiguration();
        if (config != null) {
            Xpp3Dom bootClassPath = config.getChild("bootClassPath");
            if (bootClassPath != null) {
                Xpp3Dom includeParent = bootClassPath.getChild("includes");
                if (includeParent != null) {
                    Xpp3Dom[] includes = includeParent.getChildren("include");
                    if (includes.length > 0) {
                        compilerConfiguration.addCompilerCustomArgument(
                                "-bootclasspath",
                                scanBootclasspath(javaToolChain.getJavaHome(), includes,
                                        bootClassPath.getChild("excludes")));
                    }
                }
            }
        }

        if (!StringUtils.isEmpty(extraBootclasspath)) {
            compilerConfiguration.addCompilerCustomArgument("-bootclasspath/a", extraBootclasspath);
        }
    }

    private String scanBootclasspath(String javaHome, Xpp3Dom[] includes, Xpp3Dom excludeParent) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(javaHome);
        scanner.setIncludes(getValues(includes));
        if (excludeParent != null) {
            Xpp3Dom[] excludes = excludeParent.getChildren("exclude");
            if (excludes.length > 0) {
                scanner.setExcludes(getValues(excludes));
            }
        }
        scanner.scan();
        StringBuilder bootClassPath = new StringBuilder();
        String[] includedFiles = scanner.getIncludedFiles();
        for (int i = 0; i < includedFiles.length; i++) {
            if (i > 0) {
                bootClassPath.append(File.pathSeparator);
            }
            bootClassPath.append(new File(javaHome, includedFiles[i]).getAbsolutePath());
        }
        return bootClassPath.toString();
    }

    private static String[] getValues(Xpp3Dom[] doms) {
        String[] values = new String[doms.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = doms[i].getValue();
        }
        return values;
    }

    private DefaultJavaToolChain findMatchingJavaToolChain(final ExecutionEnvironment environment)
            throws MojoExecutionException {
        try {
            final Map requirements = Collections.singletonMap("id", environment.getProfileName());
            for (ToolchainPrivate javaToolChain : toolChainManager.getToolchainsForType("jdk", session)) {
                if (javaToolChain.matchesRequirements(requirements)) {
                    if (javaToolChain instanceof DefaultJavaToolChain) {
                        return ((DefaultJavaToolChain) javaToolChain);
                    }
                }
            }
            throw new MojoExecutionException("useJDK = BREE configured, but no toolchain of type 'jdk' with id '"
                    + environment.getProfileName()
                    + "' found. See http://maven.apache.org/guides/mini/guide-using-toolchains.html");
        } catch (MisconfiguredToolchainException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void configureSourceAndTargetLevel(CompilerConfiguration compilerConfiguration)
            throws MojoExecutionException {
        ExecutionEnvironment ee = getTargetExecutionEnvironment();
        compilerConfiguration.setSourceVersion(getSourceLevel(ee));
        compilerConfiguration.setTargetVersion(getTargetLevel(ee));
    }

    private ExecutionEnvironment getTargetExecutionEnvironment() throws MojoExecutionException {
        return TychoProjectUtils.getExecutionEnvironmentConfiguration(project).getFullSpecification();
    }

    public List<ClasspathEntry> getClasspath() throws MojoExecutionException {
        TychoProject projectType = getBundleProject();
        ArrayList<ClasspathEntry> classpath = new ArrayList<ClasspathEntry>(
                ((BundleProject) projectType).getClasspath(project));

        if (extraClasspathElements != null) {
            ArtifactRepository localRepository = session.getLocalRepository();
            List<ArtifactRepository> remoteRepositories = project.getRemoteArtifactRepositories();
            for (MavenArtifactRef a : extraClasspathElements) {
                Artifact artifact = repositorySystem.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(),
                        "jar");

                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setLocalRepository(localRepository);
                request.setRemoteRepositories(remoteRepositories);
                request.setResolveRoot(true);
                request.setResolveTransitively(true);
                ArtifactResolutionResult result = repositorySystem.resolve(request);

                if (result.hasExceptions()) {
                    throw new MojoExecutionException("Could not resolve extra classpath entry", result.getExceptions()
                            .get(0));
                }

                for (Artifact b : result.getArtifacts()) {
                    MavenProject bProject = null;
                    if (b instanceof ProjectArtifact) {
                        bProject = ((ProjectArtifact) b).getProject();
                    }
                    ArrayList<File> bLocations = new ArrayList<File>();
                    bLocations.add(b.getFile()); // TODO properly handle multiple project locations maybe
                    classpath.add(new DefaultClasspathEntry(DefaultReactorProject.adapt(bProject), null, bLocations,
                            null));
                }
            }
        }
        return classpath;
    }

    public String getExecutionEnvironment() throws MojoExecutionException {
        ExecutionEnvironment env = getTargetExecutionEnvironment();
        return env != null ? env.getProfileName() : null;
    }

    public String getSourceLevel() throws MojoExecutionException {
        return getSourceLevel(getTargetExecutionEnvironment());
    }

    private String getSourceLevel(ExecutionEnvironment env) throws MojoExecutionException {
        // first, explicit pom configuration 
        if (source != null) {
            return source;
        }
        // then, build.properties
        String javacSource = getEclipsePluginProject().getBuildProperties().getJavacSource();
        if (javacSource != null) {
            return javacSource;
        }
        // then, BREE
        if (env != null) {
            String compilerSourceLevel = env.getCompilerSourceLevel();
            // TODO 387796 never null?
            if (compilerSourceLevel != null) {
                return compilerSourceLevel;
            }
        }
        return DEFAULT_SOURCE_VERSION;
    }

    public String getTargetLevel() throws MojoExecutionException {
        return getTargetLevel(getTargetExecutionEnvironment());
    }

    public String getTargetLevel(ExecutionEnvironment env) throws MojoExecutionException {
        // first, explicit pom configuration
        if (target != null) {
            return target;
        }
        // then, build.properties
        String javacTarget = getEclipsePluginProject().getBuildProperties().getJavacTarget();
        if (javacTarget != null) {
            return javacTarget;
        }
        // then, BREE
        // TODO 387796 never null?
        if (env != null) {
            String eeTarget = env.getCompilerTargetLevel();
            if (eeTarget != null) {
                return eeTarget;
            }
        }
        return DEFAULT_TARGET_VERSION;
    }

    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(JavaCompilerConfiguration.class)) {
            return adapter.cast(this);
        }
        return null;
    }
}
