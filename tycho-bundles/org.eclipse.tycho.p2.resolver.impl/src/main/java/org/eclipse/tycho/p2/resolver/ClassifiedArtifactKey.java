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
package org.eclipse.tycho.p2.resolver;

import java.util.Objects;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class ClassifiedArtifactKey {
    public final ArtifactKey artifactKey;
    public final String classifier;

    public ClassifiedArtifactKey(ArtifactKey key, String classifier) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.artifactKey = key;
        this.classifier = classifier;
    }

    public ClassifiedArtifactKey(IArtifactFacade artifact) {
        this(new DefaultArtifactKey(artifact.getPackagingType(), artifact.getArtifactId(), artifact.getVersion()),
                artifact.getClassifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactKey, classifier);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof ClassifiedArtifactKey other //
                        && Objects.equals(this.artifactKey, other.artifactKey) //
                        && Objects.equals(this.classifier, other.classifier));
    }

}
