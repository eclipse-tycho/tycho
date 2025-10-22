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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.bnd;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.UnmodifiableDependencyMetadata;
import org.eclipse.tycho.resolver.InstallableUnitProvider;
import org.eclipse.tycho.resolver.P2MetadataProvider;

@Named(TychoConstants.PDE_BND)
@Singleton
public class BndP2MetadataProvider implements P2MetadataProvider {

    @Inject
    @Named(TychoConstants.PDE_BND)
    InstallableUnitProvider installableUnitProvider;

    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        Set<IInstallableUnit> units;
        try {
            units = Set.copyOf(installableUnitProvider.getInstallableUnits(project, session));
        } catch (CoreException e) {
            return Collections.emptyMap();
        }
        if (units.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.of(TychoConstants.PDE_BND,
                new UnmodifiableDependencyMetadata(units, DependencyMetadataType.INITIAL));
    }

}
