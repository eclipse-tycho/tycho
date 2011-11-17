/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Sonatype Inc. - plexus.compiler implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * See http://help.eclipse.org/ganymede/topic/org.eclipse.jdt.doc.isv/guide/jdt_api_options.htm
 */

@Component(role = org.codehaus.plexus.compiler.Compiler.class, hint = "jdt")
public class JDTCompiler extends AbstractCompiler {

    private static final String SEPARATOR = "----------";

    private static final char[] SEPARATOR_CHARS = new char[] { '/', '\\' };

    private static final char[] ADAPTER_PREFIX = "#ADAPTER#".toCharArray(); //$NON-NLS-1$

    private static final char[] ADAPTER_ENCODING = "ENCODING#".toCharArray(); //$NON-NLS-1$

    private static final char[] ADAPTER_ACCESS = "ACCESS#".toCharArray(); //$NON-NLS-1$

    String logFileName;

//    Map customDefaultOptions;

    private Map<String, String> fileEncodings = null;

    private Map<String, String> dirEncodings = null;

    private List<String> accessRules = null;

    private String javaHome = null;

    private String bootclasspathAccessRules = null;

    public JDTCompiler() {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null);
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public List<CompilerError> compile(CompilerConfiguration config) throws CompilerException {
        File destinationDir = new File(config.getOutputLocation());

        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String[] sourceFiles = getSourceFiles(config);

        if (sourceFiles.length == 0) {
            return Collections.emptyList();
        }

        getLogger().info(
                "Compiling " + sourceFiles.length + " " + "source file" + (sourceFiles.length == 1 ? "" : "s") + " to "
                        + destinationDir.getAbsolutePath());

        @SuppressWarnings("unchecked")
        Map<String, String> customCompilerArguments = config.getCustomCompilerArguments();
        checkCompilerArgs(customCompilerArguments);

        String[] args = buildCompilerArguments(config, sourceFiles);

        List<CompilerError> messages;

        if (config.isFork()) {
            String executable = config.getExecutable();

            if (StringUtils.isEmpty(executable)) {
                executable = "javac";
            }

            messages = compileOutOfProcess(config.getWorkingDirectory(), executable, args);
        } else {
            messages = compileInProcess(args);
        }

        return messages;
    }

