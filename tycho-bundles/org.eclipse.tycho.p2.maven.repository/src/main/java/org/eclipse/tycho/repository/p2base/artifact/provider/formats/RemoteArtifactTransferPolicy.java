/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

final class RemoteArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonicalAndPacked(List<IArtifactDescriptor> canonical, List<IArtifactDescriptor> packed,
            LinkedList<IArtifactDescriptor> list) {
        boolean isPack200able = Runtime.version().feature() < 14;
        if (canonical != null) {
            list.addAll(0, canonical);
        }
        if (packed != null) {
            // still consider for transtive inclusion in features on Java 14+
            list.addAll(canonical == null || isPack200able ? 0 : canonical.size(), packed);
        }
    }

}
