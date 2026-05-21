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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.version.Version;
import org.eclipse.osgi.container.ModuleRevisionBuilder;

class MavenBundleArtifactVersion extends AbstractMavenArtifactVersion {

	private final String bundleName;
	private org.osgi.framework.Version bundleVersion;

	public MavenBundleArtifactVersion(MavenArtifactVersionProvider provider, Artifact artifact, Version version,
			String bundleName, List<RemoteRepository> repositories) {
		super(provider, artifact, version, repositories);
		this.bundleName = bundleName;
	}

	@Override
	public org.osgi.framework.Version getVersion() {
		if (bundleVersion == null) {
			ModuleRevisionBuilder builder = provider.readOSGiInfo(getArtifact());
			if (builder != null && bundleName.equals(builder.getSymbolicName())) {
				bundleVersion = builder.getVersion();
			}
		}
		return bundleVersion;
	}
}
