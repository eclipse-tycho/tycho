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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.classpath.JavaCompilerConfiguration;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.MavenArtifactRef;
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
     */
    private MavenProject project;

    /**
     * If set to true, compiler will use source folders defined in build.properties file and will
     * ignore ${project.compileSourceRoots}/${project.testCompileSourceRoots}.
     * 
     * Compilation will fail with an error, if this parameter is set to true but the project does
     * not have valid build.properties file.
     * 
     * @parameter default-value="true"
     */
    private boolean usePdeSourceRoots;

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

    /** @parameter expression="${session}" */
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
     * Current build output jar
     */
    private BuildOutputJar outputJar;

    private final Set<File> outputFolders = new LinkedHashSet<File>();

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    public void execute() throws MojoExecutionException, CompilationFailureException {
        if (usePdeSourceRoots) {
            getLog().info("Using compile source roots from build.properties");
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
        return usePdeSourceRoots ? getPdeCompileSourceRoots() : getConfiguredCompileSourceRoots();
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

    protected abstract List<String> getConfiguredCompileSourceRoots();

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

    protected List<String> getPdeCompileSourceRoots() throws MojoExecutionException {
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

    @Override
    protected CompilerConfiguration getCompilerConfiguration(List<String> compileSourceRoots)
            throws MojoExecutionException {
        CompilerConfiguration compilerConfiguration = super.getCompilerConfiguration(compileSourceRoots);
        if (usePdeSourceRoots) {
            Properties props = getEclipsePluginProject().getBuildProperties();
            String encoding = props.getProperty("javacDefaultEncoding." + outputJar.getName());
            if (encoding != null) {
                compilerConfiguration.setSourceEncoding(encoding);
            }
        }
        configureSourceAndTargetLevel(compilerConfiguration);
        configureJavaHome(compilerConfiguration);
        configureBootclasspathAccessRules(compilerConfiguration);
        return compilerConfiguration;
    }

    private void configureBootclasspathAccessRules(CompilerConfiguration compilerConfiguration)
            throws MojoExecutionException {
        ExecutionEnvironment environment = getTargetExecutionEnvironment();
        if (environment != null) {
            List<AccessRule> accessRules = new ArrayList<ClasspathEntry.AccessRule>();

            accessRules.add(new DefaultAccessRule("java/**", false));

            for (String pkg : environment.getSystemPackages()) {
                accessRules.add(new DefaultAccessRule(pkg.trim().replace('.', '/') + "/*", false));
            }

            compilerConfiguration
                    .addCompilerCustomArgument("org.osgi.framework.system.packages", toString(accessRules));
        }
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
            String javaHome = findMatchingJavaToolChain(environment).getJavaHome();
            compilerConfiguration.addCompilerCustomArgument("use.java.home", javaHome);
        }
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
        return getBundleProject().getExecutionEnvironment(project);
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
        return getTargetExecutionEnvironment().getProfileName();
    }

    public String getSourceLevel() throws MojoExecutionException {
        return getSourceLevel(getTargetExecutionEnvironment());
    }

    private String getSourceLevel(ExecutionEnvironment env) {
        if (source != null) {
            // explicit pom configuration wins
            return source;
        }
        if (env != null) {
            return env.getCompilerSourceLevel();
        }
        return DEFAULT_SOURCE_VERSION;
    }

    public String getTargetLevel() throws MojoExecutionException {
        return getTargetLevel(getTargetExecutionEnvironment());
    }

    public String getTargetLevel(ExecutionEnvironment env) {
        if (target != null) {
            // explicit pom configuration wins
            return target;
        }
        if (env != null) {
            return env.getCompilerTargetLevel();
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
