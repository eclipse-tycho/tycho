/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import java.util.Map;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MavenArtifactCoordinates;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.util.GAVArtifactDescriptorBase;

public class GAVArtifactDescriptor extends GAVArtifactDescriptorBase {

    /**
     * Creates a new {@link GAVArtifactDescriptor} instance based on the given p2 artifact
     * descriptor. If the given descriptor contains Maven coordinates as properties, these values
     * are used. Otherwise, fake Maven coordinates with a groupId starting with "p2" will be derived
     * from the p2 artifact key.
     * 
     * @param base
     *            The artifact descriptor to be copy-converted into a
     *            <code>GAVArtifactDescriptor</code>
     */
    public GAVArtifactDescriptor(IArtifactDescriptor base) {
        super(base, readOrDeriveMavenCoordinates(base), false);
    }

    /**
     * Creates a new {@link GAVArtifactDescriptorBase} instance with the given p2 artifact key. The
     * instance will have fake Maven coordinates with a groupId starting with "p2". These are
     * derived from the key.
     * 
     * @param key
     *            The artifact key for the newly created instance
     */
    public GAVArtifactDescriptor(IArtifactKey key) {
        super(key, getP2DerivedCoordinates(key, null), false);
    }

    /**
     * @return fake Maven coordinates derived from the given key; never <code>null</code>
     */
    private static MavenArtifactCoordinates getP2DerivedCoordinates(IArtifactKey key, Map<String, String> properties) {
        GAV gav = RepositoryLayoutHelper.getP2Gav(key.getClassifier(), key.getId(), key.getVersion().toString());
        String classifier = null;
        String extension = RepositoryLayoutHelper.DEFAULT_EXTERNSION;

        if (properties != null && IArtifactDescriptor.FORMAT_PACKED.equals(properties.get(IArtifactDescriptor.FORMAT))) {
            classifier = RepositoryLayoutHelper.PACK200_CLASSIFIER;
            extension = RepositoryLayoutHelper.PACK200_EXTENSION;
        }

        return new MavenArtifactCoordinates(gav, classifier, extension);
    }

    private static MavenArtifactCoordinates readOrDeriveMavenCoordinates(IArtifactDescriptor base) {
        MavenArtifactCoordinates result = readMavenCoordinateProperties(base);
        if (result == null) {
            result = getP2DerivedCoordinates(base.getArtifactKey(), base.getProperties());
        }
        return result;
    }

}
