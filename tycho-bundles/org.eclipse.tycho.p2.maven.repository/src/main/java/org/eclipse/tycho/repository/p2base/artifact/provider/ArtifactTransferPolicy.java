/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Policy for picking the internally used artifact format when obtaining an artifact. Different
 * policies may optimize for network bandwidth or CPU usage.
 * 
 * @see IArtifactProvider#getArtifact(IArtifactKey, OutputStream, IProgressMonitor)
 */
public abstract class ArtifactTransferPolicy {

    /**
     * Returns the preferred format to be used for a (non-raw) artifact read operation.
     * 
     * @param formats
     *            the list of raw artifact formats available from a provider
     * @throws IllegalArgumentException
     *             if the list of formats is empty
     */
    // TODO remove?
    public abstract IArtifactDescriptor pickFormat(IArtifactDescriptor[] formats) throws IllegalArgumentException;

    // TODO javadoc
    public abstract List<IArtifactDescriptor> sortFormatsByPreference(IArtifactDescriptor[] artifactDescriptors);

    public static boolean isCanonicalFormat(IArtifactDescriptor format) {
        return null == format.getProperty(IArtifactDescriptor.FORMAT);
    }

    public static boolean isPack200Format(IArtifactDescriptor format) {
        return IArtifactDescriptor.FORMAT_PACKED.equals(format.getProperty(IArtifactDescriptor.FORMAT));
    }

}
