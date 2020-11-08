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

package copied.org.apache.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.manager.NoSuchCompilerException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.StringUtils;

/**
 * TODO: At least one step could be optimized, currently the plugin will do two scans of all the
 * source code if the compiler has to have the entire set of sources. This is currently the case for
 * at least the C# compiler and most likely all the other .NET compilers too.
 * 
 * @author others
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author Jan Sievers (SAP) added support for annotation processor options
 * @version $Id: AbstractCompilerMojo.java 210 2007-02-20 03:02:41Z jvanzyl $
 */
public abstract class AbstractCompilerMojo extends AbstractMojo {

    public static final String DEFAULT_SOURCE_VERSION = "11";

    public static final String DEFAULT_TARGET_VERSION = "11";

    // ----------------------------------------------------------------------
    // Configurables
    // ----------------------------------------------------------------------

    /**
     * Whether to include debugging information in the compiled class files. The default value is
     * true.
     */
    @Parameter(property = "maven.compiler.debug", defaultValue = "true")
    private boolean debug;

    /**
     * Whether to output messages about what the compiler is doing
     */
    @Parameter(property = "maven.compiler.verbose", defaultValue = "false")
    private boolean verbose;

    /**
     * Output source locations where deprecated APIs are used
     */
    @Parameter(property = "maven.compiler.showDeprecation", defaultValue = "false")
    private boolean showDeprecation;

    /**
     * Optimize compiled code using the compiler's optimization methods
     */
    @Parameter(property = "maven.compiler.optimize", defaultValue = "false")
    private boolean optimize;

    /**
     * Output warnings
     */
    @Parameter(property = "maven.compiler.showWarnings", defaultValue = "false")
    private boolean showWarnings;

