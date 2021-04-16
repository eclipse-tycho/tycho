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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jdt.internal.compiler.util.CtSym;
import org.eclipse.jdt.internal.compiler.util.JRTUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.classpath.ClasspathEntry.AccessRule;
import org.eclipse.tycho.classpath.JavaCompilerConfiguration;
import org.eclipse.tycho.classpath.SourcepathEntry;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.maven.ToolchainProvider.JDKUsage;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry.DefaultAccessRule;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.runtime.Adaptable;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Namespace;

import copied.org.apache.maven.plugin.AbstractCompilerMojo;

public abstract class AbstractOsgiCompilerMojo extends AbstractCompilerMojo
        implements JavaCompilerConfiguration, Adaptable {

    public static final String RULE_SEPARATOR = File.pathSeparator;

    /**
     * Exclude all but keep looking for other another match
     */
    public static final String RULE_EXCLUDE_ALL = "?**/*";

    private static final Set<String> MATCH_ALL = Collections.singleton("**/*");

    private static final String PREFS_FILE_PATH = ".settings" + File.separator + "org.eclipse.jdt.core.prefs";

    @Parameter(property = "project", readonly = true)
    protected MavenProject project;

    /**
     * Transitively add specified maven artifacts to compile classpath in addition to elements
     * calculated according to OSGi rules. All packages from additional entries will be accessible
     * at compile time.
     * 
     * Useful when OSGi runtime classpath contains elements not defined using normal dependency
     * mechanisms. For example, when Eclipse Equinox is started from application server with
     * -Dosgi.parentClassloader=fwk parameter.
     */
    @Parameter
    private Dependency[] extraClasspathElements;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Component
    private RepositorySystem repositorySystem;

    /**
     * Which JDK to use for compilation. Default value is SYSTEM which means the currently running
     * JDK. If BREE is specified, MANIFEST header <code>Bundle-RequiredExecutionEnvironment</code>
     * is used to define the JDK to compile against. In this case, you need to provide a
     * <a href="http://maven.apache.org/guides/mini/guide-using-toolchains.html">toolchains.xml</a>
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
     */
    @Parameter(defaultValue = "SYSTEM")
    private JDKUsage useJDK;

    @Component
    private ToolchainManagerPrivate toolChainManager;

    /**
     * A list of inclusion filters for the compiler.
     */
    @Parameter
    private Set<String> includes = new HashSet<>();

    /**
     * A list of exclusion filters for the compiler.
     */
    @Parameter
    private Set<String> excludes = new HashSet<>();

    /**
     * A list of exclusion filters for non-java resource files which should not be copied to the
     * output directory.
     */
    @Parameter
    private Set<String> excludeResources = new HashSet<>();

    /**
     * Whether a bundle is required to explicitly import non-java.* packages from the JDK. This is
     * the design-time equivalent to the equinox runtime option
     * <a href="https://wiki.eclipse.org/Equinox_Boot_Delegation#The_solution"
     * >osgi.compatibility.bootdelegation</a>.
     */
    @Parameter(defaultValue = "false")
    private boolean requireJREPackageImports;

    /**
     * If set to <code>false</code> (the default) issue a warning if effective compiler target level
     * is incompatible with bundle minimal execution environment. If set to <code>true</code> will
     * fail the build if effective compiler target and minimal BREE are incompatible.
     */
    @Parameter(defaultValue = "false")
    private boolean strictCompilerTarget;

    /**
     * If set to <code>true</code>, the settings file
     * ${project.basedir}/.settings/org.eclipse.jdt.core.prefs will be passed to the compiler. If
     * the file is not present, the build will not fail.
     */
    @Parameter(defaultValue = "true")
    private boolean useProjectSettings;

    /**
     * Whether the <code>-release</code> argument for the Java compiler should be derived from the
     * target level. Enabled by default.
     * <p>
     * Disabling this can be useful in situations where compiling using <code>-release</code> cannot
     * be used, e.g. when referencing internal JDK classes exported via an OSGI framework extension.
     * In that case <code>&lt;release&gt;</code> should also be explicitly set to an empty value to
     * prevent it from being inherited.
     */
    @Parameter(defaultValue = "true")
    private boolean deriveReleaseCompilerArgumentFromTargetLevel = true;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Component
    private BundleReader bundleReader;

    /**
     * Whether all resources in the source folders should be copied to
     * ${project.build.outputDirectory}.
     * 
     * <code>true</code> (default) means that all resources are copied from the source folders to
     * <code>${project.build.outputDirectory}</code>.
     * 
     * <code>false</code> means that no resources are copied from the source folders to
     * <code>${project.build.outputDirectory}</code>.
     * 
     * Set this to <code>false</code> in case you want to keep resources separate from java files in
     * <code>src/main/resources</code> and handle them using
     * <a href="http://maven.apache.org/plugins/maven-resources-plugin/"> maven-resources-plugin</a>
     * (e.g. for <a href=
     * "http://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html">resource
     * filtering<a/>.
     * 
     */
    @Parameter(defaultValue = "true")
    private boolean copyResources;

    /**
     * The directory where the compiler log files should be placed. For each output jar a log file
     * will be created and stored in this directory. Logging into files is only enabled if
     * {@link #log} is specified. Default: <code>${project.build.directory}/compile-logs</code>
     */
    @Parameter(defaultValue = "${project.build.directory}/compile-logs")
    private File logDirectory;

    /**
     * The format of the compiler log file. <code>plain</code> will log into a plain text file
     * (.log), <code>xml</code> will log in xml format (.xml). If omitted, no logging into files is
     * done. The log file name is derived from the jar file name:
     * 
     * <pre>
     * Example:
     * build.properties:
     * 
     * output.lib1/library.jar = lib1bin/ 
     * output.lib2/library.jar = lib2bin/ 
     * output.. = bin/
     * 
     * And a configuration:
     * 
     * &lt;configuration&gt;
     *   &lt;logEnabled&gt;true&lt;/logEnabled&gt;
     *   &lt;logDirectory&gt;${project.build.directory}/logfiles&lt;/logDirectory&gt;
     *   &lt;log&gt;xml&lt;/log&gt; 
     * &lt;/configuration&gt;
     * 
     * Will produce the following log files
     * 
     * ${project.build.directory}/logfiles/@dot.xml
     * ${project.build.directory}/logfiles/lib1_library.jar.xml
     * ${project.build.directory}/logfiles/lib2_library.jar.xml
     * </pre>
     */
    @Parameter
    private String log;

    @Component
    ToolchainProvider toolchainProvider;

    @Component
    private ToolchainManager toolchainManager;

    @Component
    private Logger logger;

    private StandardExecutionEnvironment[] manifestBREEs;

    private File currentOutputDirectory;

    private List<String> currentSourceRoots;

    private List<String> currentExcludes;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Manifest BREEs: " + Arrays.toString(getBREE()));
        getLog().debug("Target Platform EE: " + getTargetExecutionEnvironment());
        String effectiveTargetLevel = getTargetLevel();
        getLog().debug("Effective source/target: " + getSourceLevel() + "/" + effectiveTargetLevel);

        checkTargetLevelCompatibleWithManifestBREEs(effectiveTargetLevel, manifestBREEs);

        doCompile();
        doFinish();
    }

    private void doCompile() throws MojoExecutionException, MojoFailureException {
        List<SourcepathEntry> sourcepath = getSourcepath();
        if (sourcepath.isEmpty()) {
            return;
        }
        Map<File, List<SourcepathEntry>> outputMap = sourcepath.stream().collect(
                Collectors.groupingBy(SourcepathEntry::getOutputDirectory, LinkedHashMap::new, Collectors.toList()));

        for (Entry<File, List<SourcepathEntry>> entry : outputMap.entrySet()) {
            this.currentOutputDirectory = entry.getKey();
            this.currentOutputDirectory.mkdirs();
            this.currentSourceRoots = entry.getValue().stream().map(SourcepathEntry::getSourcesRoot)
                    .map(root -> new File(root.toURI().normalize()).toString()).collect(Collectors.toList());
            this.currentExcludes = entry.getValue().stream().map(SourcepathEntry::getExcludes).filter(Objects::nonNull)
                    .flatMap(Collection::stream).distinct().collect(Collectors.toList());
            super.execute();
        }
        doCopyResources();
    }

    /**
     * Subclasses might override this method to perform final tasks and as a last opportunity to
     * fail the compile
     * 
     * @throws MojoExecutionException
     */
    protected void doFinish() throws MojoExecutionException {
        //empty
    }

    /**
     * Only public for tests purpose!
     */
    public StandardExecutionEnvironment[] getBREE() {
        if (manifestBREEs == null) {
            OsgiManifest manifest = bundleReader.loadManifest(project.getBasedir());
            manifestBREEs = Arrays.stream(manifest.getExecutionEnvironments())
                    .map(ee -> ExecutionEnvironmentUtils.getExecutionEnvironment(ee, toolchainManager, session, logger))
                    .toArray(StandardExecutionEnvironment[]::new);
            if (manifestBREEs.length == 0) {
                ManifestElement[] requireCapability = manifest.getManifestElements(Constants.REQUIRE_CAPABILITY);
                if (requireCapability != null) {
                    List<Filter> eeFilters = Arrays.stream(requireCapability)
                            .filter(element -> ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
                                    .equals(element.getValue())) //
                            .map(element -> element.getDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE)) //
                            .map(filterDirective -> {
                                try {
                                    return FrameworkUtil.createFilter(filterDirective);
                                } catch (InvalidSyntaxException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }).filter(Objects::nonNull).collect(Collectors.toList());
                    manifestBREEs = ExecutionEnvironmentUtils.getProfileNames().stream() //
                            .map(name -> name.split("-")) //
                            .map(segments -> Map.of(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
                                    segments[0], "version", segments[1]))
                            .filter(eeCapability -> eeFilters.stream().anyMatch(filter -> filter.matches(eeCapability)))
                            .map(ee -> ee.get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE) + '-'
                                    + ee.get("version"))
                            .map(ee -> ExecutionEnvironmentUtils.getExecutionEnvironment(ee, toolchainManager, session,
                                    logger))
                            .toArray(StandardExecutionEnvironment[]::new);
                }
            }
        }
        return manifestBREEs;
    }

    /*
     * mimics the behavior of the PDE incremental builder which by default copies all (non-java)
     * resource files in source directories into the target folder
     */
    private void doCopyResources() throws MojoExecutionException {
        if (!copyResources) {
            return;
        }
        for (String sourceRoot : getCompileSourceRoots()) {
            // StaleSourceScanner.getIncludedSources throws IllegalStateException
            // if directory doesnt't exist
            File sourceRootFile = new File(sourceRoot);

            if (!sourceRootFile.isDirectory()) {
                getLog().warn("Source directory " + sourceRoot + " does not exist");
                continue;
            }

            Set<String> excludes = new HashSet<>();
            excludes.addAll(excludeResources);
            excludes.addAll(getCompileSourceExcludePaths());
            excludes.addAll(getEclipsePluginProject().getBuildProperties().getBinExcludes());
            excludes.add("**/*.java");
            StaleSourceScanner scanner = new StaleSourceScanner(0L, MATCH_ALL, excludes);
            CopyMapping copyMapping = new CopyMapping();
            scanner.addSourceMapping(copyMapping);
            try {
                scanner.getIncludedSources(sourceRootFile, getOutputDirectory());
                for (CopyMapping.SourceTargetPair sourceTargetPair : copyMapping.getSourceTargetPairs()) {
                    FileUtils.copyFile(new File(sourceRoot, sourceTargetPair.source), sourceTargetPair.target);
                }
            } catch (InclusionScanException e) {
                throw new MojoExecutionException("Exception while scanning for resource files in " + sourceRoot, e);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Exception copying resource files from " + sourceRoot + " to " + getOutputDirectory(), e);
            }
        }
    }

    /** public for testing purposes */
    public EclipsePluginProject getEclipsePluginProject() throws MojoExecutionException {
        return ((OsgiBundleProject) getBundleProject()).getEclipsePluginProject(DefaultReactorProject.adapt(project));
    }

    @Override
    protected File getOutputDirectory() {
        return this.currentOutputDirectory;
    }

    @Override
    public List<String> getClasspathElements() throws MojoExecutionException {
        final List<String> classpath = new ArrayList<>();
        for (ClasspathEntry cpe : getClasspath()) {
            for (File location : cpe.getLocations()) {
                classpath.add(location.getAbsolutePath() + toString(cpe.getAccessRules()));
            }
        }
        return classpath;
    }

    protected BundleProject getBundleProject() throws MojoExecutionException {
        TychoProject projectType = projectTypes.get(project.getPackaging());
        if (!(projectType instanceof BundleProject)) {
            throw new MojoExecutionException("Not a bundle project " + project.toString());
        }
        return (BundleProject) projectType;
    }

    private String toString(Collection<AccessRule> rules) {
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

    @Override
    protected final List<String> getCompileSourceRoots() throws MojoExecutionException {
        return currentSourceRoots;
    }

    @Override
    protected final List<String> getCompileSourceExcludePaths() throws MojoExecutionException {
        return currentExcludes;
    }

    @Override
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

    @Override
    protected SourceInclusionScanner getSourceInclusionScanner(String inputFileEnding) {
        SourceInclusionScanner scanner = null;

        if (includes.isEmpty() && excludes.isEmpty()) {
            includes = Collections.singleton("**/*." + inputFileEnding);
            scanner = new SimpleSourceInclusionScanner(includes, Collections.<String> emptySet());
        } else {
            if (includes.isEmpty()) {
                includes.add("**/*." + inputFileEnding);
            }
            scanner = new SimpleSourceInclusionScanner(includes, excludes);
        }

        return scanner;
    }

    @Override
    protected CompilerConfiguration getCompilerConfiguration(List<String> compileSourceRoots,
            List<String> compileSourceExcludes) throws MojoExecutionException, MojoFailureException {
        CompilerConfiguration compilerConfiguration = super.getCompilerConfiguration(compileSourceRoots,
                compileSourceExcludes);
        if (useProjectSettings) {
            String prefsFilePath = project.getBasedir() + File.separator + PREFS_FILE_PATH;
            if (!new File(prefsFilePath).exists()) {
                getLog().warn("Parameter 'useProjectSettings' is set to true, but preferences file '" + prefsFilePath
                        + "' could not be found!");
            } else {
                compilerConfiguration.addCompilerCustomArgument("-properties", prefsFilePath);
            }
        }
        compilerConfiguration.setTargetVersion(getTargetLevel());
        compilerConfiguration.setSourceVersion(getSourceLevel());
        String releaseLevel = getReleaseLevel();
        if (releaseLevel != null) {
            compilerConfiguration.setReleaseVersion(releaseLevel);
        }
        configureJavaHome(compilerConfiguration);
        configureBootclasspathAccessRules(compilerConfiguration);
        configureCompilerLog(compilerConfiguration);
        return compilerConfiguration;
    }

    private void configureCompilerLog(CompilerConfiguration compilerConfiguration) throws MojoFailureException {
        if (log == null) {
            return;
        }
        if (compilerConfiguration.getCustomCompilerArgumentsAsMap().containsKey("-log")) {
            throw new MojoFailureException("Compiler logging is configured by the 'log' compiler"
                    + " plugin parameter and the custom compiler argument '-log'. Only either of them is allowed.");
        }
        logDirectory.mkdirs();
        String logFileName = null;
        if (new File(project.getBuild().getOutputDirectory()).getAbsolutePath()
                .equals(getOutputDirectory().getAbsolutePath())) {
            logFileName = "@dot";
        } else {
            String suffix = "-classes";
            String basePath = new File(project.getBuild().getDirectory()).getAbsolutePath();
            String subPath = getOutputDirectory().getAbsolutePath().substring(basePath.length()).replace('\\', '/');
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }
            String name = subPath.replaceAll("/", "_");
            if (name.endsWith(suffix)) {
                logFileName = name.substring(0, name.length() - suffix.length());
            } else {
                logFileName = name;
            }
        }
        String logPath = logDirectory.getAbsolutePath();
        if (!logPath.endsWith(File.separator)) {
            logPath = logPath + File.separator;
        }
        String fileExtension = log;
        if ("plain".equals(log)) {
            fileExtension = "log";
        }
        logPath = logPath + logFileName + "." + fileExtension;
        compilerConfiguration.addCompilerCustomArgument("-log", logPath);
    }

    private void configureBootclasspathAccessRules(CompilerConfiguration compilerConfiguration)
            throws MojoExecutionException {
        List<AccessRule> accessRules = new ArrayList<>();

        if (requireJREPackageImports) {
            accessRules.addAll(getStrictBootClasspathAccessRules());
        } else {
            accessRules.add(new DefaultAccessRule("java/**", false));
            getTargetExecutionEnvironment().getSystemPackages().stream() //
                    .map(systemPackage -> systemPackage.packageName) //
                    .distinct() //
                    .map(packageName -> packageName.trim().replace('.', '/') + "/*") //
                    .map(accessRule -> new DefaultAccessRule(accessRule, false)) //
                    .forEach(accessRules::add);
            // now add packages exported by framework extension bundles
            accessRules
                    .addAll(getBundleProject().getBootClasspathExtraAccessRules(DefaultReactorProject.adapt(project)));
        }
        if (!accessRules.isEmpty()) {
            compilerConfiguration.addCompilerCustomArgument("org.osgi.framework.system.packages",
                    toString(accessRules));
        }
    }

    @SuppressWarnings("unchecked")
    private List<AccessRule> getStrictBootClasspathAccessRules() throws MojoExecutionException {
        return (List<AccessRule>) DefaultReactorProject.adapt(project)
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_STRICT_BOOTCLASSPATH_ACCESSRULES);
    }

    private void configureJavaHome(CompilerConfiguration compilerConfiguration) throws MojoExecutionException {
        if (useJDK != JDKUsage.BREE) {
            return;
        }
        StandardExecutionEnvironment[] brees = getBREE();
        String toolchainId = null;
        if (brees.length > 0) {
            toolchainId = brees[0].getProfileName();
        } else {
            getLog().warn(
                    "useJDK=BREE configured, but no BREE is set in bundle. Fail back to currently running execution environment ("
                            + getTargetExecutionEnvironment().getProfileName() + ").");
            toolchainId = getTargetExecutionEnvironment().getProfileName();
        }
        DefaultJavaToolChain toolChain = toolchainProvider.findMatchingJavaToolChain(session, toolchainId);
        if (toolChain == null) {
            throw new MojoExecutionException("useJDK = BREE configured, but no toolchain of type 'jdk' with id '"
                    + toolchainId + "' found. See http://maven.apache.org/guides/mini/guide-using-toolchains.html");
        }
        compilerConfiguration.addCompilerCustomArgument("use.java.home", toolChain.getJavaHome());
        configureBootClassPath(compilerConfiguration, toolChain);
    }

    private void configureBootClassPath(CompilerConfiguration compilerConfiguration,
            DefaultJavaToolChain javaToolChain) {
        Xpp3Dom config = (Xpp3Dom) javaToolChain.getModel().getConfiguration();
        if (config != null) {
            Xpp3Dom bootClassPath = config.getChild("bootClassPath");
            if (bootClassPath != null) {
                Xpp3Dom includeParent = bootClassPath.getChild("includes");
                if (includeParent != null) {
                    Xpp3Dom[] includes = includeParent.getChildren("include");
                    if (includes.length > 0) {
                        compilerConfiguration.addCompilerCustomArgument("-bootclasspath", scanBootclasspath(
                                javaToolChain.getJavaHome(), includes, bootClassPath.getChild("excludes")));
                    }
                }
            }
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

    private ExecutionEnvironment getTargetExecutionEnvironment() {
        // never null
        return TychoProjectUtils.getExecutionEnvironmentConfiguration(DefaultReactorProject.adapt(project))
                .getFullSpecification();
    }

    @Override
    public List<ClasspathEntry> getClasspath() throws MojoExecutionException {
        TychoProject projectType = getBundleProject();
        ArrayList<ClasspathEntry> classpath = new ArrayList<>(
                ((BundleProject) projectType).getClasspath(DefaultReactorProject.adapt(project)));

        if (extraClasspathElements != null) {
            ArtifactRepository localRepository = session.getLocalRepository();
            List<ArtifactRepository> remoteRepositories = project.getRemoteArtifactRepositories();
            for (Dependency extraDependency : extraClasspathElements) {
                Artifact artifact = repositorySystem.createDependencyArtifact(extraDependency);

                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setLocalRepository(localRepository);
                request.setRemoteRepositories(remoteRepositories);
                request.setResolveRoot(true);
                request.setResolveTransitively(true);
                ArtifactResolutionResult result = repositorySystem.resolve(request);

                if (result.hasExceptions()) {
                    throw new MojoExecutionException("Could not resolve extra classpath entry",
                            result.getExceptions().get(0));
                }

                for (Artifact b : result.getArtifacts()) {
                    MavenProject bProject = null;
                    if (b instanceof ProjectArtifact) {
                        bProject = ((ProjectArtifact) b).getProject();
                    }
                    ArrayList<File> bLocations = new ArrayList<>();
                    bLocations.add(b.getFile()); // TODO properly handle multiple project locations maybe
                    classpath.add(
                            new DefaultClasspathEntry(DefaultReactorProject.adapt(bProject), null, bLocations, null));
                }
            }
        }
        return classpath;
    }

    @Override
    public String getExecutionEnvironment() throws MojoExecutionException {
        return getTargetExecutionEnvironment().getProfileName();
    }

    @Override
    public String getSourceLevel() throws MojoExecutionException {
        // first, explicit POM configuration
        if (source != null) {
            return source;
        }
        // then, build.properties
        String javacSource = getEclipsePluginProject().getBuildProperties().getJavacSource();
        if (javacSource != null) {
            return javacSource;
        }
        String profileName = getEclipsePluginProject().getBuildProperties().getJreCompilationProfile();
        if (profileName != null) {
            return ExecutionEnvironmentUtils.getExecutionEnvironment(profileName, toolchainManager, session, logger)
                    .getCompilerSourceLevelDefault();
        }
        return Arrays.stream(getBREE()) //
                .map(ExecutionEnvironment::getCompilerSourceLevelDefault) //
                .filter(Objects::nonNull) //
                .min(Comparator.comparing(Version::parseVersion)) //
                .or(() -> Optional.ofNullable(getTargetExecutionEnvironment().getCompilerSourceLevelDefault())) //
                .orElse(DEFAULT_SOURCE_VERSION);
    }

    @Override
    public String getTargetLevel() throws MojoExecutionException {
        // first, explicit POM configuration
        if (target != null) {
            return target;
        }
        // then, build.properties
        String javacTarget = getEclipsePluginProject().getBuildProperties().getJavacTarget();
        if (javacTarget != null) {
            return javacTarget;
        }
        String profileName = getEclipsePluginProject().getBuildProperties().getJreCompilationProfile();
        if (profileName != null) {
            return ExecutionEnvironmentUtils.getExecutionEnvironment(profileName, toolchainManager, session, logger)
                    .getCompilerTargetLevelDefault();
        }
        return Arrays.stream(getBREE()) //
                .map(ExecutionEnvironment::getCompilerTargetLevelDefault) //
                .filter(Objects::nonNull) //
                .min(Comparator.comparing(Version::parseVersion)) //
                .or(() -> Optional.ofNullable(getTargetExecutionEnvironment().getCompilerTargetLevelDefault())) //
                .orElse(DEFAULT_TARGET_VERSION);
    }

    @Override
    public String getReleaseLevel() throws MojoExecutionException {
        // first, explicit POM configuration
        if (release != null) {
            return release;
        }

        // implicit determination disabled
        if (!deriveReleaseCompilerArgumentFromTargetLevel) {
            return null;
        }

        String targetLevel = getTargetLevel();
        String[] targetLevelSplit = targetLevel.split("\\.");

        String releaseLevel;
        if (targetLevelSplit.length == 1 && targetLevelSplit[0].matches("\\d+")) {
            releaseLevel = targetLevelSplit[0];
        } else if (targetLevelSplit.length == 2 && "1".equals(targetLevelSplit[0])
                && targetLevelSplit[1].matches("\\d+")) {
            releaseLevel = targetLevelSplit[1];
        } else {
            logger.debug("Cannot determining 'maven.compiler.release' property automatically, because target level '"
                    + targetLevel + "' has an unexpected format.");
            return null;
        }

        CtSym ctSym;
        try {
            ctSym = JRTUtil.getCtSym(Paths.get(System.getProperty("java.home")));
        } catch (IOException e) {
            logger.warn("Unable to determine 'maven.compiler.release' property automatically", e);
            return null;
        }

        // TODO: Replace this with CtSym#getReleaseCode(String) once eclipse.jdt.core/5ba272a2d4a7478e0eb3951208ab49b7c069f37d is available with newer ECJ release
        int releaseLevelInt = Integer.parseInt(releaseLevel);
        String releaseCode = releaseLevelInt < 10 ? releaseLevel
                : String.valueOf((char) ('A' + (releaseLevelInt - 10)));

        List<Path> releaseRoots = ctSym.releaseRoots(releaseCode);
        if (!releaseRoots.isEmpty()) {
            return releaseLevel;
        } else {
            logger.debug("Not determining 'maven.compiler.release' property automatically, because level '"
                    + releaseLevel + "' is not supported by compiler.");
            return null;
        }
    }

    private void checkTargetLevelCompatibleWithManifestBREEs(String effectiveTargetLevel,
            StandardExecutionEnvironment[] manifestBREEs) throws MojoExecutionException {
        List<String> incompatibleBREEs = new ArrayList<>();
        for (StandardExecutionEnvironment ee : manifestBREEs) {
            if (!ee.isCompatibleCompilerTargetLevel(effectiveTargetLevel)) {
                incompatibleBREEs.add(ee.getProfileName() + " (assumes " + ee.getCompilerTargetLevelDefault() + ")");
            }
        }
        if (!incompatibleBREEs.isEmpty()) {
            String message = "The effective compiler target level " + effectiveTargetLevel
                    + " is incompatible with the following OSGi execution environments: " + incompatibleBREEs + " @ "
                    + project;
            if (strictCompilerTarget) {
                throw new MojoExecutionException(message);
            }
            getLog().warn(message);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(JavaCompilerConfiguration.class)) {
            return adapter.cast(this);
        }
        return null;
    }

}
