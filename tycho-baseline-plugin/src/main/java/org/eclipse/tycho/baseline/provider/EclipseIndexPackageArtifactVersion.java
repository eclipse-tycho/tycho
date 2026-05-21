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

import java.util.List;
import java.util.Optional;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;

class EclipseIndexPackageArtifactVersion extends AbstractEclipseArtifactVersion {

	private final String unitId;
	private final String packageName;

	public EclipseIndexPackageArtifactVersion(EclipseIndexArtifactVersionProvider provider,
			List<Repository> repositories, String unitId, String packageName, Version version, Logger logger) {
		super(provider, repositories, version);
		this.unitId = unitId;
		this.packageName = packageName;
	}

	@Override
	protected Optional<IInstallableUnit> findUnit() {
		for (Repository repository : repositories) {
			try {
				org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
				r.setUrl(repository.getLocation().toString());
				IMetadataRepository metadataRepository = getVersionProvider().repositoryManager
						.getMetadataRepository(r);
				Optional<IInstallableUnit> optional = ArtifactMatcher.findPackage(packageName,
						metadataRepository.query(QueryUtil.createIUQuery(unitId), null), version);
				if (optional.isPresent()) {
					this.unitRepo = repository;
					return unit = optional;
				} else {
					getVersionProvider().logger
							.debug("Package " + packageName + " not found in metadata of " + repository.getLocation());
				}
			} catch (Exception e) {
				getVersionProvider().logger.error("Fetch metadata from " + repository.getLocation() + ":: " + e);
			}
		}
		return Optional.empty();
	}
}