    /**
     * Fail on warnings
     */
    @Parameter(property = "maven.compiler.failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    /**
     * The -source argument for the Java compiler
     */
    @Parameter(property = "maven.compiler.source")
    protected String source;

    /**
     * The -target argument for the Java compiler
     */
    @Parameter(property = "maven.compiler.target")
    protected String target;

    /**
     * The -release argument for the Java compiler
     */
    @Parameter(property = "maven.compiler.release")
    protected String release;

    /**
     * The -encoding argument for the Java compiler (kept for backwards compatibility)
     * 
     * @deprecated use {@link #encoding}
     */
    @Parameter(property = "maven.compiler.encoding", readonly = true)
    private String mavenCompilerEncoding;

    /**
     * The -encoding argument for the Java compiler
     */
    @Parameter(property = "project.build.sourceEncoding")
    private String encoding;

    /**
     * The granularity in milliseconds of the last modification date for testing whether a source
     * needs recompilation
     */
    @Parameter(property = "lastModGranularityMs", defaultValue = "0")
    private int staleMillis;

    /**
     * The compiler id of the compiler to use.
     */
    @Parameter(property = "maven.compiler.compilerId", defaultValue = "jdt")
    private String compilerId;

    /**
     * Version of the compiler to use, ex. "1.3", "1.5", if fork is set to true
     */
    @Parameter(property = "maven.compiler.compilerVersion")
    private String compilerVersion;

    /**
     * Allows running the compiler in a separate process. If "false" it uses the built in compiler,
     * while if "true" it will use an executable.
     */
    @Parameter(defaultValue = "false")
    private boolean fork;

    /**
     * Initial size, in megabytes, of the memory allocation pool, ex. "64", "64m" if fork is set to
     * true
     */
    @Parameter(property = "maven.compiler.meminitial")
    private String meminitial;

    /**
     * maximum size, in megabytes, of the memory allocation pool, ex. "128", "128m" if fork is set
     * to true
     */
    @Parameter(property = "maven.compiler.maxmem")
    private String maxmem;

    /**
     * The executable of the compiler to use when fork is true.
     */
    @Parameter(property = "maven.compiler.executable")
    private String executable;

    /**
     * If <tt>only</tt> is specified, the annotation processors will run but no compilation will be
     * performed. If <tt>none</tt> is specified, annotation processors will not be discovered or
     * run; compilation will proceed as if no annotation processors were found. By default the
     * compiler must search the classpath for annotation processors, so specifying <tt>none</tt> may
     * speed compilation if annotation processing is not required. This parameter requires a 1.6 VM
     * or above and is used only if the compliance is 1.6
     * 
     * @since 0.16.0
     */
    @Parameter
    private String proc;

    /**
     * Qualified class names of annotation processors to run. If specified, the
     * <a href="http://java.sun.com/javase/6/docs/api/javax/annotation/processing/Processor.html" >
     * normal processor discovery process</a> will be skipped. This parameter requires a 1.6 VM or
     * above and is used only if the compliance is 1.6
     * 
     * @since 0.16.0
     */
    @Parameter
    private String[] annotationProcessors;

    /**
     * The directory where source files generated by annotation processors will be created. This
     * parameter requires a 1.6 VM or above and is used only if the compliance is 1.6.
     * 
     * @since 0.16.0
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/annotations")
    private File generatedSourcesDirectory;

    /**
     * <p>
     * Arguments to be passed to the compiler (prepending a dash) if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler varies based on the
     * compiler version.
     * </p>
     * 
     * @deprecated use {@link #compilerArgs} instead.
     */
    @Parameter
    private Map<String, String> compilerArguments;

    /**
     * Arguments to be passed to the compiler.
     * 
     * @since 0.17.0
     */
    @Parameter
    private List<String> compilerArgs;

    /**
     * <p>
     * Unformatted argument string to be passed to the compiler if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler varies based on the
     * compiler version.
     * </p>
     */
    @Parameter
    private String compilerArgument;

    /**
     * Used to control the name of the output file when compiling a set of sources to a single file.
     */
    @Parameter(property = "project.build.finalName")
    private String outputFileName;

    // ----------------------------------------------------------------------
    // Read-only parameters
    // ----------------------------------------------------------------------

    /**
     * The directory to run the compiler from if fork is true.
     */
    @Parameter(property = "basedir", required = true, readonly = true)
    private File basedir;

    /**
     * The target directory of the compiler if fork is true.
     */
    @Parameter(property = "project.build.directory", readonly = true, required = true)
    private File buildDirectory;

    /**
     * Plexus compiler manager.
     */
    @Component
    private CompilerManager compilerManager;

    protected abstract SourceInclusionScanner getSourceInclusionScanner(int staleMillis);

    protected abstract SourceInclusionScanner getSourceInclusionScanner(String inputFileEnding);

    protected abstract List<String> getClasspathElements() throws MojoExecutionException;

    protected abstract List<String> getCompileSourceRoots() throws MojoExecutionException;

    protected abstract List<String> getCompileSourceExcludePaths() throws MojoExecutionException;

    protected abstract List<File> getCompileSourceExcludeFiles() throws MojoExecutionException;

    protected abstract File getOutputDirectory();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // ----------------------------------------------------------------------
        // Look up the compiler. This is done before other code than can
        // cause the mojo to return before the lookup is done possibly resulting
        // in misconfigured POMs still building.
        // ----------------------------------------------------------------------

        Compiler compiler;

        getLog().debug("Using compiler '" + compilerId + "'.");

        try {
            compiler = compilerManager.getCompiler(compilerId);
        } catch (NoSuchCompilerException e) {
            throw new MojoExecutionException("No such compiler '" + e.getCompilerId() + "'.");
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        List<String> compileSourceRoots = removeEmptyCompileSourceRoots(getCompileSourceRoots());

        if (compileSourceRoots.isEmpty()) {
            getLog().info("No sources to compile");

            return;
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Source directories: " + compileSourceRoots.toString().replace(',', '\n'));
            getLog().debug("Classpath: " + getClasspathElements().toString().replace(',', '\n'));
            getLog().debug("Output directory: " + getOutputDirectory());
        }

        // ----------------------------------------------------------------------
        // Create the compiler configuration
        // ----------------------------------------------------------------------

        CompilerConfiguration compilerConfiguration = getCompilerConfiguration(compileSourceRoots,
                getCompileSourceExcludePaths());

        // TODO: have an option to always compile (without need to clean)
        Set<File> staleSources;

        boolean canUpdateTarget;

        try {
            staleSources = computeStaleSources(compilerConfiguration, compiler, getSourceInclusionScanner(staleMillis));

            canUpdateTarget = compiler.canUpdateTarget(compilerConfiguration);

            if (compiler.getCompilerOutputStyle().equals(CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES)
                    && !canUpdateTarget) {
                getLog().info("RESCANNING!");
                // TODO: This second scan for source files is sub-optimal
                String inputFileEnding = compiler.getInputFileEnding(compilerConfiguration);

                Set<File> sources = computeStaleSources(compilerConfiguration, compiler,
                        getSourceInclusionScanner(inputFileEnding));

                compilerConfiguration.setSourceFiles(sources);
            } else {
                compilerConfiguration.setSourceFiles(staleSources);
            }
        } catch (CompilerException e) {
            throw new MojoExecutionException("Error while computing stale sources.", e);
        }

        if (staleSources.isEmpty()) {
            getLog().info("Nothing to compile - all classes are up to date");

            return;
        }

        // ----------------------------------------------------------------------
        // Dump configuration
        // ----------------------------------------------------------------------

        if (getLog().isDebugEnabled()) {
            getLog().debug("Classpath:");

            for (String s : getClasspathElements()) {
                getLog().debug(" " + s);
            }

            getLog().debug("Source roots:");

            for (String root : getCompileSourceRoots()) {
                getLog().debug(" " + root);
            }

            if (fork) {
                try {
                    if (compilerConfiguration.getExecutable() != null) {
                        getLog().debug("Excutable: ");
                        getLog().debug(" " + compilerConfiguration.getExecutable());
                    }

                    String[] cl = compiler.createCommandLine(compilerConfiguration);
                    if (cl != null && cl.length > 0) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(cl[0]);
                        for (int i = 1; i < cl.length; i++) {
                            sb.append(" ");
                            sb.append(cl[i]);
                        }
                        getLog().debug("Command line options:");
                        getLog().debug(sb);
                    }
                } catch (CompilerException ce) {
                    getLog().debug(ce);
                }
            }
        }

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        CompilerResult result;

        try {
            result = compiler.performCompile(compilerConfiguration);
        } catch (Exception e) {
            // TODO: don't catch Exception
            throw new MojoExecutionException("Fatal error compiling", e);
        }

        List<CompilerMessage> messages = result.getCompilerMessages();

        for (Iterator<CompilerMessage> i = messages.iterator(); i.hasNext();) {
            CompilerMessage message = i.next();
            if (!message.isError()) {
                getLog().warn(message.toString());
                i.remove();
            }
        }

        if (!result.isSuccess()) {
            throw new CompilationFailureException(messages);
        }
    }

