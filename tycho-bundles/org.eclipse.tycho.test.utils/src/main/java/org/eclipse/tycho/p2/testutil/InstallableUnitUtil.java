/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.testutil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    private static final String IU_CAPABILITY_NS = "org.eclipse.equinox.p2.iu"; // see IInstallableUnit.NAMESPACE_IU_ID;
    private static final String BUNDLE_CAPABILITY_NS = "osgi.bundle"; // see BundlesAction.CAPABILITY_NS_OSGI_BUNDLE
    private static final String PRODUCT_TYPE_PROPERTY = "org.eclipse.equinox.p2.type.product"; // see InstallableUnitDescription.PROP_TYPE_PRODUCT;
    private static final String FEATURE_TYPE_PROPERTY = "org.eclipse.equinox.p2.type.group"; // see InstallableUnitDescription.PROP_TYPE_GROUP;

    public static String DEFAULT_VERSION = "0.0.20";

    public static IInstallableUnit createIU(String versionedId) {
        int separator = versionedId.indexOf('/');
        if (separator > 0) {
            return createIU(versionedId.substring(0, separator), versionedId.substring(separator + 1));
        } else {
            return createIU(versionedId, DEFAULT_VERSION);
        }
    }

    public static IInstallableUnit createIU(String id, String version) {
        InstallableUnitDescription description = createIuDescription(id, version);
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createBundleIU(String bundleId, String version) {
        InstallableUnitDescription description = createIuDescription(bundleId, version);
        description.addProvidedCapabilities(createProvidedCapability(BUNDLE_CAPABILITY_NS, bundleId, version));
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createProductIU(String productId, String version) {
        InstallableUnitDescription description = createIuDescription(productId, version);
        description.setProperty(PRODUCT_TYPE_PROPERTY, Boolean.toString(true));
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createFeatureIU(String featureId, String version) {
        InstallableUnitDescription description = createIuDescription(featureId + ".feature.group", version);
        description.setProperty(FEATURE_TYPE_PROPERTY, Boolean.toString(true));
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createIUWithCapabilitiesAndFilter(String id, String version,
            Collection<IProvidedCapability> capabilities, String filter) {
        InstallableUnitDescription description = createIuDescription(id, version);
        description.addProvidedCapabilities(capabilities);
        description.setFilter(filter);
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
        final RequiredCapability requiredCapability = new RequiredCapability(IU_CAPABILITY_NS, requiredId,
                new VersionRange(requiredVersionRange), null, false, true);
        description.addRequirements(Arrays.<IRequirement> asList(requiredCapability));
        return MetadataFactory.createInstallableUnit(description);
    }

    private static InstallableUnitDescription createIuDescription(String id, String version) {
        InstallableUnitDescription description = new InstallableUnitDescription();
        description.setId(id);
        description.setVersion(Version.create(version));
        description.addProvidedCapabilities(createProvidedCapability(IU_CAPABILITY_NS, id, version));
        return description;
    }

    private static List<IProvidedCapability> createProvidedCapability(String namespace, String name, String version) {
        return Arrays.<IProvidedCapability> asList(new ProvidedCapability(namespace, name, Version.create(version)));
    }
}
