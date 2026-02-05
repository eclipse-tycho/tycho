/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.baseline.analyze.CheckContext;
import org.eclipse.tycho.baseline.analyze.ClassUsage;
import org.eclipse.tycho.baseline.analyze.DependencyAnalyzer;
import org.eclipse.tycho.baseline.analyze.DependencyVersionProblem;
import org.eclipse.tycho.baseline.analyze.ImportPackageChecker;
import org.eclipse.tycho.baseline.analyze.JrtClasses;
import org.eclipse.tycho.baseline.analyze.MethodSignature;
import org.eclipse.tycho.baseline.analyze.RequireBundleChecker;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.OSGiJavaToolchain;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;

/**
 * This mojos performs deep inspections of dependencies to find out if a version
 * range is actually valid. For this the following steps are performed:
 * <ol>
 * <li>The current project artifact is analyzed for method signatures it
 * calls</li>
 * <li>Then it is checked what of these match to a given dependency</li>
 * <li>All dependency versions matching the range are fetched and inspected
 * using {@link ArtifactVersionProvider}s</li>
 * <li>Then it checks if there are any missing signatures or inconsistencies and
 * possibly failing the build</li>
 * </ol>
 */
@Mojo(defaultPhase = LifecyclePhase.VERIFY, name = "check-dependencies", threadSafe = true, requiresProject = true)
public class DependencyCheckMojo extends AbstractMojo {

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Parameter(property = "session", readonly = true)
	private MavenSession session;

	@Parameter(defaultValue = "${project.build.directory}/versionProblems.md", property = "tycho.dependency.check.report")
	private File reportFileName;

	@Parameter(defaultValue = "${project.basedir}/META-INF/MANIFEST.MF", property = "tycho.dependency.check.manifest")
	private File manifestFile;

	@Parameter(defaultValue = "false", property = "tycho.dependency.check.apply")
	private boolean applySuggestions;