    protected CompilerConfiguration getCompilerConfiguration(List<String> compileSourceRoots,
            List<String> compileSourceExcludes) throws MojoExecutionException, MojoFailureException {

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setOutputLocation(getOutputDirectory().getAbsolutePath());

        compilerConfiguration.setClasspathEntries(getClasspathElements());

        compilerConfiguration.setSourceLocations(compileSourceRoots);

        compilerConfiguration.setOptimize(optimize);

        compilerConfiguration.setDebug(debug);

        compilerConfiguration.setVerbose(verbose);

        compilerConfiguration.setShowWarnings(showWarnings);

        compilerConfiguration.setShowDeprecation(showDeprecation);

        compilerConfiguration.setProc(proc);

        compilerConfiguration.setFailOnWarning(failOnWarning);

        compilerConfiguration.setAnnotationProcessors(annotationProcessors);

        compilerConfiguration.setGeneratedSourcesDirectory(generatedSourcesDirectory);

        compilerConfiguration.setSourceVersion(source != null ? source : DEFAULT_SOURCE_VERSION);

        compilerConfiguration.setTargetVersion(target != null ? target : DEFAULT_TARGET_VERSION);
        if (release != null) {
            compilerConfiguration.setReleaseVersion(release);
        }

        compilerConfiguration.setSourceEncoding(getEncoding());

        if ((compilerArguments != null) || (compilerArgument != null) || compilerArgs != null) {
            if (compilerArguments != null) {
                for (Entry<String, String> me : compilerArguments.entrySet()) {
                    String key = (String) me.getKey();
                    String value = (String) me.getValue();
                    if (!key.startsWith("-")) {
                        key = "-" + key;
                    }
                    if (key.startsWith("-A") && StringUtils.isNotEmpty(value)) {
                        compilerConfiguration.addCompilerCustomArgument(key + "=" + value, null);
                    } else {
                        compilerConfiguration.addCompilerCustomArgument(key, value);
                    }
                }
            }
            if (!StringUtils.isEmpty(compilerArgument)) {
                compilerConfiguration.addCompilerCustomArgument(compilerArgument, null);
            }
            if (compilerArgs != null) {
                for (String arg : compilerArgs) {
                    compilerConfiguration.addCompilerCustomArgument(arg, null);
                }
            }
        }

        compilerConfiguration.setFork(fork);

        if (fork) {
            if (!StringUtils.isEmpty(meminitial)) {
                String value = getMemoryValue(meminitial);

                if (value != null) {
                    compilerConfiguration.setMeminitial(value);
                } else {
                    getLog().info("Invalid value for meminitial '" + meminitial + "'. Ignoring this option.");
                }
            }

            if (!StringUtils.isEmpty(maxmem)) {
                String value = getMemoryValue(maxmem);

                if (value != null) {
                    compilerConfiguration.setMaxmem(value);
                } else {
                    getLog().info("Invalid value for maxmem '" + maxmem + "'. Ignoring this option.");
                }
            }
        }

        compilerConfiguration.setExecutable(executable);

        compilerConfiguration.setWorkingDirectory(basedir);

        compilerConfiguration.setCompilerVersion(compilerVersion);

        compilerConfiguration.setBuildDirectory(buildDirectory);

        compilerConfiguration.setOutputFileName(outputFileName);

        for (String exclude : compileSourceExcludes) {
            compilerConfiguration.addExclude(exclude);
        }
        return compilerConfiguration;
    }

