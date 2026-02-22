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
		boolean ok = true;
		Set<MethodSignature> set = collection.provides().collect(Collectors.toSet());
		for (MethodSignature mthd : methods) {
			if (!set.contains(mthd)) {
				List<MethodSignature> provided = collection.get(mthd.className());
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
				context.addProblem(new DependencyVersionProblem(dependencyName + "_" + version,
						String.format(
								"%s `%s %s` (compiled against `%s` provided by `%s %s`) includes `%s` (provided by `%s`) but this version is missing the method `%s#%s`",
								dependencyType, dependencyName, versionStr,
								matchedVersion != null ? matchedVersion.toString()
										: org.eclipse.equinox.p2.metadata.Version.emptyVersion.toString(),
								unit.getId(), unit.getVersion(), version, v.getProvider(), mthd.className(),
								getMethodRef(mthd)),
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
}
