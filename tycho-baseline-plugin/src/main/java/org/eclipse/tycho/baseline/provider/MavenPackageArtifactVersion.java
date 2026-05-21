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
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.version.Version;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.osgi.framework.namespace.PackageNamespace;

class MavenPackageArtifactVersion extends AbstractMavenArtifactVersion {

	private final String packageName;
	private org.osgi.framework.Version packageVersion;

	public MavenPackageArtifactVersion(MavenArtifactVersionProvider provider, Artifact artifact, Version version,
			String packageName, List<RemoteRepository> repositories) {
		super(provider, artifact, version, repositories);
		this.packageName = packageName;
	}

	@Override
	public org.osgi.framework.Version getVersion() {
		if (packageVersion == null) {
			ModuleRevisionBuilder builder = provider.readOSGiInfo(getArtifact());
			if (builder != null) {
				List<GenericInfo> capabilities = builder.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
				for (GenericInfo info : capabilities) {
					Map<String, Object> attributes = info.getAttributes();
					if (packageName.equals(attributes.get(PackageNamespace.PACKAGE_NAMESPACE))) {
						packageVersion = (org.osgi.framework.Version) attributes.getOrDefault(
								PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, org.osgi.framework.Version.emptyVersion);
					}
				}
			}
		}
		return packageVersion;
	}
}
