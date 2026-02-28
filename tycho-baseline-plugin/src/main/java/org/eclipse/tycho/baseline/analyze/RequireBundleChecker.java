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
	 * @throws MojoFailureException if checks failed
	 */
	public void check(String bundleName, String bundleVersionStr) throws MojoFailureException {
		Log log = context.getLog();
		Optional<IInstallableUnit> bundleProvidingUnit = ArtifactMatcher.findBundle(bundleName, units);
		if (bundleProvidingUnit.isEmpty()) {
			return;
		}
		IInstallableUnit unit = bundleProvidingUnit.get();
		org.eclipse.equinox.p2.metadata.Version matchedBundleVersion = unit.getVersion();
		if (matchedBundleVersion.isOSGiCompatible()) {
			Version current = new Version(matchedBundleVersion.toString());
			allVersions.computeIfAbsent(bundleName, nil -> new TreeSet<>()).add(current);
			lowestVersion.put(bundleName, current);
		}
		VersionRange versionRange = VersionRange.valueOf(bundleVersionStr);
		List<ArtifactVersion> list = context.getVersionProviders().stream()
				.flatMap(avp -> avp.getBundleVersions(unit, bundleName, versionRange, context.getProject())).toList();
		if (log.isDebugEnabled()) {
			log.debug("== Bundle " + bundleName + " " + bundleVersionStr + " is provided by " + unit
					+ " with version range " + versionRange + ", matching versions: "
					+ list.stream().map(av -> av.getVersion()).map(String::valueOf).collect(Collectors.joining(", ")));
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
			// Determine exported packages for THIS specific version from the JAR manifest
			Set<String> exportedPackages = getExportedPackagesFromJar(artifact);
			if (exportedPackages.isEmpty()) {
				continue;
			}
			// Collect methods our code uses from these exported packages
			Set<MethodSignature> bundleMethods = new TreeSet<>();
			Map<MethodSignature, Collection<String>> references = new HashMap<>();
			for (String packageName : exportedPackages) {
				bundleMethods.addAll(collectMethodsForPackage(usages, packageName));
				references.putAll(collectReferencesForPackage(usages, packageName));
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
			// For Require-Bundle, pass null as packageNameFilter since methods can come
			// from different packages
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
		return true;
	}

	@Override
	public void reportSuggestions(MarkdownBuilder results, Log log) {
		reportVersionContrainSuggestions("bundle", results, log);
	}
}
