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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy.isCanonicalFormat;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

public final class ArtifactProviderImplUtilities {

    /**
     * Checks if the given sink can be written to. This may not be the case if the sink has already
     * been committed.
     * 
     * @throws IllegalArgumentException
     *             if the check fails.
     * @see IArtifactSink#canBeginWrite()
     */
    public static void canWriteToSink(IArtifactSink sink) {
        if (!sink.canBeginWrite()) {
            throw new IllegalArgumentException(
                    "Cannot write to artifact sink. Has the sink already been used for a different read operation?");
        }
    }

    /**
     * Checks if the given sink takes an artifact in canonical format. This may not be the case for
     * instances of the sub-type {@link IRawArtifactSink}.
     * 
     * @throws IllegalArgumentException
     *             if the check fails
     */
    public static void canWriteCanonicalArtifactToSink(IArtifactSink artifactSink) throws IllegalArgumentException {
        if (artifactSink instanceof IRawArtifactSink
                && !isCanonicalFormat(((IRawArtifactSink) artifactSink).getArtifactFormatToBeWritten())) {
            throw new IllegalArgumentException(
                    "Artifact should not be written in canonical format to a sink expecting a non-canonical format. Did you mean to call getRawArtifact?");
        }
    }

    public static MultiStatus createMultiStatusWithFixedSeverity(int severity, String pluginId, List<IStatus> children,
            String message) {
        return new FixedSeverityMultiStatus(severity, pluginId, children, message);
    }

    private static class FixedSeverityMultiStatus extends MultiStatus {

        public FixedSeverityMultiStatus(int severity, String pluginId, List<IStatus> children, String message) {
            super(pluginId, 0, children.toArray(new IStatus[children.size()]), message, null);
            setSeverity(severity);
        }

    }
}
