/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.util;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MavenArtifactCoordinates;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

public class GAVArtifactDescriptorBase extends ArtifactDescriptor {

    protected final MavenArtifactCoordinates coordinates;

    // BEGIN construction

    protected GAVArtifactDescriptorBase(IArtifactDescriptor base, MavenArtifactCoordinates mavenCoordinates,
            boolean storeMavenCoordinates) {
        super(base);

        this.coordinates = mavenCoordinates;
        if (this.coordinates == null)
            throw new NullPointerException();

        if (storeMavenCoordinates) {
            setMavenCoordinateProperties();
        }
    }

    protected GAVArtifactDescriptorBase(IArtifactKey p2Key, MavenArtifactCoordinates mavenCoordinates,
            boolean storeMavenCoordinates) {
        super(p2Key);

        this.coordinates = mavenCoordinates;
        if (this.coordinates == null)
            throw new NullPointerException();

        if (storeMavenCoordinates) {
            setMavenCoordinateProperties();
        }
    }

    protected final void setMavenCoordinateProperties() {
        properties.put(RepositoryLayoutHelper.PROP_GROUP_ID, coordinates.getGroupId());
        properties.put(RepositoryLayoutHelper.PROP_ARTIFACT_ID, coordinates.getArtifactId());
        properties.put(RepositoryLayoutHelper.PROP_VERSION, coordinates.getVersion());
        properties.put(RepositoryLayoutHelper.PROP_CLASSIFIER, coordinates.getClassifier());
        properties.put(RepositoryLayoutHelper.PROP_EXTENSION, coordinates.getExtension());

        // TODO replace "jar" with null (if this is the only place handing the Maven properties)
    }

    /**
     * @return the Maven coordinates stored in the given descriptor, or <code>null</code>
     */
    public static MavenArtifactCoordinates readMavenCoordinateProperties(IArtifactDescriptor descriptor) {
        GAV gav = RepositoryLayoutHelper.getGAV(descriptor.getProperties());
        if (gav == null) {
            return null;
        }

        String classifier = RepositoryLayoutHelper.getClassifier(descriptor.getProperties());
        String extension = RepositoryLayoutHelper.getExtension(descriptor.getProperties());
        return new MavenArtifactCoordinates(gav, classifier, extension);
    }

    // END construction

    /**
     * Returns the Maven coordinates of the artifact this descriptor points to.
     * 
     * @return The Maven coordinates; never <code>null</code>
     */
    public final MavenArtifactCoordinates getMavenCoordinates() {
        return coordinates;
    }

}
