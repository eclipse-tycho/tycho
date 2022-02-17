/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP SE and others.
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

import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.IArtifactProvider;

/**
 * Policy for picking the internally used artifact format when obtaining an artifact. Different
 * policies may optimize for network bandwidth or CPU usage.
 * 
 * @see IArtifactProvider#getArtifact(IArtifactKey, OutputStream, IProgressMonitor)
 */
public abstract class ArtifactTransferPolicy {

    /**
     * Sorts a list of artifact formats by the order in which they should be tried to be used for a
     * (non-raw) artifact read operation.
     * 
     * @param formats
     *            the list of raw artifact formats available from a provider
     * @return the list of formats, sorted by preference
     */
    public abstract List<IArtifactDescriptor> sortFormatsByPreference(IArtifactDescriptor[] artifactDescriptors);

    public static boolean isCanonicalFormat(IArtifactDescriptor format) {
        return null == format.getProperty(IArtifactDescriptor.FORMAT);
    }

}
