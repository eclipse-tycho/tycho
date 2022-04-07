/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Set;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;

public class DefaultFeatureDescription extends DefaultArtifactDescriptor implements FeatureDescription {
    private Feature feature;

    private FeatureRef featureRef;

    public DefaultFeatureDescription(ArtifactKey key, File location, ReactorProject project, String classifier,
            Feature feature, FeatureRef featureRef, Set<Object> installableUnits) {
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
