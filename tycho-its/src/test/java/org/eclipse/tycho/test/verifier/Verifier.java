/*******************************************************************************
 * Copyright (c) 2008, 2026 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.executor.ExecutorException;
import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.ExecutorResult;
import org.apache.maven.executor.forked.ForkedMavenExecutor;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;

/**
 * A drop-in, Tycho-scoped replacement for the retired {@code org.apache.maven.it.Verifier}
 * (maven-verifier 1.8.0), reimplemented on top of {@code maven-executor}.
 * <p>
 * Unlike the original, this class only ever forks the build in a separate Maven process (there is
 * no embedded launcher): every Tycho integration test already runs with {@code forkJvm=true}, so
 * embedded execution was never exercised and is intentionally not reimplemented.
 */
public class Verifier {

	private static final String DEFAULT_LOG_FILENAME = "log.txt";

	private final String basedir;

	private final String mavenHome;

	private String localRepo;

	private String logFileName = DEFAULT_LOG_FILENAME;

	private boolean autoclean = true;

	private final List<String> cliOptions = new ArrayList<>();

	private final Properties systemProperties = new Properties();

	private final Map<String, String> environmentVariables = new LinkedHashMap<>();

	private final Properties verifierProperties = new Properties();

	private String lastStdOut = "";

	private String lastStdErr = "";

	public Verifier(String basedir) throws VerificationException {
		this.basedir = basedir;
		this.mavenHome = System.getProperty("maven.home");
		if (mavenHome == null || mavenHome.isBlank()) {
			throw new VerificationException(
					"System property 'maven.home' is not set; it must point at the Maven installation"
							+ " that ForkedMavenExecutor is to launch.");
		}
		this.localRepo = findLocalRepo();
	}

	// ------------------------------------------------------------------
	// basedir / local repository
	// ------------------------------------------------------------------

	public String getBasedir() {
		return basedir;
	}

	public void setLocalRepo(String localRepo) {
		this.localRepo = localRepo;
	}

	public String getLocalRepository() {
		return localRepo;
	}

