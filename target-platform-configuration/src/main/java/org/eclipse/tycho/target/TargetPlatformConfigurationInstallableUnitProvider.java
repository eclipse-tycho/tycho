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
package org.eclipse.tycho.target;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.resolver.TargetPlatformConfigurationReader;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.resolver.InstallableUnitProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides additional requirements defined in the target platform configuration
 *
 */
@Singleton
@Named("target")
public class TargetPlatformConfigurationInstallableUnitProvider implements InstallableUnitProvider {

    @Inject
    private TargetPlatformConfigurationReader configurationReader;

    @Inject
    private TychoProjectManager projectManager;

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
        if (tychoProject.isEmpty()) {
            //not a tycho project...
            return Collections.emptyList();
        }
        TargetPlatformConfiguration configuration = configurationReader.getTargetPlatformConfiguration(session,
                project);
        List<IRequirement> extraRequirements = configuration.getAdditionalArtifacts().stream()
                .map(key -> createRequirementFor(key.getType(), key.getId(), new VersionRange(key.getVersion())))
                .filter(Objects::nonNull).toList();
        if (extraRequirements.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(createUnitRequiring(extraRequirements));
    }

    private static IRequirement createRequirementFor(String type, String id, VersionRange versionRange) {
        return switch (type) {
        case ArtifactType.TYPE_ECLIPSE_PLUGIN -> MetadataFactory
                .createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, id, versionRange, null, false, true);
        case ArtifactType.TYPE_ECLIPSE_FEATURE -> MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
                id + ".feature.group", versionRange, null, false, true);
        case ArtifactType.TYPE_INSTALLABLE_UNIT -> MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
                id, versionRange, null, false, true);
        case ArtifactType.TYPE_ECLIPSE_PRODUCT -> MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
                id, versionRange, null, false, true);
        default -> null;
        };
    }

    private static IInstallableUnit createUnitRequiring(Collection<IRequirement> requirements) {
        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        result.setId("target-platform-extra-requirements-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
        result.addRequirements(requirements);
        return MetadataFactory.createInstallableUnit(result);
    }

}
