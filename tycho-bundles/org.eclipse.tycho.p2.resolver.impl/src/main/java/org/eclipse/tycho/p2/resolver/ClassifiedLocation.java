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

import java.io.File;
import java.util.Objects;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class ClassifiedLocation {
    public final File location;
    public final String classifier;

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

    @Override
    public int hashCode() {
        return Objects.hash(location, classifier);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof ClassifiedLocation other && //
                        Objects.equals(this.location, other.location) && //
                        Objects.equals(this.classifier, other.classifier));
    }

}
