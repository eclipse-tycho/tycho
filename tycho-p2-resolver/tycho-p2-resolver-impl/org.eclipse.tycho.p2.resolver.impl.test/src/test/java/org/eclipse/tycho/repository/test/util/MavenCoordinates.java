/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test.util;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;

// TODO also use this in productive code
public class MavenCoordinates {
    private GAV gav;
    private String classifier;
    private String extension;

    public MavenCoordinates(GAV gav, String classifier, String extension) {
        this.gav = gav;
        this.classifier = classifier;
        this.extension = extension;
    }

    public MavenCoordinates(IArtifactFacade artifact) {
        this.gav = new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        this.classifier = artifact.getClassidier();
        this.extension = artifact.getPackagingType(); // TODO this is probably wrong
    }

    @Override
    public int hashCode() {
        final int prime = 53;
        int result = 1;
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenCoordinates other = (MavenCoordinates) obj;

        return isEqual(gav, other.gav) && isEqual(classifier, other.classifier) && isEqual(extension, other.extension);
    }

    private <T> boolean isEqual(T left, T right) {
        if (left == right)
            return true;
        if (left == null)
            return false;
        return left.equals(right);
    }

    @Override
    public String toString() {
        return gav.toExternalForm() + ":" + classifier + ":" + extension;
    }

}
