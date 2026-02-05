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
 * Checker for Import-Package dependencies.
 */
public class ImportPackageChecker extends DependencyChecker {

	private final Collection<IInstallableUnit> units;
	private final List<ClassUsage> usages;

	/**
	 * Creates a new Import-Package checker.
	 * 
	 * @param context the check context
	 * @param units   the installable units
	 * @param usages  the class usages from the project
	 */
	public ImportPackageChecker(CheckContext context, Collection<IInstallableUnit> units, List<ClassUsage> usages) {
		super(context);
		this.units = units;
		this.usages = usages;
	}

	/**
	 * Checks an Import-Package dependency.
	 * 
	 * @param packageName    the name of the imported package
	 * @param packageVersion the version range string
	 */
	public void check(String packageName, String packageVersion) throws MojoFailureException {
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
			allVersions.computeIfAbsent(packageName, nil -> new TreeSet<>()).add(current);
			lowestVersion.put(packageName, current);
		});
		VersionRange versionRange = VersionRange.valueOf(packageVersion);
		List<ArtifactVersion> list = versionProviders.stream()
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
			if (!allVersions.computeIfAbsent(packageName, nil -> new TreeSet<>()).add(version)) {
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
			boolean ok = checkMethodsInCollection(collection, packageMethods, packageName, packageName, version,
					references, v, unit, packageVersion, matchedPackageVersion.orElse(null), "Import-Package");
			if (ok) {
				lowestVersion.merge(packageName, version, (v1, v2) -> v1.compareTo(v2) > 0 ? v2 : v1);
			}
		}
	}
}
