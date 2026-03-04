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
package org.eclipse.tycho.baseline.analyze;

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
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Checker for Require-Bundle dependencies. Works in two phases:
 * <ol>
 * <li>{@link #check(String, String)} collects all versions and required data
 * for each required bundle.</li>
 * <li>{@link #complete()} performs the actual version checking, handling
 * split-package situations where multiple bundles export the same package.</li>
 * </ol>
 */
public class RequireBundleChecker extends DependencyChecker {

	private final Collection<IInstallableUnit> units;
	private final List<ClassUsage> usages;
	private final List<BundleCheckData> pendingChecks = new ArrayList<>();

	private record BundleCheckData(String bundleName, String bundleVersionStr, IInstallableUnit unit,
			Version compiledAgainstVersion, org.eclipse.equinox.p2.metadata.Version matchedBundleVersion,
			List<ArtifactVersion> versions, Path compiledAgainstArtifact) {
	}

	/**
	 * Creates a new Require-Bundle checker.
	 * 
	 * @param context the check context
	 * @param units   the installable units
	 * @param usages  the class usages from the project
	 */
	public RequireBundleChecker(CheckContext context, Collection<IInstallableUnit> units, List<ClassUsage> usages) {
		super(context);
		this.units = units;
		this.usages = usages;
	}

	/**
	 * Collects version data for a Require-Bundle dependency. The actual version
	 * checking is deferred to {@link #complete()} to allow split-package detection
	 * across all required bundles.
	 * 
	 * @param bundleName       the symbolic name of the required bundle
	 * @param bundleVersionStr the version range string
	 * @throws MojoFailureException if data collection fails
	 */
	public void check(String bundleName, String bundleVersionStr) throws MojoFailureException {
		Optional<IInstallableUnit> bundleProvidingUnit = ArtifactMatcher.findBundle(bundleName, units);
		if (bundleProvidingUnit.isEmpty()) {
			return;
		}
		IInstallableUnit unit = bundleProvidingUnit.get();
		org.eclipse.equinox.p2.metadata.Version matchedBundleVersion = unit.getVersion();
		Version current = null;
		if (matchedBundleVersion.isOSGiCompatible()) {
			current = new Version(matchedBundleVersion.toString());
			allVersions.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(current);
			lowestVersion.put(bundleName, current);
		}
		VersionRange versionRange = VersionRange.valueOf(bundleVersionStr);
		List<ArtifactVersion> list = context.getVersionProviders().stream()
				.flatMap(avp -> avp.getBundleVersions(unit, bundleName, versionRange, context.getProject())).toList();
		// Find the compiled-against version's artifact
		Path compiledAgainstArtifact = null;
		for (ArtifactVersion v : list) {
			Version version = v.getVersion();
			if (version != null && current != null && version.equals(current) && v.getArtifact() != null) {
				compiledAgainstArtifact = v.getArtifact();
				break;
			}
		}
		pendingChecks.add(new BundleCheckData(bundleName, bundleVersionStr, unit, current, matchedBundleVersion, list,
				compiledAgainstArtifact));
	}

	/**
	 * Performs the actual version checking for all collected bundles. Detects
	 * split-package situations where the same package is exported by multiple
	 * required bundles and filters method checks to only include classes that
	 * actually belong to the respective bundle.
	 * 
	 * @throws MojoFailureException if the check encounters fatal problems
	 */
	public void complete() throws MojoFailureException {
		Log log = context.getLog();
		// Phase 1: Determine exported packages and class names per bundle from
		// compiled-against versions
		Map<String, Set<String>> bundleExportedPackages = new HashMap<>();
		Map<String, Set<String>> bundleClassNames = new HashMap<>();
		for (BundleCheckData data : pendingChecks) {
			if (data.compiledAgainstArtifact() != null) {
				Set<String> exportedPkgs = getExportedPackagesFromJar(data.compiledAgainstArtifact());
				bundleExportedPackages.put(data.bundleName(), exportedPkgs);
				ClassCollection cc = context.getClassCollection(data.compiledAgainstArtifact());
				Set<String> classNames = cc.provides().map(MethodSignature::className).collect(Collectors.toSet());
				bundleClassNames.put(data.bundleName(), classNames);
			}
		}
		// Phase 2: Identify split packages (exported by multiple required bundles)
		Map<String, Set<String>> packageToBundles = new HashMap<>();
		for (var entry : bundleExportedPackages.entrySet()) {
			for (String pkg : entry.getValue()) {
				packageToBundles.computeIfAbsent(pkg, k -> new HashSet<>()).add(entry.getKey());
			}
		}
		Set<String> splitPackages = packageToBundles.entrySet().stream().filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey).collect(Collectors.toSet());
		if (!splitPackages.isEmpty()) {
			log.debug("Detected split packages: " + splitPackages);
		}
		// Phase 3: Check each bundle's versions with split-package awareness
		for (BundleCheckData data : pendingChecks) {
			Set<String> myClassNames = bundleClassNames.getOrDefault(data.bundleName(), Set.of());
			checkBundle(data, splitPackages, myClassNames);
		}
	}

	private void checkBundle(BundleCheckData data, Set<String> splitPackages, Set<String> myClassNames)
			throws MojoFailureException {
		Log log = context.getLog();
		String bundleName = data.bundleName();
		String bundleVersionStr = data.bundleVersionStr();
		IInstallableUnit unit = data.unit();
		org.eclipse.equinox.p2.metadata.Version matchedBundleVersion = data.matchedBundleVersion();
		if (log.isDebugEnabled()) {
			log.debug("== Bundle " + bundleName + " " + bundleVersionStr + " is provided by " + unit
					+ " with version range " + VersionRange.valueOf(bundleVersionStr) + ", matching versions: "
					+ data.versions().stream().map(av -> av.getVersion()).map(String::valueOf)
							.collect(Collectors.joining(", ")));
		}
		for (ArtifactVersion v : data.versions()) {
			Version version = v.getVersion();
			if (version == null) {
				continue;
			}
			if (!allVersions.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(version)) {
				continue;
			}
			Path artifact = v.getArtifact();
			log.debug(v + "=" + artifact);
			if (artifact == null) {
				continue;
			}
			Set<String> exportedPackages = getExportedPackagesFromJar(artifact);
			if (exportedPackages.isEmpty()) {
				continue;
			}
			Set<MethodSignature> bundleMethods = new TreeSet<>();
			Map<MethodSignature, Collection<String>> references = new HashMap<>();
			for (String packageName : exportedPackages) {
				Set<MethodSignature> packageMethods = collectMethodsForPackage(usages, packageName);
				Map<MethodSignature, Collection<String>> packageRefs = collectReferencesForPackage(usages, packageName);
				if (splitPackages.contains(packageName)) {
					// Filter to only methods whose class is in this bundle
					packageMethods = packageMethods.stream().filter(m -> myClassNames.contains(m.className()))
							.collect(Collectors.toCollection(TreeSet::new));
					packageRefs.entrySet().removeIf(e -> !myClassNames.contains(e.getKey().className()));
				}
				bundleMethods.addAll(packageMethods);
				references.putAll(packageRefs);
			}
			if (bundleMethods.isEmpty()) {
				continue;
			}
			if (log.isDebugEnabled()) {
				for (MethodSignature signature : bundleMethods) {
					log.debug("Referenced from bundle " + bundleName + " version " + version + ": " + signature.id());
				}
			}
			ClassCollection collection = context.getClassCollection(artifact);
			boolean ok = checkMethodsInCollection(collection, bundleMethods, bundleName, null, version, references, v,
					unit, bundleVersionStr, matchedBundleVersion, "Require-Bundle");
			if (ok) {
				lowestVersion.merge(bundleName, version, (v1, v2) -> v1.compareTo(v2) > 0 ? v2 : v1);
			}
		}
	}

	private Set<String> getExportedPackagesFromJar(Path jarPath) {
		Set<String> packages = new HashSet<>();
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			Manifest manifest = jar.getManifest();
			if (manifest != null) {
				String exportPackage = manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
				if (exportPackage != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, exportPackage);
					if (elements != null) {
						for (ManifestElement element : elements) {
							packages.add(element.getValue());
						}
					}
				}
			}
		} catch (BundleException | java.io.IOException e) {
			context.getLog().debug("Could not read exported packages from " + jarPath + ": " + e);
		}
		return packages;
	}

	@Override
	public boolean applySuggestions(MutableBundleManifest manifest) {
		if (withError.isEmpty()) {
			return false;
		}
		Map<String, String> requiredBundleVersions = manifest.getRequiredBundleVersions();
		Map<String, String> bundleUpdates = new HashMap<>();
		Map<String, Version> lowestBundleVersion = getLowestVersions();
		for (String bundleName : withError) {
			Version lowestVersion = lowestBundleVersion.getOrDefault(bundleName, Version.emptyVersion);
			Version stripped = stripQualifier(lowestVersion, bundleName);
			String current = requiredBundleVersions.get(bundleName);
			if (current == null) {
				bundleUpdates.put(bundleName,
						String.format("[%s,%d)", stripped, (stripped.getMajor() + 1)));
			} else {
				VersionRange range = VersionRange.valueOf(current);
				Version right = range.getRight();
				if (right == null) {
					bundleUpdates.put(bundleName,
							String.format("[%s,%d)", stripped, (stripped.getMajor() + 1)));
				} else {
					bundleUpdates.put(bundleName,
							String.format("[%s,%s%c", stripped, right, range.getRightType()));
				}
			}
		}
		manifest.updateRequiredBundleVersions(bundleUpdates);
		return true;
	}

	@Override
	public void reportSuggestions(MarkdownBuilder results, Log log) {
		reportVersionContrainSuggestions("bundle", results, log);
	}
}
