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

class EclipseIndexBundleArtifactVersion extends AbstractEclipseArtifactVersion {

	private final String bundleName;

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
}
