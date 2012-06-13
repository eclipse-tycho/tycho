/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public class SimpleArtifactDelta implements ArtifactDelta {

    private final String message;

    public SimpleArtifactDelta(String message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getDetailedMessage() {
        return message;
    }
}
