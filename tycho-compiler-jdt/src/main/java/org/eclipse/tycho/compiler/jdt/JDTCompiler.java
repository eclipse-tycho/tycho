/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sonatype Inc. - plexus.compiler implementation
 *    Thomas Demande (BSB group) - added support for annotation processors (bug 360427)
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;
import org.osgi.framework.Constants;

/**
 * See https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.isv/guide/jdt_api_options.htm
 */

@Named("jdt")
@Singleton
public class JDTCompiler extends AbstractCompiler {

    private static final String SEPARATOR = "----------";

    private static final char[] SEPARATOR_CHARS = new char[] { '/', '\\' };

    private static final char[] ADAPTER_PREFIX = "#ADAPTER#".toCharArray(); //$NON-NLS-1$

    private static final char[] ADAPTER_ENCODING = "ENCODING#".toCharArray(); //$NON-NLS-1$

    private static final char[] ADAPTER_ACCESS = "ACCESS#".toCharArray(); //$NON-NLS-1$

    static final Pattern LINE_PATTERN = Pattern
            .compile("(?:(\\d*)\\. )?(ERROR|WARNING) in (.*?)( \\(at line (\\d+)\\))?\\s*");

    static final String COMPILER_NAME = getCompilerName();

    @Inject
    private JdkLibraryInfoProvider jdkLibInfoProvider;

    @Inject
    private Logger logger;

    public JDTCompiler() {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null);
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    @Override
    public CompilerResult performCompile(CompilerConfiguration config) throws CompilerException {
        CustomCompilerConfiguration custom = new CustomCompilerConfiguration();

        File destinationDir = new File(config.getOutputLocation());

        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String[] sourceFiles = getSourceFiles(config);

        if (sourceFiles.length == 0) {
            return new CompilerResult();
        }
        //even though order of java sources should not matter, it can make a difference sometimes (e.g. lamda numbering) see https://github.com/eclipse-jdt/eclipse.jdt.core/issues/1921
        //so to have always the same arguments regardless of hash table ordering that is used internally by plexus compiler we sort the files simply by name
        Arrays.sort(sourceFiles);

        logger.info("Compiling " + sourceFiles.length + " " + "source file" + (sourceFiles.length == 1 ? "" : "s")
                + " to " + destinationDir.getAbsolutePath() + " using " + COMPILER_NAME + "");

        Collection<Map.Entry<String, String>> customCompilerArgumentEntries = config
                .getCustomCompilerArgumentsEntries();
        checkCompilerArgs(customCompilerArgumentEntries, custom);

        String[] args = buildCompilerArguments(config, custom, sourceFiles);

        CompilerResult messages;

        if (requireFork(config, custom)) {
            messages = compileOutOfProcess(args, config, custom);
        } else {
            messages = compileInProcess(args, config, custom);
        }

        return messages;
    }

    private static String getCompilerName() {

        try {
            URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            try (JarFile jarFile = new JarFile(file)) {
                Manifest manifest = jarFile.getManifest();
                String name = manifest.getMainAttributes().getValue(Constants.BUNDLE_NAME);
                String version = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                if (name != null && version != null) {
                    return name + " " + version;
                }
                if (version != null) {
                    return "Eclipse Compiler for Java(TM) " + version;
                }
                if (name != null) {
                    return name;
                }
            }
        } catch (Exception e) {
        }
        return "Unknown Compiler";
    }

    private boolean requireFork(CompilerConfiguration config, CustomCompilerConfiguration custom) {
        if (config.isFork()) {
            return true;
        }
//        if (custom.javaHome != null) {
//            String sourceLevel = config.getSourceVersion();
//            if (sourceLevel == null || CompilerOptions.versionToJdkLevel(sourceLevel) <= ClassFileConstants.JDK1_8) {
//                return false;
//            }
//            return true;
//        }
        return false;
    }

