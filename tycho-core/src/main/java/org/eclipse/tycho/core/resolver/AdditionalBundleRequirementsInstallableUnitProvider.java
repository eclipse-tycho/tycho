/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich. and others.
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
package org.eclipse.tycho.core.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2maven.InstallableUnitProvider;

/**
 * This provides P2 visible meta-data for bundles that are not expressed in the manifest (e.g.
 * build.properties derived)
 *
 */
@Component(role = InstallableUnitProvider.class, hint = "bundle-requirement")
public class AdditionalBundleRequirementsInstallableUnitProvider implements InstallableUnitProvider {
    @Requirement
    private Logger logger;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        if (projectTypes.get(project.getPackaging()) instanceof BundleProject) {
            ReactorProject reactorProject = DefaultReactorProject.adapt(project);
            BuildProperties buildProperties = reactorProject.getBuildProperties();
            List<IRequirement> additionalBundleRequirements = buildProperties.getAdditionalBundles().stream()
                    .map(bundleName -> MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
                            bundleName, VersionRange.emptyRange, null, true, true))
                    .toList();
            return createIU(additionalBundleRequirements);
        }
        return Collections.emptyList();
    }

    private Collection<IInstallableUnit> createIU(List<IRequirement> additionalBundleRequirements) {
        if (additionalBundleRequirements.isEmpty()) {
            return Collections.emptyList();
        }
        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        result.setId("additional-bundle-requirements-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
        result.addRequirements(additionalBundleRequirements);
        return List.of(MetadataFactory.createInstallableUnit(result));
    }

}
