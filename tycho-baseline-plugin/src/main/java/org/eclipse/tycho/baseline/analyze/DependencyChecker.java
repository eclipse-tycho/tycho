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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Base class for dependency checkers that validates version ranges for
 * used in dependencies.
 */
public abstract class DependencyChecker {

	protected final Map<String, Version> lowestVersion = new HashMap<>();
	protected final Map<String, Set<Version>> allVersions = new HashMap<>();
	protected final Set<String> withError = new HashSet<>();
	protected final CheckContext context;

	/**
	 * Creates a new dependency checker.
	 * 
	 * @param context the check context containing shared state
	 */
	protected DependencyChecker(CheckContext context) {
		this.context = context;
	}

	/**
	 * Gets the lowest version found for each dependency with errors.
	 * @return a map with lowest version found for each dependency with errors.
	 */
	public Map<String, Version> getLowestVersions() {
		return lowestVersion;
	}

	/**
	 * Gets all versions checked for each dependency.
	 * @return a map of all versions checked for each dependency.
	 */
	public Map<String, Set<Version>> getAllVersions() {
		return allVersions;
	}

	/**
	 * Gets the set of dependencies that have errors.
	 * @return a set of dependencies that have errors.
	 */
	public Set<String> getWithError() {
		return withError;
	}

	/**
	 * Collects method signatures used from the given package.
	 */
	protected Set<MethodSignature> collectMethodsForPackage(List<ClassUsage> usages, String packageName) {
		Set<MethodSignature> methods = new TreeSet<>();
		for (ClassUsage usage : usages) {
			usage.signatures().filter(ms -> packageName.equals(ms.packageName())).forEach(methods::add);
		}
		return methods;
	}

	/**
	 * Collects references to method signatures for the given package.
	 */
	protected Map<MethodSignature, Collection<String>> collectReferencesForPackage(List<ClassUsage> usages,
			String packageName) {
		Map<MethodSignature, Collection<String>> references = new HashMap<>();
		for (ClassUsage usage : usages) {
			usage.signatures().filter(ms -> packageName.equals(ms.packageName())).forEach(sig -> {
				references.computeIfAbsent(sig, nil -> new TreeSet<>()).addAll(usage.classRef(sig));
			});
		}
		return references;
	}

	/**
	 * Checks if methods are present in the collection and reports problems.
	 * 
	 * @param collection        the class collection to check
	 * @param methods           the methods to find
	 * @param dependencyName    the name of the dependency
	 * @param packageNameFilter optional filter to restrict provided method list
	 * @param version           the version being checked
	 * @param references        the references to the methods
	 * @param v                 the artifact version
	 * @param unit              the installable unit
	 * @param versionStr        the version string from the manifest
	 * @param matchedVersion    the matched version
	 * @param dependencyType    the type of dependency (e.g., "Import-Package")
	 * @return true if all methods were found
	 */
	protected boolean checkMethodsInCollection(ClassCollection collection, Set<MethodSignature> methods,
			String dependencyName, String packageNameFilter, Version version,
			Map<MethodSignature, Collection<String>> references, ArtifactVersion v, IInstallableUnit unit,
			String versionStr, org.eclipse.equinox.p2.metadata.Version matchedVersion, String dependencyType) {
		return checkMethodsInCollections(List.of(collection), methods, dependencyName, packageNameFilter, version,
				references, v, unit, versionStr, matchedVersion, dependencyType);
	}

	/**
	 * Checks if methods are present in any of the given collections and reports
	 * problems for missing ones. This supports checking against a main bundle's
	 * classes combined with re-exported bundle classes.
	 *
	 * @param collections       the class collections to check (main + re-exported)
	 * @param methods           the methods to find
	 * @param dependencyName    the name of the dependency
	 * @param packageNameFilter optional filter to restrict provided method list
	 * @param version           the version being checked
	 * @param references        the references to the methods
	 * @param v                 the artifact version
	 * @param unit              the installable unit
	 * @param versionStr        the version string from the manifest
	 * @param matchedVersion    the matched version
	 * @param dependencyType    the type of dependency (e.g., "Require-Bundle")
	 * @return true if all methods were found
	 */
	protected boolean checkMethodsInCollections(List<ClassCollection> collections, Set<MethodSignature> methods,
			String dependencyName, String packageNameFilter, Version version,
			Map<MethodSignature, Collection<String>> references, ArtifactVersion v, IInstallableUnit unit,
			String versionStr, org.eclipse.equinox.p2.metadata.Version matchedVersion, String dependencyType) {
		return checkMethodsInCollections(collections, methods, dependencyName, packageNameFilter, version, references,
				v, unit, versionStr, matchedVersion, dependencyType, Map.of());
	}

