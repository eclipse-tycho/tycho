/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;

public class DefaultFeatureDescription extends DefaultArtifactDescriptor implements FeatureDescription {
    private final Feature feature;

    private final FeatureRef featureRef;

    public DefaultFeatureDescription(ArtifactKey key, File location, ReactorProject project, String classifier,
            Feature feature, FeatureRef featureRef, Collection<IInstallableUnit> installableUnits) {
        super(key, location, project, classifier, installableUnits);
        this.feature = feature;
        this.featureRef = featureRef;
    }

    @Override
    public FeatureRef getFeatureRef() {
        return featureRef;
    }

    @Override
    public Feature getFeature() {
        return feature;
    }
}
