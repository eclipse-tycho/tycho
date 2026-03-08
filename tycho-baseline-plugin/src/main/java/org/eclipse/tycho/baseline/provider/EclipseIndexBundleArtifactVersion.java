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
package org.eclipse.tycho.baseline.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;

class EclipseIndexBundleArtifactVersion extends AbstractEclipseArtifactVersion {

	private final String bundleName;
	private List<ArtifactVersion> cachedFragments;

	public EclipseIndexBundleArtifactVersion(EclipseIndexArtifactVersionProvider provider,
			List<Repository> repositories, String bundleName, Version version, Logger logger) {
		super(provider, repositories, version);
		this.bundleName = bundleName;
	}

	@Override
	protected Optional<IInstallableUnit> findUnit() {
		for (Repository repository : repositories) {
			try {
				org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
				r.setUrl(repository.getLocation().toString());
				IMetadataRepository metadataRepository = getVersionProvider().repositoryManager
						.getMetadataRepository(r);
				IInstallableUnit foundUnit = metadataRepository
						.query(QueryUtil.createIUQuery(bundleName, version), null).stream().findFirst().orElse(null);
				if (foundUnit != null) {
					this.unitRepo = repository;
					return unit = Optional.of(foundUnit);
				} else {
					getVersionProvider().logger.debug("Bundle " + bundleName + " version " + version
							+ " not found in metadata of " + repository.getLocation());
				}
			} catch (Exception e) {
				getVersionProvider().logger.error("Fetch metadata from " + repository.getLocation() + ":: " + e);
			}
		}
		return Optional.empty();
	}

	@Override
	public Stream<ArtifactVersion> fragments() {
		if (cachedFragments != null) {
			return cachedFragments.stream();
		}
		// Ensure the unit is resolved so we know which repo it came from
		Optional<IInstallableUnit> hostUnit = unit;
		if (hostUnit == null) {
			hostUnit = findUnit();
		}
		if (hostUnit == null || hostUnit.isEmpty() || unitRepo == null) {
			cachedFragments = List.of();
			return cachedFragments.stream();
		}
		Version hostVersion = hostUnit.get().getVersion();
		try {
			org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
			r.setUrl(unitRepo.getLocation().toString());
			IMetadataRepository metadataRepository = getVersionProvider().repositoryManager
					.getMetadataRepository(r);
			Collection<IInstallableUnit> candidates = metadataRepository
					.query(QueryUtil.createMatchQuery(
							"providedCapabilities.exists(x | x.namespace == $0 && x.name == $1)",
							BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT, bundleName),
							null)
					.toUnmodifiableSet();
			cachedFragments = candidates.stream()
					.filter(candidate -> fragmentMatchesHostVersion(candidate, bundleName, hostVersion))
					.map(fragmentIU -> (ArtifactVersion) new EclipseIndexFragmentArtifactVersion(
							getVersionProvider(), unitRepo, fragmentIU))
					.toList();
			return cachedFragments.stream();
		} catch (Exception e) {
			getVersionProvider().logger.error(
					"Failed to query fragments for " + bundleName + " from " + unitRepo.getLocation() + ": " + e);
			cachedFragments = List.of();
			return cachedFragments.stream();
		}
	}

	/**
	 * Checks whether a fragment IU's {@code Fragment-Host} requirement includes the
	 * given host version.
	 */
	private static boolean fragmentMatchesHostVersion(IInstallableUnit fragmentIU, String hostBundleName,
			Version hostVersion) {
		for (IRequirement req : fragmentIU.getRequirements()) {
			if (req instanceof IRequiredCapability rc
					&& BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(rc.getNamespace())
					&& hostBundleName.equals(rc.getName())) {
				VersionRange range = rc.getRange();
				return range.isIncluded(hostVersion);
			}
		}
		return false;
	}
}
