/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testutil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

@SuppressWarnings("restriction")
public class InstallableUnitUtil {

    static final String IU_CAPABILITY_NS = "org.eclipse.equinox.p2.iu"; // see IInstallableUnit.NAMESPACE_IU_ID;
    static final String BUNDLE_CAPABILITY_NS = "osgi.bundle"; // see BundlesAction.CAPABILITY_NS_OSGI_BUNDLE
    static final String PRODUCT_TYPE_PROPERTY = "org.eclipse.equinox.p2.type.product"; // see InstallableUnitDescription.PROP_TYPE_PRODUCT;
    static final String FEATURE_TYPE_PROPERTY = "org.eclipse.equinox.p2.type.group"; // see InstallableUnitDescription.PROP_TYPE_GROUP;

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

    public static IInstallableUnit createIUArtifact(String id, String version, String artifactId,
            String artifactVersion) {
        InstallableUnitDescription description = createIuDescription(id, version);
        description.setArtifacts(
                new IArtifactKey[] { new ArtifactKey("type", artifactId, Version.create(artifactVersion)) });
        return MetadataFactory.createInstallableUnit(description);
    }

    public static IInstallableUnit createIURequirement(String id, String version, String requiredId,
            String requiredVersionRange) {
        InstallableUnitDescription description = createIuDescription(id, version);
        final IRequirement requiredCapability = createRequirement(requiredId, requiredVersionRange);
        description.addRequirements(Arrays.asList(requiredCapability));
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

    static IRequirement createRequirement(String requiredId, String requiredVersionRange) {
        return MetadataFactory.createRequirement(IU_CAPABILITY_NS, requiredId, new VersionRange(requiredVersionRange),
                null, false, false, true);
    }

    static IRequirement createStrictRequirement(String requiredId, String requiredVersion) {
        Version parsedVersion = Version.create(requiredVersion);
        return MetadataFactory.createRequirement(IU_CAPABILITY_NS, requiredId,
                new VersionRange(parsedVersion, true, parsedVersion, true), null, false, false, true);
    }
}
