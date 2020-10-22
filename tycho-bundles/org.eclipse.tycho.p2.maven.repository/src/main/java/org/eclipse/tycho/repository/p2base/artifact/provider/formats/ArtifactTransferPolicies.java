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
package org.eclipse.tycho.repository.p2base.artifact.provider.formats;

public final class ArtifactTransferPolicies {

    /**
     * Returns the {@link ArtifactTransferPolicy} optimized for artifacts stored in the local file
     * system. A provider with this policy will use the canonical format (if available) when asked
     * for an artifact, avoiding unnecessary pack200 decompression operations.
     */
    public static ArtifactTransferPolicy forLocalArtifacts() {
        return new LocalArtifactTransferPolicy();
    }

    /**
     * Returns the {@link ArtifactTransferPolicy} optimized for artifacts stored on a remote server.
     * A provider with this policy will internally use the size-optimized pack200 format (if
     * available) when asked for an artifact. This policy leads to a lower network usage, at the
     * cost of a higher CPU usage.
     */
    public static ArtifactTransferPolicy forRemoteArtifacts() {
        return new RemoteArtifactTransferPolicy();
    }

}
