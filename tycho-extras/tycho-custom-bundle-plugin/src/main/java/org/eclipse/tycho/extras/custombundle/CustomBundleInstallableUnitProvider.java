/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.custombundle;

import java.util.Collection;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.p2maven.InstallableUnitProvider;
import org.eclipse.tycho.p2maven.InstallableUnitPublisher;
import org.eclipse.tycho.p2maven.actions.BundleDependenciesAction;

@Component(role = InstallableUnitProvider.class, hint = "custom-bundle")
public class CustomBundleInstallableUnitProvider implements InstallableUnitProvider {

	@Requirement
	private InstallableUnitPublisher publisher;

	@Override
	public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
			throws CoreException {
		List<BundleDependenciesAction> actions = CustomBundleP2MetadataProvider.getCustomArtifacts(project)
				.map(artifact -> new BundleDependenciesAction(artifact.getLocation(), OptionalResolutionAction.REQUIRE))
				.toList();
		return publisher.publishMetadata(actions);
	}

}