    private String getEncoding() {
        if (encoding != null) {
            return encoding;
        }
        return mavenCompilerEncoding;
    }

    private String getMemoryValue(String setting) {
        String value = null;

        // Allow '128' or '128m'
        if (isDigits(setting)) {
            value = setting + "m";
        } else {
            if ((isDigits(setting.substring(0, setting.length() - 1))) && (setting.toLowerCase().endsWith("m"))) {
                value = setting;
            }
        }
        return value;
    }

    private boolean isDigits(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isDigit(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Set<File> computeStaleSources(CompilerConfiguration compilerConfiguration, Compiler compiler,
            SourceInclusionScanner scanner) throws MojoExecutionException, CompilerException {
        CompilerOutputStyle outputStyle = compiler.getCompilerOutputStyle();

        SourceMapping mapping;

        File outputDirectory;

        if (outputStyle == CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE) {
            mapping = new SuffixMapping(compiler.getInputFileEnding(compilerConfiguration),
                    compiler.getOutputFileEnding(compilerConfiguration));

            outputDirectory = getOutputDirectory();
        } else if (outputStyle == CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES) {
            mapping = new SingleTargetSourceMapping(compiler.getInputFileEnding(compilerConfiguration),
                    compiler.getOutputFile(compilerConfiguration));

            outputDirectory = buildDirectory;
        } else {
            throw new MojoExecutionException("Unknown compiler output style: '" + outputStyle + "'.");
        }

        scanner.addSourceMapping(mapping);

        Set<File> staleSources = new HashSet<>();

        for (String sourceRoot : getCompileSourceRoots()) {
            File rootFile = new File(sourceRoot);

            if (!rootFile.isDirectory()) {
                continue;
            }

            try {
                staleSources.addAll(scanner.getIncludedSources(rootFile, outputDirectory));
            } catch (InclusionScanException e) {
                throw new MojoExecutionException(
                        "Error scanning source root: \'" + sourceRoot + "\' " + "for stale files to recompile.", e);
            }

        }
        staleSources.removeAll(getCompileSourceExcludeFiles());

        return staleSources;
    }

    /**
     * @todo also in ant plugin. This should be resolved at some point so that it does not need to
     *       be calculated continuously - or should the plugins accept empty source roots as is?
     */
    protected static List<String> removeEmptyCompileSourceRoots(List<String> compileSourceRootsList) {
        List<String> newCompileSourceRootsList = new ArrayList<>();
        if (compileSourceRootsList != null) {
            // copy as I may be modifying it
            for (String srcDir : compileSourceRootsList) {
                if (!newCompileSourceRootsList.contains(srcDir) && new File(srcDir).exists()) {
                    newCompileSourceRootsList.add(srcDir);
                }
            }
        }
        return newCompileSourceRootsList;
    }
}
