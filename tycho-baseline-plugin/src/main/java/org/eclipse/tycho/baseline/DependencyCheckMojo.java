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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
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
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.baseline.analyze.ClassCollection;
import org.eclipse.tycho.baseline.analyze.ClassMethods;
import org.eclipse.tycho.baseline.analyze.ClassUsage;
import org.eclipse.tycho.baseline.analyze.DependencyAnalyzer;
import org.eclipse.tycho.baseline.analyze.JrtClasses;
import org.eclipse.tycho.baseline.analyze.MethodSignature;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.OSGiJavaToolchain;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
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
		List<ClassUsage> usages = DependencyAnalyzer.analyzeUsage(file, jrtClassResolver);
		if (usages.isEmpty()) {
			return;
		}
		Collection<IInstallableUnit> units = artifacts.getInstallableUnits();
		ModuleRevisionBuilder builder = readOSGiInfo(file);
		List<GenericInfo> requirements = builder.getRequirements();
		List<DependencyVersionProblem> dependencyProblems = new ArrayList<>();
		Map<Path, ClassCollection> analyzeCache = new HashMap<>();
		Log log = getLog();
		Map<String, Version> lowestPackageVersion = new HashMap<>();
		Map<String, Set<Version>> allPackageVersion = new HashMap<>();
		Set<String> packageWithError = new HashSet<>();
		Map<String, Version> lowestBundleVersion = new HashMap<>();
		Map<String, Set<Version>> allBundleVersion = new HashMap<>();
		Set<String> bundleWithError = new HashSet<>();
		DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer((m, e) -> getLog().error(m, e));
		Function<String, Optional<ClassMethods>> classResolver = dependencyAnalyzer
				.createDependencyClassResolver(jrtClassResolver, artifacts);
		for (GenericInfo genericInfo : requirements) {
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(genericInfo.getNamespace())) {
				checkImportPackage(genericInfo, units, usages, analyzeCache, dependencyProblems, dependencyAnalyzer,
						classResolver, lowestPackageVersion, allPackageVersion, packageWithError, log);
			} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(genericInfo.getNamespace())) {
				checkRequireBundle(genericInfo, units, usages, analyzeCache, dependencyProblems, dependencyAnalyzer,
						classResolver, lowestBundleVersion, allBundleVersion, bundleWithError, log);
			}
		}
		if (dependencyProblems.isEmpty()) {
			return;
		}
		if (applySuggestions) {
			applyLowerBounds(packageWithError, lowestPackageVersion, bundleWithError, lowestBundleVersion);
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
		if (!packageWithError.isEmpty()) {
			results.add("");
			for (String pkg : packageWithError) {
				String suggestion = String.format("Suggested lower version for package `%s` is `%s`", pkg,
						lowestPackageVersion.get(pkg));
				Set<Version> all = allPackageVersion.get(pkg);
				if (all != null && !all.isEmpty()) {
					suggestion += " out of " + all.stream().map(v -> String.format("`%s`", v))
							.collect(Collectors.joining(", ", "[", "]"));
				}
				log.info(suggestion);
				results.add(suggestion);
			}
		}
		if (!bundleWithError.isEmpty()) {
			results.add("");
			for (String bundle : bundleWithError) {
				String suggestion = String.format("Suggested lower version for bundle `%s` is `%s`", bundle,
						lowestBundleVersion.get(bundle));
				Set<Version> all = allBundleVersion.get(bundle);
				if (all != null && !all.isEmpty()) {
					suggestion += " out of " + all.stream().map(v -> String.format("`%s`", v))
							.collect(Collectors.joining(", ", "[", "]"));
				}
				log.info(suggestion);
				results.add(suggestion);
			}
		}
		results.write();
	}

	private void checkImportPackage(GenericInfo genericInfo, Collection<IInstallableUnit> units,
			List<ClassUsage> usages, Map<Path, ClassCollection> analyzeCache,
			List<DependencyVersionProblem> dependencyProblems, DependencyAnalyzer dependencyAnalyzer,
			Function<String, Optional<ClassMethods>> classResolver, Map<String, Version> lowestPackageVersion,
			Map<String, Set<Version>> allPackageVersion, Set<String> packageWithError, Log log)
			throws MojoFailureException {
		Map<String, String> pkgInfo = getVersionInfo(genericInfo, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		String packageVersion = pkgInfo.getOrDefault(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0");
		String packageName = pkgInfo.get(PackageNamespace.PACKAGE_NAMESPACE);
		Optional<IInstallableUnit> packageProvidingUnit = ArtifactMatcher.findPackage(packageName, units);
		if (packageProvidingUnit.isEmpty()) {
			return;
		}
		IInstallableUnit unit = packageProvidingUnit.get();
		Optional<org.eclipse.equinox.p2.metadata.Version> matchedPackageVersion = ArtifactMatcher
				.getPackageVersion(unit, packageName);
		if (matchedPackageVersion.isEmpty()
				|| matchedPackageVersion.get().equals(org.eclipse.equinox.p2.metadata.Version.emptyVersion)) {
			log.warn("Package " + packageName + " has no version exported and can not be checked for compatibility");
			return;
		}
		matchedPackageVersion.filter(v -> v.isOSGiCompatible()).ifPresent(v -> {
			Version current = new Version(v.toString());
			allPackageVersion.computeIfAbsent(packageName, nil -> new TreeSet<>()).add(current);
			lowestPackageVersion.put(packageName, current);
		});
		VersionRange versionRange = VersionRange.valueOf(packageVersion);
		List<ArtifactVersion> list = versionProvider.stream()
				.flatMap(avp -> avp.getPackageVersions(unit, packageName, versionRange, project)).toList();
		if (log.isDebugEnabled()) {
			log.debug("== " + packageName + " " + packageVersion + " is provided by " + unit + " with version range "
					+ versionRange + ", matching versions: "
					+ list.stream().map(av -> av.getVersion()).map(String::valueOf).collect(Collectors.joining(", ")));
		}
		Set<MethodSignature> packageMethods = collectMethodsForPackage(usages, packageName);
		Map<MethodSignature, Collection<String>> references = collectReferencesForPackage(usages, packageName);
		if (packageMethods.isEmpty()) {
			return;
		}
		if (log.isDebugEnabled()) {
			for (MethodSignature signature : packageMethods) {
				log.debug("Referenced: " + signature.id());
			}
		}
		for (ArtifactVersion v : list) {
			Version version = v.getVersion();
			if (version == null) {
				continue;
			}
			if (!allPackageVersion.computeIfAbsent(packageName, nil -> new TreeSet<>()).add(version)) {
				continue;
			}
			Path artifact = v.getArtifact();
			log.debug(v + "=" + artifact);
			if (artifact == null) {
				continue;
			}
			ClassCollection collection = analyzeCache.get(artifact);
			if (collection == null) {
				collection = dependencyAnalyzer.analyzeProvides(artifact.toFile(), classResolver);
				analyzeCache.put(artifact, collection);
			}
			boolean ok = checkMethodsInCollection(collection, packageMethods, packageName, version, references,
					dependencyProblems, packageWithError, v, unit, packageVersion, matchedPackageVersion, log,
					"Import-Package");
			if (ok) {
				lowestPackageVersion.merge(packageName, version, (v1, v2) -> v1.compareTo(v2) > 0 ? v2 : v1);
			}
		}
	}

	private void checkRequireBundle(GenericInfo genericInfo, Collection<IInstallableUnit> units,
			List<ClassUsage> usages, Map<Path, ClassCollection> analyzeCache,
			List<DependencyVersionProblem> dependencyProblems, DependencyAnalyzer dependencyAnalyzer,
			Function<String, Optional<ClassMethods>> classResolver, Map<String, Version> lowestBundleVersion,
			Map<String, Set<Version>> allBundleVersion, Set<String> bundleWithError, Log log)
			throws MojoFailureException {
		Map<String, String> bundleInfo = getVersionInfo(genericInfo, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		String bundleVersionStr = bundleInfo.getOrDefault(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, "0.0.0");
		String bundleName = bundleInfo.get(BundleNamespace.BUNDLE_NAMESPACE);
		Optional<IInstallableUnit> bundleProvidingUnit = ArtifactMatcher.findBundle(bundleName, units);
		if (bundleProvidingUnit.isEmpty()) {
			return;
		}
		IInstallableUnit unit = bundleProvidingUnit.get();
		org.eclipse.equinox.p2.metadata.Version matchedBundleVersion = unit.getVersion();
		if (matchedBundleVersion.equals(org.eclipse.equinox.p2.metadata.Version.emptyVersion)) {
			log.warn("Bundle " + bundleName + " has no version and can not be checked for compatibility");
			return;
		}
		if (matchedBundleVersion.isOSGiCompatible()) {
			Version current = new Version(matchedBundleVersion.toString());
			allBundleVersion.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(current);
			lowestBundleVersion.put(bundleName, current);
		}
		VersionRange versionRange = VersionRange.valueOf(bundleVersionStr);
		List<ArtifactVersion> list = versionProvider.stream()
				.flatMap(avp -> avp.getBundleVersions(unit, bundleName, versionRange, project)).toList();
		if (log.isDebugEnabled()) {
			log.debug("== Bundle " + bundleName + " " + bundleVersionStr + " is provided by " + unit
					+ " with version range " + versionRange + ", matching versions: "
					+ list.stream().map(av -> av.getVersion()).map(String::valueOf).collect(Collectors.joining(", ")));
		}
		// For require-bundle, collect all packages exported by this bundle and check
		// methods from all of them
		Set<String> exportedPackages = getExportedPackages(unit);
		Set<MethodSignature> bundleMethods = new TreeSet<>();
		Map<MethodSignature, Collection<String>> references = new HashMap<>();
		for (String packageName : exportedPackages) {
			bundleMethods.addAll(collectMethodsForPackage(usages, packageName));
			references.putAll(collectReferencesForPackage(usages, packageName));
		}
		if (bundleMethods.isEmpty()) {
			return;
		}
		if (log.isDebugEnabled()) {
			for (MethodSignature signature : bundleMethods) {
				log.debug("Referenced from bundle " + bundleName + ": " + signature.id());
			}
		}
		for (ArtifactVersion v : list) {
			Version version = v.getVersion();
			if (version == null) {
				continue;
			}
			if (!allBundleVersion.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(version)) {
				continue;
			}
			Path artifact = v.getArtifact();
			log.debug(v + "=" + artifact);
			if (artifact == null) {
				continue;
			}
			ClassCollection collection = analyzeCache.get(artifact);
			if (collection == null) {
				collection = dependencyAnalyzer.analyzeProvides(artifact.toFile(), classResolver);
				analyzeCache.put(artifact, collection);
			}
			boolean ok = true;
			Set<MethodSignature> set = collection.provides().collect(Collectors.toSet());
			for (MethodSignature mthd : bundleMethods) {
				if (!set.contains(mthd)) {
					List<MethodSignature> provided = collection.get(mthd.className());
					if (log.isDebugEnabled()) {
						log.debug("Not found: " + mthd);
						if (provided != null) {
							for (MethodSignature s : provided) {
								log.debug("Provided:  " + s);
							}
						}
					}
					dependencyProblems.add(new DependencyVersionProblem(bundleName + "_" + version,
							String.format(
									"Require-Bundle `%s %s` (compiled against `%s` from `%s %s`) includes `%s` (provided by `%s`) but this version is missing the method `%s#%s`",
									bundleName, bundleVersionStr, matchedBundleVersion.toString(), unit.getId(),
									unit.getVersion(), version, v.getProvider(), mthd.className(), getMethodRef(mthd)),
							references.get(mthd), provided));
					ok = false;
					bundleWithError.add(bundleName);
				}
			}
			if (ok) {
				lowestBundleVersion.merge(bundleName, version, (v1, v2) -> v1.compareTo(v2) > 0 ? v2 : v1);
			}
		}
	}

	private Set<String> getExportedPackages(IInstallableUnit unit) {
		Set<String> packages = new HashSet<>();
		unit.getProvidedCapabilities().stream()
				.filter(cap -> "java.package".equals(cap.getNamespace()))
				.forEach(cap -> packages.add(cap.getName()));
		return packages;
	}

	private Set<MethodSignature> collectMethodsForPackage(List<ClassUsage> usages, String packageName) {
		Set<MethodSignature> methods = new TreeSet<>();
		for (ClassUsage usage : usages) {
			usage.signatures().filter(ms -> packageName.equals(ms.packageName())).forEach(methods::add);
		}
		return methods;
	}

	private Map<MethodSignature, Collection<String>> collectReferencesForPackage(List<ClassUsage> usages,
			String packageName) {
		Map<MethodSignature, Collection<String>> references = new HashMap<>();
		for (ClassUsage usage : usages) {
			usage.signatures().filter(ms -> packageName.equals(ms.packageName())).forEach(sig -> {
				references.computeIfAbsent(sig, nil -> new TreeSet<>()).addAll(usage.classRef(sig));
			});
		}
		return references;
	}

	private boolean checkMethodsInCollection(ClassCollection collection, Set<MethodSignature> methods,
			String packageName, Version version, Map<MethodSignature, Collection<String>> references,
			List<DependencyVersionProblem> dependencyProblems, Set<String> withError, ArtifactVersion v,
			IInstallableUnit unit, String versionStr, Optional<org.eclipse.equinox.p2.metadata.Version> matchedVersion,
			Log log, String dependencyType) {
		boolean ok = true;
		Set<MethodSignature> set = collection.provides().collect(Collectors.toSet());
		for (MethodSignature mthd : methods) {
			if (!set.contains(mthd)) {
				List<MethodSignature> provided = collection.get(mthd.className());
				if (provided != null) {
					provided = provided.stream().filter(ms -> packageName.equals(ms.packageName())).toList();
				}
				if (log.isDebugEnabled()) {
					log.debug("Not found: " + mthd);
					if (provided != null) {
						for (MethodSignature s : provided) {
							log.debug("Provided:  " + s);
						}
					}
				}
				dependencyProblems.add(new DependencyVersionProblem(packageName + "_" + version,
						String.format(
								"%s `%s %s` (compiled against `%s` provided by `%s %s`) includes `%s` (provided by `%s`) but this version is missing the method `%s#%s`",
								dependencyType, packageName, versionStr,
								matchedVersion.orElse(org.eclipse.equinox.p2.metadata.Version.emptyVersion).toString(),
								unit.getId(), unit.getVersion(), version, v.getProvider(), mthd.className(),
								getMethodRef(mthd)),
						references.get(mthd), provided));
				ok = false;
				withError.add(packageName);
			}
		}
		return ok;
	}

	private String getMethodRef(MethodSignature mthd) {
		if (verbose) {
			return String.format("%s %s", mthd.methodName(), mthd.signature());
		}
		return mthd.methodName();
	}

	private void applyLowerBounds(Set<String> packageWithError, Map<String, Version> lowestPackageVersion,
			Set<String> bundleWithError, Map<String, Version> lowestBundleVersion) throws MojoFailureException {
		if (packageWithError.isEmpty() && bundleWithError.isEmpty()) {
			return;
		}
		try {
			MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
			// Handle import-package updates
			if (!packageWithError.isEmpty()) {
				Map<String, String> importedPackagesVersion = manifest.getImportPackagesVersions();
				Map<String, String> packageUpdates = new HashMap<>();
				for (String packageName : packageWithError) {
					Version lowestVersion = lowestPackageVersion.getOrDefault(packageName, Version.emptyVersion);
					String current = importedPackagesVersion.get(packageName);
					if (current == null) {
						packageUpdates.put(packageName,
								String.format("[%s,%d)", lowestVersion, (lowestVersion.getMajor() + 1)));
					} else {
						VersionRange range = VersionRange.valueOf(current);
						Version right = range.getRight();
						packageUpdates.put(packageName,
								String.format("[%s,%s%c", lowestVersion, right, range.getRightType()));
					}
				}
				manifest.updateImportedPackageVersions(packageUpdates);
			}
			// Handle require-bundle updates
			if (!bundleWithError.isEmpty()) {
				Map<String, String> requiredBundleVersions = manifest.getRequiredBundleVersions();
				Map<String, String> bundleUpdates = new HashMap<>();
				for (String bundleName : bundleWithError) {
					Version lowestVersion = lowestBundleVersion.getOrDefault(bundleName, Version.emptyVersion);
					String current = requiredBundleVersions.get(bundleName);
					if (current == null) {
						bundleUpdates.put(bundleName,
								String.format("[%s,%d)", lowestVersion, (lowestVersion.getMajor() + 1)));
					} else {
						VersionRange range = VersionRange.valueOf(current);
						Version right = range.getRight();
						bundleUpdates.put(bundleName,
								String.format("[%s,%s%c", lowestVersion, right, range.getRightType()));
					}
				}
				manifest.updateRequiredBundleVersions(bundleUpdates);
			}
			MutableBundleManifest.write(manifest, manifestFile);
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
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

	private static record DependencyVersionProblem(String key, String message, Collection<String> references,
			List<MethodSignature> provided) {

	}
}
