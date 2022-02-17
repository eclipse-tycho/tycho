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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

abstract class ArtifactTransferPolicyBase extends ArtifactTransferPolicy {

    @Override
    public final List<IArtifactDescriptor> sortFormatsByPreference(IArtifactDescriptor[] artifactDescriptors) {
        LinkedList<IArtifactDescriptor> result = new LinkedList<>();

        List<IArtifactDescriptor> canonical = new ArrayList<>();
        List<IArtifactDescriptor> packed = new ArrayList<>();
        for (IArtifactDescriptor descriptor : artifactDescriptors) {
            if (isCanonicalFormat(descriptor)) {
                canonical.add(descriptor);
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
    protected abstract void insertCanonicalAndPacked(List<IArtifactDescriptor> canonical,
            List<IArtifactDescriptor> packed, LinkedList<IArtifactDescriptor> list);

}
