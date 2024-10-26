/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.eclipsebuild;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.helper.PluginConfigurationHelper;
import org.eclipse.tycho.helper.PluginConfigurationHelper.Configuration;
import org.eclipse.tycho.resolver.InstallableUnitProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("eclipse-build")
public class EclipseBuildInstallableUnitProvider implements InstallableUnitProvider {

	private final PluginConfigurationHelper configurationHelper;

	@Inject
	public EclipseBuildInstallableUnitProvider(PluginConfigurationHelper configurationHelper) {
		this.configurationHelper = configurationHelper;
	}

	@Override
	public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
			throws CoreException {
		Configuration configuration = configurationHelper.getConfiguration(EclipseBuildMojo.class, project, session);
		Optional<Boolean> local = configuration.getBoolean(EclipseBuildMojo.PARAMETER_LOCAL);
		if (local.isPresent() && local.get()) {
			// for local target resolution the bundles become requirements...
			Optional<List<String>> list = configuration.getStringList("bundles");
			return InstallableUnitProvider.createIU(list.stream().flatMap(Collection::stream)
					.map(bundleName -> MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
							bundleName,
							VersionRange.emptyRange, null, false, true)),
					"eclipse-build-bundles");
		}
		return List.of();
	}

}
