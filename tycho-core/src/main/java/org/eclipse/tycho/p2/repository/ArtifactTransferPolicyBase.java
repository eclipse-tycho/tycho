/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP SE and others.
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
package org.eclipse.tycho.p2.repository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

abstract class ArtifactTransferPolicyBase extends ArtifactTransferPolicy {

    @Override
    public final List<IArtifactDescriptor> sortFormatsByPreference(IArtifactDescriptor[] artifactDescriptors) {
        LinkedList<IArtifactDescriptor> result = new LinkedList<>();

        List<IArtifactDescriptor> canonical = new ArrayList<>();
        for (IArtifactDescriptor descriptor : artifactDescriptors) {
            if (isCanonicalFormat(descriptor)) {
                canonical.add(descriptor);
            } else {
                result.add(descriptor);
            }
        }
        insertCanonical(canonical, result);
        return result;
    }

    /**
     * Inserts the canonical descriptor in preferred order.
     * 
     * @param canonical
     *            The canonical descriptor to be inserted in preferred order, or <code>null</code>
     *            if the list to be sorted did not contain a canonical descriptor.
     * @param list
     *            All other descriptors from the list to be sorted. To be completed by the canonical
     *            descriptors (if available).
     */
    protected abstract void insertCanonical(List<IArtifactDescriptor> canonical, LinkedList<IArtifactDescriptor> list);

}
