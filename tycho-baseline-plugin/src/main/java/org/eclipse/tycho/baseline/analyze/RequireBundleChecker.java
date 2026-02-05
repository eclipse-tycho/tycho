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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Checker for Require-Bundle dependencies.
 */
public class RequireBundleChecker extends DependencyChecker {

	private final Collection<IInstallableUnit> units;
	private final List<ClassUsage> usages;

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
	 * Checks a Require-Bundle dependency.
	 * 
	 * @param bundleName       the symbolic name of the required bundle
	 * @param bundleVersionStr the version range string
	 */
	public void check(String bundleName, String bundleVersionStr) throws MojoFailureException {
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
			allVersions.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(current);
			lowestVersion.put(bundleName, current);
		}
		VersionRange versionRange = VersionRange.valueOf(bundleVersionStr);
		List<ArtifactVersion> list = versionProviders.stream()
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
			if (!allVersions.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(version)) {
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
			// For Require-Bundle, pass null as packageNameFilter since methods can come
			// from different packages
			boolean ok = checkMethodsInCollection(collection, bundleMethods, bundleName, null, version, references, v,
					unit, bundleVersionStr, matchedBundleVersion, "Require-Bundle");
			if (ok) {
				lowestVersion.merge(bundleName, version, (v1, v2) -> v1.compareTo(v2) > 0 ? v2 : v1);
			}
		}
	}

	private Set<String> getExportedPackages(IInstallableUnit unit) {
		Set<String> packages = new HashSet<>();
		unit.getProvidedCapabilities().stream().filter(cap -> "java.package".equals(cap.getNamespace()))
				.forEach(cap -> packages.add(cap.getName()));
		return packages;
	}
}
