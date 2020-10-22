/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.zipcomparator.internal;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public class SimpleArtifactDelta implements ArtifactDelta {

    private final String message;
    private final String baseline;
    private final String reactor;

    public SimpleArtifactDelta(String message) {
        this(message, null, null);
    }

    public SimpleArtifactDelta(String message, String baseline, String reactor) {
        this.baseline = baseline;
        this.reactor = reactor;
        if (message == null) {
            throw new IllegalArgumentException();
        }
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDetailedMessage() {
        return message;
    }

    public String getBaseline() {
        return baseline;
    }

    public String getReactor() {
        return reactor;
    }
}
