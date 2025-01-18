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
import java.nio.file.Files;
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
		Function<String, Optional<ClassMethods>> classResolver = DependencyAnalyzer
				.createDependencyClassResolver(jrtClassResolver, artifacts);
		for (GenericInfo genericInfo : requirements) {
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(genericInfo.getNamespace())) {
				Map<String, String> pkgInfo = getVersionInfo(genericInfo,
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				String packageVersion = pkgInfo.getOrDefault(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0");
				String packageName = pkgInfo.get(PackageNamespace.PACKAGE_NAMESPACE);
				Optional<IInstallableUnit> packageProvidingUnit = ArtifactMatcher.findPackage(packageName, units);
				if (packageProvidingUnit.isEmpty()) {
					continue;
				}
				IInstallableUnit unit = packageProvidingUnit.get();
				Optional<org.eclipse.equinox.p2.metadata.Version> matchedPackageVersion = ArtifactMatcher
						.getPackageVersion(unit, packageName);
				if (matchedPackageVersion.isEmpty()
						|| matchedPackageVersion.get().equals(org.eclipse.equinox.p2.metadata.Version.emptyVersion)) {
					log.warn("Package " + packageName
							+ " has no version exported and can not be checked for compatibility");
					continue;
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
					log.debug("== " + packageName + " " + packageVersion + " is provided by " + unit
							+ " with version range " + versionRange + ", matching versions: " + list.stream()
									.map(av -> av.getVersion()).map(String::valueOf).collect(Collectors.joining(", ")));
				}
				Set<MethodSignature> packageMethods = new TreeSet<>();
				Map<MethodSignature, Collection<String>> references = new HashMap<>();
				for (ClassUsage usage : usages) {
					usage.signatures().filter(ms -> packageName.equals(ms.packageName())).forEach(sig -> {
						packageMethods.add(sig);
						references.computeIfAbsent(sig, nil -> new TreeSet<>()).addAll(usage.classRef(sig));
					});
				}
				if (packageMethods.isEmpty()) {
					// it could be that actually no methods referenced (e.g. interface is only
					// referencing a type)
					// TODO we need to check that the types used are present in all versions as
					// otherwise we will get CNF exception!
					// TODO a class can also reference fields!
					continue;
				}
				if (log.isDebugEnabled()) {
					for (MethodSignature signature : packageMethods) {
						log.debug("Referenced: " + signature.id());
					}
				}
				// now we need to inspect all jars
				for (ArtifactVersion v : list) {
					Version version = v.getVersion();
					if (version == null) {
						continue;
					}
					if (!allPackageVersion.computeIfAbsent(packageName, nil -> new TreeSet<>()).add(version)) {
						// already checked!
						continue;
					}
					Path artifact = v.getArtifact();
					log.debug(v + "=" + artifact);
					if (artifact == null) {
						// Retrieval of artifacts might be lazy and we can't get this one --> error?
						continue;
					}
					ClassCollection collection = analyzeCache.get(artifact);
					if (collection == null) {
						collection = DependencyAnalyzer.analyzeProvides(artifact.toFile(), classResolver, null);
						analyzeCache.put(artifact, collection);
					}
					boolean ok = true;
					Set<MethodSignature> set = collection.provides().collect(Collectors.toSet());
					for (MethodSignature mthd : packageMethods) {
						if (!set.contains(mthd)) {
							List<MethodSignature> provided = collection.get(mthd.className());
							if (provided != null) {
								provided = provided.stream().filter(ms -> packageName.equals(ms.packageName()))
										.toList();
							}
							if (log.isDebugEnabled()) {
								log.debug("Not found: " + mthd);
								if (provided != null) {
									for (MethodSignature s : provided) {
										log.debug("Provided:  " + s);
									}
								}
							}
							dependencyProblems.add(new DependencyVersionProblem(String.format(
									"Import-Package `%s %s` (compiled against `%s` provided by `%s %s`) includes `%s` (provided by `%s`) but this version is missing the method `%s#%s`",
									packageName, packageVersion,
									matchedPackageVersion.orElse(org.eclipse.equinox.p2.metadata.Version.emptyVersion)
											.toString(),
									unit.getId(), unit.getVersion(),
									v.getVersion(), v.getProvider(), mthd.className(), getMethodRef(mthd)),
									references.get(mthd), provided));
							ok = false;
							packageWithError.add(packageName);
						}
					}
					if (ok) {
						lowestPackageVersion.merge(packageName, version, (v1, v2) -> {
							if (v1.compareTo(v2) > 0) {
								return v2;
							}
							return v1;
						});
					}
				}
				// TODO we should emit a warning if the lower bound is not part of the
				// discovered versions (or even fail?)

			}
		}
		if (dependencyProblems.isEmpty()) {
			return;
		}
		List<String> results = new ArrayList<>();
		for (DependencyVersionProblem problem : dependencyProblems) {
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
					message = String.format("%s referenced by `%s`.", problem.message(), references.iterator().next());
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
		if (results.isEmpty()) {
			return;
		}
		try {
			Files.writeString(reportFileName.toPath(),
					results.stream().collect(Collectors.joining(System.lineSeparator())));
			if (applySuggestions) {
				applyLowerBounds(packageWithError, lowestPackageVersion);
			}
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
	}

	private String getMethodRef(MethodSignature mthd) {
		if (verbose) {
			return String.format("%s %s", mthd.methodName(), mthd.signature());
		}
		return mthd.methodName();
	}

	private void applyLowerBounds(Set<String> packageWithError, Map<String, Version> lowestPackageVersion)
			throws IOException {
		MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
		Map<String, String> exportedPackagesVersion = manifest.getExportedPackagesVersion();
		Map<String, String> updates = new HashMap<>();
		for (String packageName : packageWithError) {
			Version lowestVersion = lowestPackageVersion.getOrDefault(packageName, Version.emptyVersion);
			String current = exportedPackagesVersion.get(packageName);
			if (current == null) {
				updates.put(packageName, String.format("[%s,%d)", lowestVersion, (lowestVersion.getMajor() + 1)));
			} else {
				VersionRange range = VersionRange.valueOf(current);
				Version right = range.getRight();
				updates.put(packageName, String.format("[%s,%s%c", lowestVersion, right, range.getRightType()));
			}
		}
		manifest.updateImportedPackageVersions(updates);
		MutableBundleManifest.write(manifest, manifestFile);
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

	private static record DependencyVersionProblem(String message, Collection<String> references,
			List<MethodSignature> provided) {

	}
}
