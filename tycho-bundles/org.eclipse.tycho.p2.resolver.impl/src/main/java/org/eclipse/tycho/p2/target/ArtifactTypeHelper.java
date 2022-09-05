/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.ArtifactType.TYPE_BUNDLE_FRAGMENT;
import static org.eclipse.tycho.ArtifactType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.ArtifactType.TYPE_ECLIPSE_PLUGIN;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.IllegalArtifactReferenceException;

@SuppressWarnings("restriction")
public class ArtifactTypeHelper {

    // p2 installable units

    /**
     * Returns a query matching the installable units representing the specified Eclipse
     * artifact(s).
     * 
     * @param type
     *            Eclipse artifact type as defined in Tycho's {@link ArtifactType}
     */
    public static IQuery<IInstallableUnit> createQueryFor(String type, String id, VersionRange versionRange) {

        if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            return QueryUtil.createMatchQuery(createBundleRequirement(id, versionRange).getMatches());

        } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
            return QueryUtil.createPipeQuery(QueryUtil.createIUQuery(id + ".feature.group", versionRange),
                    QueryUtil.createIUGroupQuery());

        } else if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type)) {
            return QueryUtil.createPipeQuery(QueryUtil.createIUQuery(id, versionRange),
                    QueryUtil.createIUProductQuery());

        } else if (ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
            return QueryUtil.createIUQuery(id, versionRange);

        } else {

            IRequirement requirement = MetadataFactory.createRequirement(type, id, versionRange, null,
                    1 /* min */, Integer.MAX_VALUE /* max */, false /* greedy */);
            return QueryUtil.createMatchQuery(requirement.getMatches());
        }
    }

    public static IRequirement createRequirementFor(String type, String id, VersionRange versionRange)
            throws IllegalArtifactReferenceException {
        if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            return createBundleRequirement(id, versionRange);

        } else if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(type)) {
            return createFeatureRequirement(id, versionRange);

        } else if (ArtifactType.TYPE_INSTALLABLE_UNIT.equals(type)) {
            return createProductRequirement(id, versionRange);

        } else if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type)) {
            return createProductRequirement(id, versionRange);

        } else {
            throw new IllegalArtifactReferenceException("Unknown artifact type \"" + type + "\"");
        }
    }

    private static IRequirement createBundleRequirement(String id, VersionRange versionRange) {
        return MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, id, versionRange, null, false,
                true); // optional=false, multiple=true
    }

    private static IRequirement createFeatureRequirement(String id, VersionRange versionRange) {
        // features don't provide a dedicated capability; they can only be found by their name with the conventional suffix
        return createIURequirement(id + ".feature.group", versionRange);
        // TODO make ".feature.group" a constant in FeaturesAction
    }

    private static IRequirement createProductRequirement(String id, VersionRange versionRange) {
        // products don't provide a dedicated capability; they cannot be distinguished from other IUs
        return createIURequirement(id, versionRange);
    }

    private static IRequirement createIURequirement(String id, VersionRange versionRange) {
        return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, versionRange, null, false, true);
    }

    public static org.eclipse.tycho.ArtifactKey toTychoArtifact(IInstallableUnit unit) {
        // TODO 428889 unit test & add more cases
        if (Boolean.parseBoolean(unit.getProperty(InstallableUnitDescription.PROP_TYPE_GROUP))) {
            // TODO 428889 check suffix
            String id = unit.getId();
            return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_FEATURE,
                    id.substring(0, id.length() - ".feature.group".length()), unit.getVersion().toString());
        }
        throw new IllegalArgumentException(unit.toString());
    }

    // p2 artifacts

    public static IArtifactKey toP2ArtifactKey(org.eclipse.tycho.ArtifactKey artifact) {
        if (TYPE_ECLIPSE_PLUGIN.equals(artifact.getType()) || TYPE_BUNDLE_FRAGMENT.equals(artifact.getType())) {
            return createP2ArtifactKey(PublisherHelper.OSGI_BUNDLE_CLASSIFIER, artifact);

        } else if (TYPE_ECLIPSE_FEATURE.equals(artifact.getType())) {
            return createP2ArtifactKey(PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER, artifact);

        } else {
            // other artifacts don't have files that can be referenced by their Eclipse coordinates
            return null;
        }
    }

    private static IArtifactKey createP2ArtifactKey(String type, org.eclipse.tycho.ArtifactKey artifact) {
        return new org.eclipse.equinox.internal.p2.metadata.ArtifactKey(type, artifact.getId(),
                Version.parseVersion(artifact.getVersion()));
    }

}