	/**
	 * Checks if methods are present in any of the given collections and reports
	 * problems for missing ones. This supports checking against a main bundle's
	 * classes combined with re-exported bundle classes, with optional provenance
	 * information for re-exported packages.
	 *
	 * @param collections         the class collections to check (main + re-exported)
	 * @param methods             the methods to find
	 * @param dependencyName      the name of the dependency
	 * @param packageNameFilter   optional filter to restrict provided method list
	 * @param version             the version being checked
	 * @param references          the references to the methods
	 * @param v                   the artifact version
	 * @param unit                the installable unit
	 * @param versionStr          the version string from the manifest
	 * @param matchedVersion      the matched version
	 * @param dependencyType      the type of dependency (e.g., "Require-Bundle")
	 * @param reexportProvenance  map from package name to provenance description
	 *                            (e.g., "re-exported from `org.eclipse.swt [3.133.0,4.0.0)`")
	 * @return true if all methods were found
	 */
	protected boolean checkMethodsInCollections(List<ClassCollection> collections, Set<MethodSignature> methods,
			String dependencyName, String packageNameFilter, Version version,
			Map<MethodSignature, Collection<String>> references, ArtifactVersion v, IInstallableUnit unit,
			String versionStr, org.eclipse.equinox.p2.metadata.Version matchedVersion, String dependencyType,
			Map<String, String> reexportProvenance) {
		boolean ok = true;
		Set<MethodSignature> set = new HashSet<>();
		for (ClassCollection cc : collections) {
			cc.provides().forEach(set::add);
		}
		for (MethodSignature mthd : methods) {
			if (!set.contains(mthd)) {
				List<MethodSignature> provided = null;
				for (ClassCollection cc : collections) {
					provided = cc.get(mthd.className());
					if (provided != null && !provided.isEmpty()) {
						break;
					}
				}
				if (provided != null && packageNameFilter != null) {
					provided = provided.stream().filter(ms -> packageNameFilter.equals(ms.packageName())).toList();
				}
				Log log = context.getLog();
				if (log.isDebugEnabled()) {
					log.debug("Not found: " + mthd);
					if (provided != null) {
						for (MethodSignature s : provided) {
							log.debug("Provided:  " + s);
						}
					}
				}
				String provenance = reexportProvenance.getOrDefault(mthd.packageName(), "");
				String provenanceSuffix = provenance.isEmpty() ? "" : " (package `" + mthd.packageName() + "` " + provenance + ")";
				context.addProblem(new DependencyVersionProblem(dependencyName + "_" + version,
						String.format(
								"%s `%s %s` (compiled against `%s` provided by `%s %s`) includes `%s` (provided by `%s`) but this version is missing the method `%s#%s`%s",
								dependencyType, dependencyName, versionStr,
								matchedVersion != null ? matchedVersion.toString()
										: org.eclipse.equinox.p2.metadata.Version.emptyVersion.toString(),
								unit.getId(), unit.getVersion(), version, v.getProvider(), mthd.className(),
								getMethodRef(mthd), provenanceSuffix),
						references.get(mthd), provided));
				ok = false;
				withError.add(dependencyName);
			}
		}
		return ok;
	}

	private String getMethodRef(MethodSignature mthd) {
		if (context.isVerbose()) {
			return String.format("%s %s", mthd.methodName(), mthd.signature());
		}
		return mthd.methodName();
	}

	/**
	 * Applies version suggestions to the bundle manifest.
	 *
	 * @param manifest the mutable bundle manifest to update
	 * @return {@code true} if any suggestions were applied
	 */
	public abstract boolean applySuggestions(MutableBundleManifest manifest);

	/**
	 * Reports version suggestions to the given results builder and log.
	 *
	 * @param results the markdown builder to append suggestions to
	 * @param log     the Maven log for informational output
	 */
	public abstract void reportSuggestions(MarkdownBuilder results, Log log);
	
	/**
	 * Returns a version suitable for use in version ranges by stripping the
	 * qualifier. If the version has a qualifier and there are broken versions
	 * with the same {@code major.minor.micro}, this finds the next higher
	 * {@code major.minor.micro} version from the available versions. If no
	 * broken versions share the same base, the qualifier is simply removed.
	 *
	 * @param version  the version to strip
	 * @param name     the dependency name to look up available versions
	 * @return a version without qualifier
	 */
	protected Version stripQualifier(Version version, String name) {
		Version baseVersion = new Version(version.getMajor(), version.getMinor(), version.getMicro());
		if (version.getQualifier() == null || version.getQualifier().isEmpty()) {
			return baseVersion;
		}
		Set<Version> versions = allVersions.get(name);
		Version lowest = lowestVersion.get(name);
		if (versions != null && lowest != null) {
			// Check if there are broken versions with the same major.minor.micro
			boolean hasBadWithSameBase = versions.stream()
					.filter(v -> v.compareTo(lowest) < 0)
					.anyMatch(v -> v.getMajor() == baseVersion.getMajor()
							&& v.getMinor() == baseVersion.getMinor()
							&& v.getMicro() == baseVersion.getMicro());
			if (hasBadWithSameBase) {
				for (Version v : versions) {
					Version vBase = new Version(v.getMajor(), v.getMinor(), v.getMicro());
					if (vBase.compareTo(baseVersion) > 0) {
						return vBase;
					}
				}
				// fallback: bump micro
				return new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1);
			}
		}
		return baseVersion;
	}

	protected void reportVersionContrainSuggestions(String type, MarkdownBuilder results, Log log) {
		Set<String> withError = getWithError();
		if (!withError.isEmpty()) {
			results.add("");
			for (String name : withError) {
				String suggestion = String.format("Suggested lower version for %s `%s` is `%s`", type, name,
						getLowestVersions().get(name));
				Set<Version> all = getAllVersions().get(name);
				if (all != null && !all.isEmpty()) {
					suggestion += " out of " + all.stream().map(v -> String.format("`%s`", v))
							.collect(Collectors.joining(", ", "[", "]"));
				}
				log.info(suggestion);
				results.add(suggestion);
			}
		}
	}

	/**
	 * Checks whether two version range strings are semantically equivalent using
	 * OSGi {@link VersionRange} comparison. For example {@code [3.5.0,4)} and
	 * {@code [3.5.0,4.0.0)} are semantically equal.
	 *
	 * @param range1 the first version range string
	 * @param range2 the second version range string
	 * @return {@code true} if both ranges represent the same semantic range
	 */
	protected static boolean isSameVersionRange(String range1, String range2) {
		if (range1 == null || range2 == null) {
			return range1 == range2;
		}
		return VersionRange.valueOf(range1).equals(VersionRange.valueOf(range2));
	}
}