	/**
	 * If <code>true</code> skips the check.
	 */
	@Parameter(property = "tycho.dependency.check.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * If <code>true</code> print additional verbose messages.
	 */
	@Parameter(property = "tycho.dependency.check.verbose", defaultValue = "false")
	private boolean verbose;

	@Component
	private TychoProjectManager projectManager;

	@Component
	private List<ArtifactVersionProvider> versionProvider;

	@Component
	private BundleReader bundleReader;

	@Component
	ToolchainProvider toolchainProvider;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		if (!"jar".equals(project.getPackaging())
				&& !PackagingType.TYPE_ECLIPSE_PLUGIN.equals(project.getPackaging())) {
			return;
		}
		DependencyArtifacts artifacts = projectManager.getDependencyArtifacts(project).orElse(null);
		File file = project.getArtifact().getFile();
		if (file == null || !file.isFile()) {
			throw new MojoFailureException("Project artifact is not a valid file");
		}
		JrtClasses jrtClassResolver = getJRTClassResolver();
		List<ClassUsage> usages;
		try {
			usages = DependencyAnalyzer.analyzeUsage(file, jrtClassResolver);
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		if (usages.isEmpty()) {
			return;
		}
		Collection<IInstallableUnit> units = artifacts.getInstallableUnits();
		ModuleRevisionBuilder builder = readOSGiInfo(file);
		List<GenericInfo> requirements = builder.getRequirements();
		Log log = getLog();
		DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(jrtClassResolver, (m, e) -> getLog().error(m, e));
		// Create the shared check context
		CheckContext context = new CheckContext(dependencyAnalyzer,artifacts,versionProvider, project, log, verbose);

		// Create checkers that maintain their own state
		ImportPackageChecker importPackageChecker = new ImportPackageChecker(context, units, usages);
		RequireBundleChecker requireBundleChecker = new RequireBundleChecker(context, units, usages);

		for (GenericInfo genericInfo : requirements) {
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(genericInfo.getNamespace())) {
				Map<String, String> pkgInfo = getVersionInfo(genericInfo,
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				String packageVersion = pkgInfo.getOrDefault(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0");
				String packageName = pkgInfo.get(PackageNamespace.PACKAGE_NAMESPACE);
				importPackageChecker.check(packageName, packageVersion);
			} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(genericInfo.getNamespace())) {
				Map<String, String> bundleInfo = getVersionInfo(genericInfo,
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
				String bundleVersionStr = bundleInfo.getOrDefault(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
						"0.0.0");
				String bundleName = bundleInfo.get(BundleNamespace.BUNDLE_NAMESPACE);
				requireBundleChecker.check(bundleName, bundleVersionStr);
			}
		}
		List<DependencyVersionProblem> dependencyProblems = context.getProblems();
		if (dependencyProblems.isEmpty()) {
			return;
		}
		if (applySuggestions) {
			try {
				MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
				boolean changed = importPackageChecker.applySuggestions(manifest);
				changed |= requireBundleChecker.applySuggestions(manifest);
				if (changed) {
					MutableBundleManifest.write(manifest, manifestFile);
				}
			} catch (IOException ioe) {
				throw new MojoFailureException(ioe);
			}
		}
		MarkdownBuilder results = new MarkdownBuilder(reportFileName);
		Set<String> keys = new HashSet<>();
		for (DependencyVersionProblem problem : dependencyProblems) {
			if (!verbose && !keys.add(problem.key())) {
				// we have already reported one problem in this category
				continue;
			}
			Collection<String> references = problem.references();
			String message;
			if (references == null || references.isEmpty()) {
				message = problem.message();
			} else {
				if (verbose) {
					String delimiter = System.lineSeparator() + "- ";
					message = String.format("%s referenced by:%s%s ", problem.message(), delimiter,
							problem.references().stream().collect(Collectors.joining(delimiter)));
				} else {
					int size = references.size();
					if (size == 1) {
						message = String.format("%s referenced by `%s`.", problem.message(),
								references.iterator().next());
					} else {
						message = String.format("%s referenced by `%s` and %d other.", problem.message(),
								references.iterator().next(), size - 1);
					}
				}
			}
			log.error(message);
			results.add(message);
			if (verbose) {
				List<MethodSignature> provided = problem.provided();
				if (provided != null && !provided.isEmpty()) {
					results.add("Provided Methods in this version are:");
					for (MethodSignature sig : provided) {
						results.add("\t" + sig.id());
					}
				}
			}
			results.add("");
		}
		importPackageChecker.reportSuggestions(results, log);
		requireBundleChecker.reportSuggestions(results, log);
		results.write();
	}

	private Map<String, String> getVersionInfo(GenericInfo genericInfo, String versionAttribute) {
		Map<String, String> directives = new HashMap<>(genericInfo.getDirectives());
		String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		FilterImpl filterImpl;
		try {
			filterImpl = FilterImpl.newInstance(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		return filterImpl.getStandardOSGiAttributes(versionAttribute);
	}

	private ModuleRevisionBuilder readOSGiInfo(File file) throws MojoFailureException {
		OsgiManifest manifest = bundleReader.loadManifest(file);
		ModuleRevisionBuilder builder;
		try {
			builder = OSGiManifestBuilderFactory.createBuilder(manifest.getHeaders());
		} catch (BundleException e) {
			throw new MojoFailureException(e);
		}
		return builder;
	}

	private JrtClasses getJRTClassResolver() {
		String profileName = projectManager.getExecutionEnvironments(project, session).findFirst()
				.map(ee -> ee.getProfileName()).orElse(null);
		if (profileName != null) {
			OSGiJavaToolchain osgiToolchain = toolchainProvider.getToolchain(profileName).orElse(null);
			if (osgiToolchain != null) {
				return new JrtClasses(osgiToolchain.getJavaHome());
			}
		}
		// use running jvm
		return new JrtClasses(null);
	}
}
