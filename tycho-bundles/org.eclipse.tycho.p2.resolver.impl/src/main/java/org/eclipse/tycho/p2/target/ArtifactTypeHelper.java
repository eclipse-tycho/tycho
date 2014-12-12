/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;

@SuppressWarnings("restriction")
public class ArtifactTypeHelper {

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
        return MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, id, versionRange, null,
                false, true); // optional=false, multiple=true
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

}
