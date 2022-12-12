/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.util;

import java.util.Map;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.MavenRepositoryCoordinates;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

public class GAVArtifactDescriptorBase extends ArtifactDescriptor {

    protected final MavenRepositoryCoordinates coordinates;

    // BEGIN construction

    protected GAVArtifactDescriptorBase(IArtifactDescriptor base, MavenRepositoryCoordinates mavenCoordinates,
            boolean storeMavenCoordinates) {
        super(base);

        this.coordinates = mavenCoordinates;
        if (this.coordinates == null)
            throw new NullPointerException();

        if (storeMavenCoordinates) {
            setMavenCoordinateProperties();
        }
    }

    protected GAVArtifactDescriptorBase(IArtifactKey p2Key, MavenRepositoryCoordinates mavenCoordinates,
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
        properties.put(TychoConstants.PROP_GROUP_ID, coordinates.getGroupId());
        properties.put(TychoConstants.PROP_ARTIFACT_ID, coordinates.getArtifactId());
        properties.put(TychoConstants.PROP_VERSION, coordinates.getVersion());
        putOrRemoveOnNull(properties, TychoConstants.PROP_CLASSIFIER, coordinates.getClassifier());
        //This is only for older tycho versions trying to read newver repository formats
        putOrRemoveOnNull(properties, TychoConstants.PROP_EXTENSION, coordinates.getType());
        putOrRemoveOnNull(properties, TychoConstants.PROP_TYPE, coordinates.getType());
    }

    private static void putOrRemoveOnNull(Map<String, String> properties, String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    /**
     * @return the Maven coordinates stored in the properties of the given descriptor, or
     *         <code>null</code>
     */
    public static MavenRepositoryCoordinates readMavenCoordinateProperties(IArtifactDescriptor descriptor) {
        GAV gav = RepositoryLayoutHelper.getGAV(descriptor.getProperties());
        if (gav == null) {
            return null;
        }

        String classifier = RepositoryLayoutHelper.getClassifier(descriptor.getProperties());
        String extension = RepositoryLayoutHelper.getType(descriptor.getProperties());
        return new MavenRepositoryCoordinates(gav, classifier, extension);
    }

    // END construction

    /**
     * Returns the Maven coordinates of the artifact this descriptor points to.
     * 
     * @return The Maven coordinates; never <code>null</code>
     */
    public final MavenRepositoryCoordinates getMavenCoordinates() {
        return coordinates;
    }

}
