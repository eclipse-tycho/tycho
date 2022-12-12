/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.artifact.provider;

import static org.eclipse.tycho.p2.repository.ArtifactTransferPolicy.isCanonicalFormat;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.tycho.p2.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.p2.artifact.provider.streaming.IRawArtifactSink;

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
        if (artifactSink instanceof IRawArtifactSink rawSink
                && !isCanonicalFormat(rawSink.getArtifactFormatToBeWritten())) {
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