    public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
        return buildCompilerArguments(config, getSourceFiles(config));
    }

    public String[] buildCompilerArguments(CompilerConfiguration config, String[] sourceFiles) {
        List<String> args = new ArrayList<String>();

        // ----------------------------------------------------------------------
        // Set output
        // ----------------------------------------------------------------------

        File destinationDir = new File(config.getOutputLocation());

        args.add("-d");

        args.add(destinationDir.getAbsolutePath());

        // ----------------------------------------------------------------------
        // Set the class and source paths
        // ----------------------------------------------------------------------

        @SuppressWarnings("unchecked")
        List<String> classpathEntries = config.getClasspathEntries();
        if (classpathEntries != null && !classpathEntries.isEmpty()) {
            args.add("-classpath");

            String cp = createClasspathArgument(classpathEntries);

            args.add(cp);
        }

        @SuppressWarnings("unchecked")
        List<String> sourceLocations = config.getSourceLocations();
        if (sourceLocations != null && !sourceLocations.isEmpty() && (sourceFiles.length == 0)) {
            args.add("-sourcepath");

            args.add(getPathString(sourceLocations));
        }

        for (int i = 0; i < sourceFiles.length; i++) {
            args.add(sourceFiles[i]);
        }

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

        // TODO: this could be much improved
        if (StringUtils.isEmpty(config.getTargetVersion())) {
            // Required, or it defaults to the target of your JDK (eg 1.5)
            args.add("-target");
            args.add("1.1");
        } else {
            args.add("-target");
            args.add(config.getTargetVersion());
        }

        if (!suppressSource(config) && StringUtils.isEmpty(config.getSourceVersion())) {
            // If omitted, later JDKs complain about a 1.1 target
            args.add("-source");
            args.add("1.3");
        } else if (!suppressSource(config)) {
            args.add("-source");
            args.add(config.getSourceVersion());
        }

        if (!suppressEncoding(config) && !StringUtils.isEmpty(config.getSourceEncoding())) {
            args.add("-encoding");
            args.add(config.getSourceEncoding());
        }

        @SuppressWarnings("unchecked")
        Map<String, String> customCompilerArguments = config.getCustomCompilerArguments();
        for (Map.Entry<String, String> entry : customCompilerArguments.entrySet()) {

            String key = (String) entry.getKey();

            if (StringUtils.isEmpty(key) || key.startsWith("@")) {
                continue;
            }

            if ("use.java.home".equals(key)) {
                this.javaHome = (String) entry.getValue();
                continue;
            }

            if ("org.osgi.framework.system.packages".equals(key)) {
                this.bootclasspathAccessRules = entry.getValue();
                continue;
            }

            args.add(key);

            String value = (String) entry.getValue();

            if (StringUtils.isEmpty(value)) {
                continue;
            }

            args.add(value);
        }

        return (String[]) args.toArray(new String[args.size()]);
    }

    private static boolean suppressSource(CompilerConfiguration config) {
        return "1.3".equals(config.getCompilerVersion());
    }

    private static boolean suppressEncoding(CompilerConfiguration config) {
        return "1.3".equals(config.getCompilerVersion());
    }

    /**
     * Compile the java sources in a external process, calling an external executable, like javac.
     * 
     * @param workingDirectory
     *            base directory where the process will be launched
     * @param executable
     *            name of the executable to launch
     * @param args
     *            arguments for the executable launched
     * @return List of CompilerError objects with the errors encountered.
     * @throws CompilerException
     */
    List<CompilerError> compileOutOfProcess(File workingDirectory, String executable, String[] args)
            throws CompilerException {
        if (true /* fork is not supported */) {
            throw new UnsupportedOperationException("compileoutOfProcess not supported");
        }

        Commandline cli = new Commandline();

        cli.setWorkingDirectory(workingDirectory.getAbsolutePath());

        cli.setExecutable(executable);

        cli.addArguments(args);

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        int returnCode;

        List<CompilerError> messages;

        try {
            returnCode = CommandLineUtils.executeCommandLine(cli, out, err);

            messages = parseModernStream(new BufferedReader(new StringReader(err.getOutput())));
        } catch (CommandLineException e) {
            throw new CompilerException("Error while executing the external compiler.", e);
        } catch (IOException e) {
            throw new CompilerException("Error while executing the external compiler.", e);
        }

        if (returnCode != 0 && messages.isEmpty()) {
            // TODO: exception?
            messages.add(new CompilerError("Failure executing javac,  but could not parse the error:" + EOL
                    + err.getOutput(), true));
        }

        return messages;
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable, using
     * <code>com.sun.tools.javac.Main</code> class
     * 
     * @param args
     *            arguments for the compiler as they would be used in the command line javac
     * @return List of CompilerError objects with the errors encountered.
     * @throws CompilerException
     */
    List<CompilerError> compileInProcess(String[] args) throws CompilerException {

        List<CompilerError> messages;

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        CompilerMain compiler = new CompilerMain(new PrintWriter(out), new PrintWriter(err), false, getLogger());
        compiler.options.put(CompilerOptions.OPTION_ReportForbiddenReference, CompilerOptions.ERROR);
        if (javaHome != null) {
            compiler.setJavaHome(new File(javaHome));
        }
        compiler.setBootclasspathAccessRules(bootclasspathAccessRules);
        compiler.compile(args);

        try {
            String output = err.toString();
            System.out.println(output);
            messages = parseModernStream(new BufferedReader(new StringReader(output)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }

    /**
     * Parse the output from the compiler into a list of CompilerError objects
     * 
     * @param input
     *            The output of the compiler
     * @return List of CompilerError objects
     * @throws IOException
     */
    protected static List<CompilerError> parseModernStream(BufferedReader input) throws IOException {
        List<CompilerError> errors = new ArrayList<CompilerError>();

        Pattern linePattern = Pattern.compile("(\\d*). (ERROR|WARNING) in (.*)");
        Pattern lineNrPattern = Pattern.compile(" \\(at line (\\d*)\\)");
        Pattern pointerPattern = Pattern.compile("(\\s*)(\\^*)");
        String line;

        StringBuilder buffer;

        int lineNr = -1;
        String file = null;
        String type = null;
        int startCol = 0, endCol = 0;

        while (true) {
            buffer = new StringBuilder(EOL);

            boolean processing = false;

            do {
                line = input.readLine();

                if (line == null) {
                    return errors;
                }

                if (!processing) {
                    Matcher matcher = linePattern.matcher(line);
                    if (processing = matcher.matches()) {
                        /* String errorNr = */matcher.group(1);
                        type = matcher.group(2);
                        file = matcher.group(3);
                    }
                } else if (!line.equals(SEPARATOR)) {
                    Matcher m;
                    if ((m = lineNrPattern.matcher(line)).matches()) {
                        lineNr = Integer.parseInt(m.group(1));
                    } else {
                        if ((m = pointerPattern.matcher(line)).matches()) {
                            startCol = m.group(1).length();
                            endCol = startCol + m.group(2).length();
                        }
                        buffer.append(line).append(EOL);
                    }
                }

            } while (!line.endsWith(SEPARATOR));

            if (processing) {
                CompilerError error = new CompilerError(file, "ERROR".equals(type), lineNr, lineNr, startCol, endCol,
                        buffer.toString());
                errors.add(error);
            }
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
    private String createClasspathArgument(List<String> classpath) {
        final String[] pathElements = (String[]) classpath.toArray(new String[classpath.size()]);

        // empty path return empty string
        if (pathElements.length == 0) {
            return "";
        }

        // no access rules, can set the path directly
        if (accessRules == null) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < pathElements.length; i++) {
                result.append(pathElements[i]);
                if (i < pathElements.length - 1) {
                    result.append(File.pathSeparatorChar);
                }
            }
            return result.toString();
        }

        int rulesLength = accessRules.size();
        String[] rules = (String[]) accessRules.toArray(new String[rulesLength]);
        int nextRule = 0;
        final StringBuffer result = new StringBuffer();

        // access rules are expected in the same order as the classpath, but
        // there could
        // be elements in the classpath not in the access rules or access rules
        // not in the classpath
        for (int i = 0, max = pathElements.length; i < max; i++) {
            if (i > 0)
                result.append(File.pathSeparatorChar);
            String pathElement = pathElements[i];
            result.append(pathElement);
            // the rules list is [path, rule, path, rule, ...]
            for (int j = nextRule; j < rulesLength; j += 2) {
                String rule = rules[j];
                if (pathElement.endsWith(rule)) {
                    result.append(rules[j + 1]);
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
                    if (pathElement
                            .regionMatches(false, pathElement.length() - ruleLength + 1, rule, 0, ruleLength - 1)) {
                        result.append(rules[j + 1]);
                        nextRule = j + 2;
                        break;
                    }
                } else if (pathElement.endsWith(File.separator)) {
                    // rule doesn't end with the File.separator, but pathElement
                    // might
                    int ruleLength = rule.length();
                    if (pathElement.regionMatches(false, pathElement.length() - ruleLength - 1, rule, 0, ruleLength)) {
                        result.append(rules[j + 1]);
                        nextRule = j + 2;
                        break;
                    }
                }
            }
        }

        String s = result.toString();
        return s;
    }

    /**
     * check the compiler arguments. Extract from files specified using @, lines marked with
     * ADAPTER_PREFIX These lines specify information that needs to be interpreted by us.
     * 
     * @param args
     *            compiler arguments to process
     */
    private void checkCompilerArgs(Map<String, String> args) {
        for (String arg : args.keySet()) {
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
                                    if (fileEncodings == null)
                                        fileEncodings = new HashMap<String, String>();
                                    // use File to translate the string into a
                                    // path with the correct File.seperator
                                    fileEncodings.put(str, enc);
                                } else {
                                    if (dirEncodings == null)
                                        dirEncodings = new HashMap<String, String>();
                                    dirEncodings.put(str, enc);
                                }
                            }
                        } else if (CharOperation.equals(ADAPTER_ACCESS, content, start, start + ADAPTER_ACCESS.length)) {
                            // access rules for the classpath
                            start += ADAPTER_ACCESS.length;
                            int accessStart = CharOperation.indexOf('[', content, start, end);
                            // CharOperation.replace(content, SEPARATOR_CHARS,
                            // File.separatorChar, start, accessStart);
                            if (start < accessStart && accessStart < end) {
                                String path = String.valueOf(content, start, accessStart - start);
                                String access = String.valueOf(content, accessStart, end - accessStart + 1);
                                if (accessRules == null)
                                    accessRules = new ArrayList<String>();
                                accessRules.add(path);
                                accessRules.add(access);
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

}
