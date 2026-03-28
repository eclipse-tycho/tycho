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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;

/**
 * Wraps a pre-resolved fragment {@link IInstallableUnit} found in the same
 * repository as its host bundle. Used by
 * {@link EclipseIndexBundleArtifactVersion#fragments()} to provide fragment
 * artifacts without additional repository lookups.
 */
class EclipseIndexFragmentArtifactVersion extends AbstractEclipseArtifactVersion {

	EclipseIndexFragmentArtifactVersion(EclipseIndexArtifactVersionProvider provider, Repository repository,
			IInstallableUnit fragmentIU) {
		super(provider, List.of(repository), fragmentIU.getVersion());
		this.unitRepo = repository;
		this.unit = Optional.of(fragmentIU);
	}

	@Override
	protected Optional<IInstallableUnit> findUnit() {
		return unit;
	}
}
