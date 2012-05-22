/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class ClassifiedLocation {
    private final File location;

    private final String classifier;

    public ClassifiedLocation(File location, String classifier) {
        if (location == null) {
            throw new NullPointerException();
        }

        this.location = location;
        this.classifier = classifier;
    }

    public ClassifiedLocation(IArtifactFacade artifact) {
        this(artifact.getLocation(), artifact.getClassifier());
    }

    public File getLocation() {
        return location;
    }

    public String getClassifier() {
        return classifier;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(location.getAbsolutePath());
        if (classifier != null) {
            sb.append('[').append(classifier).append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = location.hashCode();
        hash = 17 * hash + (classifier != null ? classifier.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ClassifiedLocation)) {
            return false;
        }

        ClassifiedLocation other = (ClassifiedLocation) obj;

        return eq(this.location, other.location) && eq(this.classifier, other.classifier);
    }

    static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

}
