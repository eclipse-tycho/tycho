/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.util.Arrays;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

@SuppressWarnings({ "restriction", "nls" })
public class InstallableUnitUtil {

    public static IInstallableUnit createIU(String id, String version) {
        InstallableUnitDescription description = createIuDescription(id, version);
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createIUCapability(String id, String version, String capabilityId,
            String capabilityVersion) {
        InstallableUnitDescription description = createIuDescription(id, version);
        description.addProvidedCapabilities(Arrays.<IProvidedCapability> asList(new ProvidedCapability(
                IInstallableUnit.NAMESPACE_IU_ID, capabilityId, Version.create(capabilityVersion))));
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createIUArtifact(String id, String version, String artifactId, String artifactVersion) {
        InstallableUnitDescription description = createIuDescription(id, version);
        description.setArtifacts(new IArtifactKey[] { new ArtifactKey("type", artifactId, Version
                .create(artifactVersion)) });
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createIURequirement(String id, String version, String requiredId,
            String requiredVersionRange) {
        InstallableUnitDescription description = createIuDescription(id, version);
        final RequiredCapability requiredCapability = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID,
                requiredId, new VersionRange(requiredVersionRange), null, false, true);
        description.addRequirements(Arrays.<IRequirement> asList(requiredCapability));
        return MetadataFactory.createInstallableUnit(description);
    }

    private static InstallableUnitDescription createIuDescription(String id, String version) {
        InstallableUnitDescription description = new InstallableUnitDescription();
        description.setId(id);
        description.setVersion(Version.create(version));
        return description;
    }
}
