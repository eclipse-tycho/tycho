/*******************************************************************************
 * Copyright (c) 2013, 2020 IBH SYSTEMS GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *     Obeo - Fix bug #440546
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.exec.OS;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.OsgiManifest;
import org.eclipse.tycho.core.osgitools.BundleReader;

public class JavadocRunner {
	private File output;

	private ToolchainManager toolchainManager;

	private MavenSession session;

	private Set<File> sourceFolders = Collections.<File>emptySet();

	private Set<File> manifestFiles = Collections.<File>emptySet();

	private Log log;

	private JavadocOptions options;

	private File buildDirectory;

	private BundleReader bundleReader;

	private Collection<String> classPath = Collections.<String>emptyList();

	private String lineSeparator = System.lineSeparator();

	private DocletArtifactsResolver docletArtifactsResolver;

	private PackageNameMatcher includeMatcher;

	private PackageNameMatcher excludeMatcher;

	public JavadocRunner() {
	}

	public void setBundleReader(final BundleReader bundleReader) {
		this.bundleReader = bundleReader;
	}

	public void setBuildDirectory(final File buildDirectory) {
		this.buildDirectory = buildDirectory;
	}

	public void setOptions(final JavadocOptions options) {
		this.options = options;
		if (this.options == null) {
			this.options = new JavadocOptions();
		}
	}

	public void setLog(final Log log) {
		this.log = log;
	}

	public void setSession(final MavenSession session) {
		this.session = session;
	}

	public void setOutput(final File output) {
		this.output = output;
	}

	public void run() throws Exception {
		this.output.mkdirs();
		this.buildDirectory.mkdirs();

		final File optionsFile = new File(this.buildDirectory, "javadoc.options.txt");
		final Commandline cli = createCommandLine(optionsFile.getAbsolutePath());

		try (PrintStream ps = new PrintStream(optionsFile)) {
			ps.print(createOptionsFileContent());

			this.log.info("Calling: " + cli);
			final int rc = CommandLineUtils.executeCommandLine(cli, new DefaultConsumer(), new DefaultConsumer());
			if (rc != 0) {
				if (!this.options.isIgnoreError()) {
					throw new MojoExecutionException("Failed to execute javadoc - rc = " + rc);
				} else {
					this.log.info("execution failed with rc = " + rc);
				}
			}
		}
	}

	/* VisibleForTesting */Commandline createCommandLine(String optionsFileAbsolutePath) {
		Commandline cli = new Commandline();
		cli.setExecutable(getExecutable());
		cli.setWorkingDirectory(this.output);
		cli.createArg().setValue("@" + optionsFileAbsolutePath);
		addJvmArgs(cli);
		return cli;
	}

	/* VisibleForTesting */String createOptionsFileContent() throws Exception {

		// initialize include/exclude filters
		if (options != null) {
			if (!options.getIncludes().isEmpty()) {
				includeMatcher = PackageNameMatcher.compile(options.getIncludes());
				this.log.info("Including packages matching " + includeMatcher);
			}
			if (!options.getExcludes().isEmpty()) {
				excludeMatcher = PackageNameMatcher.compile(options.getExcludes());
				this.log.info("Excluding packages matching " + excludeMatcher);
			}
		}

		StringBuilder sb = new StringBuilder();
		addSourcePaths(sb);
		addClassPath(sb);
		addDoclet(sb);
		addDocletPaths(sb);
		addEncoding(sb);
		addArguments(sb);

		final int count = addPackages(sb);
		if (count <= 0) {
			this.log.warn("No packages found");
		}

		return sb.toString();
	}

	private void addEncoding(final StringBuilder sb) {
		if (this.options.getEncoding() != null) {
			sb.append("-encoding ").append(this.options.getEncoding()).append(lineSeparator);
		}
	}

	private void addDoclet(final StringBuilder sb) {
		if (this.options.getDoclet() == null) {
			return;
		}
		sb.append("-doclet ").append(this.options.getDoclet()).append(lineSeparator);
	}

	private void addDocletPaths(final StringBuilder sb) throws MojoExecutionException {
		Set<String> resolvedArtifactJars = docletArtifactsResolver.resolveArtifacts(this.options.getDocletArtifacts());
		addPathArgument(sb, "-docletpath", resolvedArtifactJars);
	}

	private void addClassPath(final StringBuilder sb) {
		addPathArgument(sb, "-classpath", this.classPath);
	}

	private void addArguments(final StringBuilder sb) {
		for (final String argument : this.options.getAdditionalArguments()) {
			sb.append(argument).append(lineSeparator);
		}
	}

	private void addJvmArgs(final Commandline cli) {

		for (final String arg : this.options.getJvmOptions()) {
			cli.createArg().setValue("-J" + arg);
		}
	}

	private int addPackages(final StringBuilder sb) throws Exception {
		int count = 0;

		for (final File manifestFile : this.manifestFiles) {
			if (!manifestFile.canRead()) {
				this.log.debug("No readable manifest: " + manifestFile);
				continue;
			}

			final OsgiManifest bundle = this.bundleReader.loadManifest(manifestFile);
			count += addPackages(sb, bundle.getManifestElements("Export-Package"));
		}
		return count;
	}

	private int addPackages(final StringBuilder sb, final ManifestElement[] manifestElements) {
		if (manifestElements == null) {
			return 0;
		}

		for (final ManifestElement ele : manifestElements) {
			final String pkg = ele.getValue();

			final boolean include = includeMatcher != null ? includeMatcher.matches(pkg) : true;
			final boolean exclude = excludeMatcher != null ? excludeMatcher.matches(pkg) : false;

			if (include && !exclude) {
				sb.append(pkg).append(lineSeparator);
			}
		}

		return manifestElements.length;
	}

	private void addPath(final StringBuilder sb, final Collection<?> path) {
		boolean first = true;
		for (final Object ele : path) {
			if (ele == null) {
				continue;
			}
			// convert black slashes to forward slashes for javadoc
			final String pathEle = ele.toString().replace('\\', '/');
			if (!first) {
				sb.append(File.pathSeparator);
			} else {
				first = false;
			}
			sb.append(pathEle);
		}
	}

	private void addSourcePaths(final StringBuilder sb) {
		addPathArgument(sb, "-sourcepath", this.sourceFolders);
	}

	private void addPathArgument(final StringBuilder sb, final String arg, final Collection<?> path) {
		if (path.isEmpty()) {
			return;
		}
		sb.append(arg);
		sb.append(" '");
		addPath(sb, path);
		sb.append("'" + lineSeparator);
	}

	protected String getExecutable() {
		log.debug("Find javadoc executable");

		if (this.options.getExecutable() != null) {
			// prefer the specific one
			log.debug("Using specified javadoc: " + options.getExecutable());
			return this.options.getExecutable();
		}

		log.debug("Toolchain manager: " + toolchainManager);

		if (this.toolchainManager != null) {
			// try the toolchain
			final Toolchain tc = this.toolchainManager.getToolchainFromBuildContext("jdk", this.session);
			log.debug("Toolchain: " + tc);

			if (tc != null) {
				final String exe = tc.findTool("javadoc");
				log.debug("Toolchain Tool: " + exe);
				if (exe != null) {
					return exe;
				}
			}
		}

		String javaHome = System.getProperty("java.home");
		String javadocFromJavaHome;

		// derive path to javac from java.home similar to
		// org.codehaus.plexus.compiler.javac.JavacCompiler.getJavacExecutable() in
		// plexus-compiler-javac
		if (OS.isFamilyMac()) {
			javadocFromJavaHome = javaHome + File.separator + "bin" + File.separator + "javadoc";
		} else {
			javadocFromJavaHome = javaHome + File.separator + ".." + File.separator + "bin" + File.separator
					+ "javadoc";
		}

		if (OS.isFamilyWindows()) {
			javadocFromJavaHome += ".exe";
		}

		log.debug("Testing javadoc from java.home = " + javadocFromJavaHome);

		if (new File(javadocFromJavaHome).canExecute()) {
			return javadocFromJavaHome;
		}

		// use JAVA_HOME
		javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null && !javaHome.isBlank()) {

			javadocFromJavaHome = javaHome + File.separator + "bin" + File.separator + "javadoc";
			if (OS.isFamilyWindows()) {
				javadocFromJavaHome += ".exe";
			}

			log.debug("Testing javadoc from JAVA_HOME = " + javadocFromJavaHome);

			if (new File(javadocFromJavaHome).canExecute()) {
				return javadocFromJavaHome;
			}
		}

		log.debug("Using path fallback");

		// fall back
		return "javadoc";
	}

	public void setToolchainManager(final ToolchainManager toolchainManager) {
		this.toolchainManager = toolchainManager;
	}

	public void setSourceFolders(final Set<File> sourceFolders) {
		this.sourceFolders = sourceFolders;
	}

	public void setClassPath(final Collection<String> classPath) {
		this.classPath = classPath;
	}

	public void setManifestFiles(Set<File> manifestFiles) {
		this.manifestFiles = manifestFiles;
	}

	public void setDocletArtifactsResolver(DocletArtifactsResolver docletArtifactsResolver) {
		this.docletArtifactsResolver = docletArtifactsResolver;
	}
}