	private static String findLocalRepo() {
		String repo = System.getProperty("maven.repo.local");
		if (repo == null) {
			repo = retrieveLocalRepoFromSettings();
		}
		if (repo == null) {
			repo = System.getProperty("user.home") + "/.m2/repository";
		}
		File repoDir = new File(repo);
		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}
		return repoDir.getAbsolutePath();
	}

	private static String retrieveLocalRepoFromSettings() {
		File userXml = new File(System.getProperty("user.home"), ".m2/settings.xml");
		if (!userXml.isFile()) {
			return null;
		}
		try {
			String content = Files.readString(userXml.toPath(), StandardCharsets.UTF_8);
			Matcher matcher = Pattern.compile("<localRepository>\\s*([^<]*?)\\s*</localRepository>").matcher(content);
			if (matcher.find()) {
				String value = matcher.group(1).trim();
				if (!value.isEmpty()) {
					return new File(value).getAbsolutePath();
				}
			}
		} catch (IOException e) {
			// fall through to the next default
		}
		return null;
	}

	// ------------------------------------------------------------------
	// CLI options / system properties / environment / verifier properties
	// ------------------------------------------------------------------

	public List<String> getCliOptions() {
		return cliOptions;
	}

	public void addCliOption(String option) {
		cliOptions.add(option);
	}

	public Properties getSystemProperties() {
		return systemProperties;
	}

	public void setSystemProperty(String key, String value) {
		if (value != null) {
			systemProperties.setProperty(key, value);
		} else {
			systemProperties.remove(key);
		}
	}

	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariable(String key, String value) {
		if (value != null) {
			environmentVariables.put(key, value);
		} else {
			environmentVariables.remove(key);
		}
	}

	public Properties getVerifierProperties() {
		return verifierProperties;
	}

	public void setAutoclean(boolean autoclean) {
		this.autoclean = autoclean;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public void setLogFileName(String logFileName) {
		if (logFileName == null || logFileName.isEmpty()) {
			throw new IllegalArgumentException("log file name unspecified");
		}
		this.logFileName = logFileName;
	}

	/**
	 * Tycho integration tests always fork (see {@code AbstractTychoIntegrationTest#isForked()});
	 * embedded execution was never exercised in this test suite and is not implemented here.
	 */
	public void setForkJvm(boolean forkJvm) {
		if (!forkJvm) {
			throw new UnsupportedOperationException(
					"Embedded (non-forked) execution is not supported by this Verifier; Tycho ITs always fork.");
		}
	}

	/**
	 * No-op: unlike the original maven-verifier, this implementation never redirects
	 * {@code System.out}/{@code System.err} (forked build output is captured by the executor and
	 * written to the log file directly), so there is nothing to reset.
	 */
	public void resetStreams() {
		// intentionally empty
	}

	/** Prints the standard output and error of the last run, as maven-verifier 1.8.0 did. */
	public void displayStreamBuffers() {
		if (!lastStdOut.isBlank()) {
			System.out.println("----- Standard Out -----");
			System.out.println(lastStdOut);
		}
		if (!lastStdErr.isBlank()) {
			System.out.println("----- Standard Error -----");
			System.out.println(lastStdErr);
		}
	}

	// ------------------------------------------------------------------
	// build execution
	// ------------------------------------------------------------------

	public void executeGoal(String goal) throws VerificationException {
		executeGoal(goal, environmentVariables);
	}

	public void executeGoal(String goal, Map<String, String> envVars) throws VerificationException {
		executeGoals(List.of(goal), envVars);
	}

	public void executeGoals(List<String> goals) throws VerificationException {
		executeGoals(goals, environmentVariables);
	}

	/**
	 * Runs the goals with {@code envVars} as the environment for this run, used instead of the
	 * verifier's own variables, as in maven-verifier 1.8.0.
	 */
	public void executeGoals(List<String> goals, Map<String, String> envVars) throws VerificationException {
		List<String> arguments = buildArguments(goals);

		Map<String, String> env = new LinkedHashMap<>();
		env.put("M2_HOME", mavenHome);
		if (envVars != null) {
			env.putAll(envVars);
		}
		env.putIfAbsent("JAVA_HOME", System.getProperty("java.home"));
		env.put("MAVEN_TERMINATE_CMD", "on");

		File logFile = new File(basedir, logFileName);

		try (ForkedMavenExecutor executor = new ForkedMavenExecutor(Path.of(mavenHome))) {
			ExecutorRequest request = ExecutorRequest.mavenBuilder().cwd(Path.of(basedir)).arguments(arguments)
					.environmentVariables(env).grabOutputAsString(true).build();

			ExecutorResult result = executor.execute(request);

			lastStdOut = result.stdOutString().orElse("");
			lastStdErr = result.stdErrString().orElse("");
			String log = lastStdOut + lastStdErr;
			Files.writeString(logFile.toPath(), log, StandardCharsets.UTF_8);

			if (!result.success()) {
				throw new VerificationException("Exit code was non-zero: "
						+ result.exitCode().map(String::valueOf).orElse("unknown") + "; command line and log = \n"
						+ mavenHome + "/bin/mvn " + String.join(" ", arguments) + "\n" + log);
			}
		} catch (ExecutorException e) {
			throw new VerificationException("Failed to execute Maven: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new VerificationException(e);
		}
	}

	/**
	 * Builds the full Maven CLI argument list for the given goals, in the same order the retired
	 * maven-verifier 1.8.0 launcher used: system properties first, then the (${basedir}-resolved
	 * and shell-tokenized) CLI options, then the default options, then the local-repo pin, and
	 * finally the goals (with the clean-plugin prepended for autoclean).
	 * <p>
	 * Package-private so the argument-building unit test can exercise it without launching Maven.
	 */
	List<String> buildArguments(List<String> goals) throws VerificationException {
		List<String> args = new ArrayList<>();

		for (String key : systemProperties.stringPropertyNames()) {
			args.add("-D" + key + "=" + systemProperties.getProperty(key));
		}

		for (String option : cliOptions) {
			String resolved = resolveCommandLineArg(option);
			for (String token : translateCommandline(resolved)) {
				args.add(token);
			}
		}

		args.add("-e");
		args.add("--batch-mode");

		boolean useMavenRepoLocal = Boolean.parseBoolean(verifierProperties.getProperty("use.mavenRepoLocal", "true"));
		if (useMavenRepoLocal) {
			args.add("-Dmaven.repo.local=" + localRepo);
		}

		if (autoclean) {
			args.add("org.apache.maven.plugins:maven-clean-plugin:clean");
		}
		args.addAll(goals);

		return args;
	}

	/**
	 * Substitutes {@code ${basedir}} and collapses doubled slashes, exactly like maven-verifier
	 * 1.8.0's {@code resolveCommandLineArg}. The slash-collapse is load-bearing: several Tycho ITs
	 * write CLI options such as {@code -De342-repo=https:////download.eclipse.org/...} specifically
	 * to survive this single collapsing pass with the intended {@code https://} URL intact.
	 * <p>
	 * The original also tried to collapse doubled backslashes via a regex whose replacement string
	 * is invalid ({@code replaceAll("\\\\\\\\", "\\\\")}) and would throw at runtime if it ever
	 * matched; since Tycho's basedir-derived arguments never contain doubled backslashes on the
	 * platforms this suite runs on, that (dead, buggy) step is intentionally not reproduced.
	 */
	String resolveCommandLineArg(String key) {
		String result = key.replace("${basedir}", basedir);
		result = result.replace("//", "/");
		return result;
	}

	/**
	 * Reimplementation of
	 * {@code org.apache.maven.shared.utils.cli.CommandLineUtils#translateCommandline(String)}
	 * (verified by decompiling maven-shared-utils 3.4.2): splits on unquoted whitespace, honours
	 * single/double quotes, and treats {@code \\} as an escape for the next character. Written out
	 * locally so that tycho-its does not need to (re-)add maven-shared-utils as a dependency.
	 */
	static String[] translateCommandline(String toProcess) throws VerificationException {
		if (toProcess == null || toProcess.isEmpty()) {
			return new String[0];
		}

		final int NORMAL = 0;
		final int IN_SINGLE_QUOTE = 1;
		final int IN_DOUBLE_QUOTE = 2;

		StringTokenizer tokenizer = new StringTokenizer(toProcess, "\"' \\", true);
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean escaped = false;
		int state = NORMAL;

		while (tokenizer.hasMoreTokens()) {
			String nextTok = tokenizer.nextToken();
			switch (state) {
			case IN_SINGLE_QUOTE:
				if ("'".equals(nextTok)) {
					if (escaped) {
						current.append(nextTok);
						escaped = false;
					} else {
						state = NORMAL;
					}
				} else {
					current.append(nextTok);
					escaped = "\\".equals(nextTok);
				}
				break;
			case IN_DOUBLE_QUOTE:
				if ("\"".equals(nextTok)) {
					if (escaped) {
						current.append(nextTok);
						escaped = false;
					} else {
						state = NORMAL;
					}
				} else {
					current.append(nextTok);
					escaped = "\\".equals(nextTok);
				}
				break;
			default:
				if ("'".equals(nextTok)) {
					if (escaped) {
						escaped = false;
						current.append(nextTok);
					} else {
						state = IN_SINGLE_QUOTE;
					}
				} else if ("\"".equals(nextTok)) {
					if (escaped) {
						escaped = false;
						current.append(nextTok);
					} else {
						state = IN_DOUBLE_QUOTE;
					}
				} else if (" ".equals(nextTok)) {
					if (current.length() != 0) {
						tokens.add(current.toString());
						current.setLength(0);
					}
				} else {
					current.append(nextTok);
					escaped = "\\".equals(nextTok);
				}
			}
		}

		if (current.length() != 0) {
			tokens.add(current.toString());
		}

		if (state == IN_SINGLE_QUOTE || state == IN_DOUBLE_QUOTE) {
			throw new VerificationException("unbalanced quotes in " + toProcess);
		}

		return tokens.toArray(new String[0]);
	}

	// ------------------------------------------------------------------
	// log file assertions
	// ------------------------------------------------------------------

	public void verifyErrorFreeLog() throws VerificationException {
		List<String> lines = loadFile(basedir, logFileName, false);
		for (String line : lines) {
			if (stripAnsi(line).contains("[ERROR]") && !isVelocityError(line)) {
				throw new VerificationException("Error in execution: " + line);
			}
		}
	}

	public void verifyTextInLog(String text) throws VerificationException {
		List<String> lines = loadFile(basedir, logFileName, false);
		for (String line : lines) {
			if (stripAnsi(line).contains(text)) {
				return;
			}
		}
		throw new VerificationException("Text not found in log: " + text);
	}

	private static boolean isVelocityError(String line) {
		return line.contains("VM_global_library.vm") || (line.contains("VM #") && line.contains("macro"));
	}

	public static String stripAnsi(String msg) {
		return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
	}

	/**
	 * Loads the non-empty, non-comment lines of a text file relative to {@code basedir}.
	 * <p>
	 * Unlike maven-verifier 1.8.0, this does not expand {@code ${artifact:g:a:v:e}} markers: Tycho
	 * only ever calls this to read back the build log, which never contains them.
	 */
	public List<String> loadFile(String basedir, String filename, boolean hasCommand) throws VerificationException {
		File file = new File(basedir, filename);
		List<String> lines = new ArrayList<>();
		if (file.exists()) {
			try {
				for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
					String trimmed = line.trim();
					if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
						lines.add(trimmed);
					}
				}
			} catch (IOException e) {
				throw new VerificationException(e);
			}
		}
		return lines;
	}

	// ------------------------------------------------------------------
	// file / artifact assertions
	// ------------------------------------------------------------------

	public void verifyFilePresent(String file) throws VerificationException {
		verifyFilePresence(file, true);
	}

	public void verifyFileNotPresent(String file) throws VerificationException {
		verifyFilePresence(file, false);
	}

	private void verifyFilePresence(String filePath, boolean wanted) throws VerificationException {
		File expectedFile = new File(filePath);
		if (!expectedFile.isAbsolute()) {
			expectedFile = new File(basedir, filePath);
		}
		boolean exists = expectedFile.exists();
		if (wanted && !exists) {
			throw new VerificationException("Expected file was not found: " + expectedFile.getPath());
		}
		if (!wanted && exists) {
			throw new VerificationException("Unwanted file was found: " + expectedFile.getPath());
		}
	}

	public String getArtifactPath(String groupId, String artifactId, String version, String ext) {
		return getArtifactPath(groupId, artifactId, version, ext, null);
	}

	private String getArtifactPath(String groupId, String artifactId, String version, String ext,
			String classifier) {
		if (classifier != null && classifier.isEmpty()) {
			classifier = null;
		}
		if ("maven-plugin".equals(ext)) {
			ext = "jar";
		} else if ("test-jar".equals(ext)) {
			ext = "jar";
			classifier = "tests";
		}
		String path = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;
		if (classifier != null) {
			path += "-" + classifier;
		}
		path += "." + ext;
		return localRepo + "/" + path;
	}

	private List<String> getArtifactFileNameList(String groupId, String artifactId, String version, String ext) {
		List<String> files = new ArrayList<>();
		String artifactPath = getArtifactPath(groupId, artifactId, version, ext);
		files.add(artifactPath);
		File dir = new File(artifactPath).getParentFile();
		addMetadataFiles(dir, files);
		if (dir != null) {
			addMetadataFiles(dir.getParentFile(), files);
		}
		return files;
	}

	private static void addMetadataFiles(File dir, List<String> files) {
		if (dir != null && dir.isDirectory()) {
			String[] names = dir.list((d, name) -> name.startsWith("maven-metadata") && name.endsWith(".xml"));
			if (names != null) {
				for (String name : names) {
					files.add(new File(dir, name).getPath());
				}
			}
		}
	}

	public void deleteArtifact(String groupId, String artifactId, String version, String ext) throws IOException {
		for (String file : getArtifactFileNameList(groupId, artifactId, version, ext)) {
			FileUtils.forceDelete(new File(file));
		}
	}

	public void deleteArtifacts(String groupId, String artifactId, String version) throws IOException {
		String path = groupId.replace('.', '/') + '/' + artifactId + '/' + version;
		FileUtils.deleteDirectory(new File(localRepo, path));
	}

	public void verifyArtifactPresent(String groupId, String artifactId, String version, String ext)
			throws VerificationException {
		for (String file : getArtifactFileNameList(groupId, artifactId, version, ext)) {
			verifyFilePresence(file, true);
		}
	}

	public void assertArtifactContents(String groupId, String artifactId, String version, String ext,
			String contents) throws IOException {
		String fileName = getArtifactPath(groupId, artifactId, version, ext);
		Assert.assertEquals(contents, FileUtils.fileRead(fileName));
	}

}
