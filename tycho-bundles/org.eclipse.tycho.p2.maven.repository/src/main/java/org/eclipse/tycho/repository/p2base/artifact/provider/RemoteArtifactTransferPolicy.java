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

import java.util.LinkedList;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * {@link ArtifactTransferPolicy} optimized for artifacts stored on a remote server. A provider with
 * this policy will internally use the size-optimized pack200 format (if available) when asked for
 * an artifact. This policy leads to a lower network usage, at the cost of a higher CPU usage.
 */
// TODO instantiate via factory?
public final class RemoteArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonicalAndPacked(IArtifactDescriptor canonical, IArtifactDescriptor packed,
            LinkedList<IArtifactDescriptor> list) {
        if (canonical != null) {
            list.add(0, canonical);
        }
        if (packed != null) {
            // packed is most preferred -> add at head of the list
            list.add(0, packed);
        }
    }

}
