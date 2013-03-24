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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

abstract class ArtifactTransferPolicyBase extends ArtifactTransferPolicy {

    @Override
    public final IArtifactDescriptor pickFormat(IArtifactDescriptor[] formats) throws IllegalArgumentException {
        if (formats.length < 1) {
            throw new IllegalArgumentException("List of artifact formats is empty");
        }
        List<IArtifactDescriptor> sortedFormats = sortFormatsByPreference(formats);
        return sortedFormats.get(0);
    }

    @Override
    public final List<IArtifactDescriptor> sortFormatsByPreference(IArtifactDescriptor[] artifactDescriptors) {
        LinkedList<IArtifactDescriptor> result = new LinkedList<IArtifactDescriptor>();

        IArtifactDescriptor canonical = null;
        IArtifactDescriptor packed = null;
        for (IArtifactDescriptor descriptor : artifactDescriptors) {
            if (isCanonicalFormat(descriptor)) {
                canonical = descriptor;
            } else if (isPack200Format(descriptor)) {
                packed = descriptor;
            } else {
                result.add(descriptor);
            }
        }
        insertCanonicalAndPacked(canonical, packed, result);
        return result;
    }

    /**
     * Inserts the canonical and packed descriptor in preferred order.
     * 
     * @param canonical
     *            The canonical descriptor to be inserted in preferred order, or <code>null</code>
     *            if the list to be sorted did not contain a canonical descriptor.
     * @param packed
     *            The packed descriptor to be inserted in preferred order, or <code>null</code> if
     *            the list to be sorted did not contain a packed descriptor.
     * @param list
     *            All other descriptors from the list to be sorted. To be completed by the canonical
     *            and packed descriptors (if available).
     */
    protected abstract void insertCanonicalAndPacked(IArtifactDescriptor canonical, IArtifactDescriptor packed,
            LinkedList<IArtifactDescriptor> list);

}