    @Override
    public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
        return buildCompilerArguments(config, new CustomCompilerConfiguration(), getSourceFiles(config));
    }

    public String[] buildCompilerArguments(CompilerConfiguration config, CustomCompilerConfiguration custom,
            String[] sourceFiles) {
        List<String> args = new ArrayList<>();

        Collection<Map.Entry<String, String>> customCompilerArgumentsEntries = config
                .getCustomCompilerArgumentsEntries();
        for (Map.Entry<String, String> entry : customCompilerArgumentsEntries) {

            String key = entry.getKey();

            if (StringUtils.isEmpty(key) || key.startsWith("@")) {
                continue;
            }

            if ("use.java.home".equals(key)) {
                custom.javaHome = entry.getValue();
                continue;
            }

            if ("org.osgi.framework.system.packages".equals(key)) {
                custom.bootclasspathAccessRules = entry.getValue();
                continue;
            }

            args.add(key);

            String value = entry.getValue();

            if (StringUtils.isEmpty(value)) {
                continue;
            }

            args.add(value);
        }

        // ----------------------------------------------------------------------
        // Set output
        // ----------------------------------------------------------------------

        File destinationDir = new File(config.getOutputLocation());

        if (!StringUtils.isEmpty(config.getProc())) {
            args.add("-proc:" + config.getProc());
        }

        String[] annotationProcessors = config.getAnnotationProcessors();
        if (annotationProcessors != null && annotationProcessors.length > 0) {
            args.add("-processor");
            args.add(StringUtils.join(annotationProcessors, ","));
        }

        if (config.getGeneratedSourcesDirectory() != null) {
            config.getGeneratedSourcesDirectory().mkdirs();

            args.add("-s");
            args.add(config.getGeneratedSourcesDirectory().getAbsolutePath());
        }
        args.add("-d");

        args.add(destinationDir.getAbsolutePath());

        // ----------------------------------------------------------------------
        // Set the class and source paths
        // ----------------------------------------------------------------------

        List<String> classpathEntries = config.getClasspathEntries();
        if (classpathEntries != null && !classpathEntries.isEmpty()) {
            args.add("-classpath");

            String cp = createClasspathArgument(classpathEntries, custom);

            args.add(cp);
        }

        List<String> sourceLocations = config.getSourceLocations();
        if (sourceLocations != null && !sourceLocations.isEmpty() && (sourceFiles.length == 0)) {
            args.add("-sourcepath");

            args.add(getPathString(sourceLocations));
        }

        args.addAll(Arrays.asList(sourceFiles));

        if (config.isOptimize()) {
            args.add("-O");
        }

        if (config.isDebug()) {
            args.add("-g");
        }

        if (config.isVerbose()) {
            args.add("-verbose");
        }

        if (config.isShowDeprecation()) {
            args.add("-deprecation");

            // This is required to actually display the deprecation messages
            config.setShowWarnings(true);
        }

        if (!StringUtils.isEmpty(config.getMaxmem())) {
            args.add("-J-Xmx" + config.getMaxmem());
        }

        if (!StringUtils.isEmpty(config.getMeminitial())) {
            args.add("-J-Xms" + config.getMeminitial());
        }

        if (!config.isShowWarnings()) {
            args.add("-nowarn");
        }

        if (config.isFailOnWarning()) {
            args.add("-failOnWarning");
        }

        // TODO: this could be much improved
        if (StringUtils.isEmpty(config.getTargetVersion())) {
            // Required, or it defaults to the target of your JDK (eg 1.5)
            args.add("-target");
            args.add("1.1");
        } else {
            args.add("-target");
            args.add(config.getTargetVersion());
        }

        if (StringUtils.isEmpty(config.getSourceVersion())) {
            // If omitted, later JDKs complain about a 1.1 target
            args.add("-source");
            args.add("1.3");
        } else {
            args.add("-source");
            args.add(config.getSourceVersion());
        }

        if (!StringUtils.isEmpty(config.getReleaseVersion())) {
            if (custom.javaHome == null) {
                //release can only be used without custom java home!
                args.add("--release");
                args.add(config.getReleaseVersion());
            } else {
                logger.debug("Custom java home and --release are incompatible, ignore --release="
                        + config.getReleaseVersion() + " setting ");
            }
        }

        if (!StringUtils.isEmpty(config.getSourceEncoding())) {
            args.add("-encoding");
            args.add(config.getSourceEncoding());
        }

        return args.toArray(new String[args.size()]);
    }

    /**
     * Compile the java sources in a external process, calling an external executable, like javac.
     *
     */
    private CompilerResult compileOutOfProcess(String[] args, CompilerConfiguration config,
            CustomCompilerConfiguration custom) throws CompilerException {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory(config.getWorkingDirectory().getAbsolutePath());

        cli.setExecutable(new File(custom.javaHome, "bin/java").getAbsolutePath());
        List<String> arguments = new ArrayList<String>();
        arguments.add("-cp");
        try {
            URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            arguments.add(file.getAbsolutePath());
        } catch (Exception e) {
            throw new CompilerException("Can't determine location of the compiler!", e);
        }
        arguments.add(Main.class.getName());
        arguments.addAll(Arrays.asList(args));
        cli.addArguments(arguments.toArray(String[]::new));

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        int returnCode;

        List<CompilerMessage> messages;

        try {
            returnCode = CommandLineUtils.executeCommandLine(cli, out, err);

            messages = parseModernStream(new BufferedReader(new StringReader(err.getOutput())));
        } catch (CommandLineException | IOException e) {
            throw new CompilerException("Error while executing the external compiler.", e);
        }

        if (returnCode != 0 && messages.isEmpty()) {
            // low-level, e.g. configuration error
            throw new CompilerException(
                    "Failure executing ejc, but could not parse the error:" + EOL + err.getOutput());
        }

        return new CompilerResult(returnCode == 0, messages);
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable, using
     * <code>com.sun.tools.javac.Main</code> class
     *
     * @param args
     *            arguments for the compiler as they would be used in the command line javac
     * @return CompilerResult with the errors and warnings encountered.
     * @throws CompilerException
     */
    CompilerResult compileInProcess(String[] args, CompilerConfiguration config, CustomCompilerConfiguration custom)
            throws CompilerException {

        List<CompilerMessage> messages;

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        Main compiler = new Main(new PrintWriter(out), new PrintWriter(err), false, null, null);
        compiler.options.put(CompilerOptions.OPTION_ReportForbiddenReference, CompilerOptions.ERROR);
        List<String> jdtCompilerArgs = new ArrayList<>(Arrays.asList(args));
        if (custom.javaHome != null) {
            String sourceLevel = config.getSourceVersion();
            if (sourceLevel == null || CompilerOptions.versionToJdkLevel(sourceLevel) <= ClassFileConstants.JDK1_8) {
                try {
                    addExternalJavaHomeArgs(jdtCompilerArgs, custom.javaHome);
                } catch (ArtifactResolutionException e) {
                    throw new CompilerException("can't determine required options", e);
                }
            } else {
                addToCompilerArgumentsIfNotSet("--system", custom.javaHome, jdtCompilerArgs);
            }
        }
        logger.debug("JDT compiler args: " + jdtCompilerArgs);
        boolean success = compiler.compile(jdtCompilerArgs.toArray(new String[0]));

        try {
            String output = err.toString();
            logger.debug("Original compiler output: " + output);
            messages = parseModernStream(new BufferedReader(new StringReader(output)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!success && messages.isEmpty()) {
            // low-level, e.g. configuration error
            throw new CompilerException(err.toString());
        }
        return new CompilerResult(success, messages);
    }

    private void addExternalJavaHomeArgs(List<String> jdtCompilerArgs, String javaHome)
            throws ArtifactResolutionException {
        LibraryInfo jdkLibInfo = jdkLibInfoProvider.getLibraryInfo(javaHome);
        if (jdkLibInfo.getBootpath().length > 0) {
            addToCompilerArgumentsIfNotSet("-bootclasspath", String.join(File.pathSeparator, jdkLibInfo.getBootpath()),
                    jdtCompilerArgs);
        }
        if (jdkLibInfo.getExtensionDirs().length > 0) {
            addToCompilerArgumentsIfNotSet("-extdirs", String.join(File.pathSeparator, jdkLibInfo.getExtensionDirs()),
                    jdtCompilerArgs);
        }
        if (jdkLibInfo.getEndorsedDirs().length > 0) {
            addToCompilerArgumentsIfNotSet("-endorseddirs",
                    String.join(File.pathSeparator, jdkLibInfo.getEndorsedDirs()), jdtCompilerArgs);
        }
    }

    private void addToCompilerArgumentsIfNotSet(String argument, String value, List<String> compilerArguments) {
        if (compilerArguments.contains(argument)) {
            // argument explicitly set by user - nothing to do
            return;
        }
        compilerArguments.add(argument);
        compilerArguments.add(value);
    }

    /**
     * Parse the output from the compiler into a list of CompilerMessage objects
     *
     * @param input
     *            The output of the compiler
     * @return List of CompilerMessage objects
     * @throws IOException
     */
    protected static List<CompilerMessage> parseModernStream(BufferedReader input) throws IOException {
        List<CompilerMessage> messages = new ArrayList<>();
        String type = null;
        String file = null;
        int lineNr = -1;
        StringBuilder messageBuffer = new StringBuilder();
        for (String line = input.readLine(); line != null; line = input.readLine()) {
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                addMessageIfFound(messages, type, file, lineNr, messageBuffer.toString());
                /* String errorNr = */matcher.group(1);
                type = matcher.group(2);
                file = matcher.group(3);
                String lineNumberString = matcher.group(5);
                if (lineNumberString != null) {
                    lineNr = Integer.parseInt(lineNumberString);
                } else {
                    lineNr = -1;
                }
                messageBuffer = new StringBuilder();
            } else {
                // context line
                if (!SEPARATOR.equals(line) && line.trim().length() > 0) {
                    messageBuffer.append(EOL).append(line);
                }
            }
        }
        addMessageIfFound(messages, type, file, lineNr, messageBuffer.toString());
        return messages;
    }

    private static void addMessageIfFound(List<CompilerMessage> messages, String type, String file, int line,
            String message) {
        if (type != null) {
            Kind kind = "ERROR".equals(type) ? Kind.ERROR : Kind.WARNING;
            messages.add(new CompilerMessage(file, kind, line, 0, line, 0, message));
        }
    }

    // /////////////////////////////////////////////:
    // copied from JDTCompilerAdapter

    /**
     * Copy the classpath to the command line with access rules included.
     *
     * @param cmd
     *            the given command line
     * @param classpath
     *            the given classpath entry
     */
    private String createClasspathArgument(List<String> classpath, CustomCompilerConfiguration custom) {
        final String[] pathElements = classpath.toArray(new String[classpath.size()]);

        // empty path return empty string
        if (pathElements.length == 0) {
            return "";
        }

        // no access rules, can set the path directly
        if (custom.accessRules == null) {
            return Stream.of(pathElements).collect(Collectors.joining(File.pathSeparator));
        }

        int rulesLength = custom.accessRules.size();
        String[] rules = custom.accessRules.toArray(new String[rulesLength]);
        int nextRule = 0;
        final StringJoiner result = new StringJoiner(File.pathSeparator);

        // access rules are expected in the same order as the classpath, but
        // there could
        // be elements in the classpath not in the access rules or access rules
        // not in the classpath
        for (String pathElement : pathElements) {
            result.add(pathElement);
            // the rules list is [path, rule, path, rule, ...]
            for (int j = nextRule; j < rulesLength; j += 2) {
                String rule = rules[j];
                if (pathElement.endsWith(rule)) {
                    result.add(rules[j + 1]);
                    nextRule = j + 2;
                    break;
                }
                // if the path doesn't match, it could be due to a trailing file
                // separatorChar in the rule
                if (rule.endsWith(File.separator)) {
                    // rule ends with the File.separator, but pathElement might
                    // not
                    // otherwise it would match on the first endsWith
                    int ruleLength = rule.length();
                    if (pathElement.regionMatches(false, pathElement.length() - ruleLength + 1, rule, 0,
                            ruleLength - 1)) {
                        result.add(rules[j + 1]);
                        nextRule = j + 2;
                        break;
                    }
                } else if (pathElement.endsWith(File.separator)) {
                    // rule doesn't end with the File.separator, but pathElement
                    // might
                    int ruleLength = rule.length();
                    if (pathElement.regionMatches(false, pathElement.length() - ruleLength - 1, rule, 0, ruleLength)) {
                        result.add(rules[j + 1]);
                        nextRule = j + 2;
                        break;
                    }
                }
            }
        }

        return result.toString();
    }

    /**
     * check the compiler arguments. Extract from files specified using @, lines marked with
     * {@link #ADAPTER_PREFIX}. These lines specify information that needs to be interpreted by us.
     *
     * @param args
     *            compiler arguments to process
     */
    private void checkCompilerArgs(Collection<Map.Entry<String, String>> argEntries,
            CustomCompilerConfiguration custom) {
        for (Map.Entry<String, String> argEntry : argEntries) {
            String arg = argEntry.getKey();
            if (arg.charAt(0) == '@') {
                try {
                    char[] content = Util.getFileCharContent(new File(arg.substring(1)), null);
                    int offset = 0;
                    int prefixLength = ADAPTER_PREFIX.length;
                    while ((offset = CharOperation.indexOf(ADAPTER_PREFIX, content, true, offset)) > -1) {
                        int start = offset + prefixLength;
                        int end = CharOperation.indexOf('\n', content, start);
                        if (end == -1)
                            end = content.length;
                        while (CharOperation.isWhitespace(content[end])) {
                            end--;
                        }

                        // end is inclusive, but in the API end is exclusive
                        if (CharOperation.equals(ADAPTER_ENCODING, content, start, start + ADAPTER_ENCODING.length)) {
                            CharOperation.replace(content, SEPARATOR_CHARS, File.separatorChar, start, end + 1);
                            // file or folder level custom encoding
                            start += ADAPTER_ENCODING.length;
                            int encodeStart = CharOperation.lastIndexOf('[', content, start, end);
                            if (start < encodeStart && encodeStart < end) {
                                boolean isFile = CharOperation.equals(SuffixConstants.SUFFIX_java, content,
                                        encodeStart - 5, encodeStart, false);

                                String str = String.valueOf(content, start, encodeStart - start);
                                String enc = String.valueOf(content, encodeStart, end - encodeStart + 1);
                                if (isFile) {
                                    if (custom.fileEncodings == null)
                                        custom.fileEncodings = new HashMap<>();
                                    // use File to translate the string into a
                                    // path with the correct File.separator
                                    custom.fileEncodings.put(str, enc);
                                } else {
                                    if (custom.dirEncodings == null)
                                        custom.dirEncodings = new HashMap<>();
                                    custom.dirEncodings.put(str, enc);
                                }
                            }
                        } else if (CharOperation.equals(ADAPTER_ACCESS, content, start,
                                start + ADAPTER_ACCESS.length)) {
                            // access rules for the classpath
                            start += ADAPTER_ACCESS.length;
                            int accessStart = CharOperation.indexOf('[', content, start, end);
                            // CharOperation.replace(content, SEPARATOR_CHARS,
                            // File.separatorChar, start, accessStart);
                            if (start < accessStart && accessStart < end) {
                                String path = String.valueOf(content, start, accessStart - start);
                                String access = String.valueOf(content, accessStart, end - accessStart + 1);
                                if (custom.accessRules == null)
                                    custom.accessRules = new ArrayList<>();
                                custom.accessRules.add(path);
                                custom.accessRules.add(access);
                            }
                        }
                        offset = end;
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }

    @Override
    public String getCompilerId() {
        return "tycho-jdt";
    }

}
