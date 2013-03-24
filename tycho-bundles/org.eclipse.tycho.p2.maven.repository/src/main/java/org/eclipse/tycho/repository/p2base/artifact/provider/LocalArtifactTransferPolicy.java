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
 * {@link ArtifactTransferPolicy} optimized for artifacts stored in the local file system. A
 * provider with this policy will use the canonical format (if available) when asked for an
 * artifact, avoiding unnecessary pack200 decompression operations.
 */
// TODO instantiate via factory?
public class LocalArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonicalAndPacked(IArtifactDescriptor canonical, IArtifactDescriptor packed,
            LinkedList<IArtifactDescriptor> list) {
        if (packed != null) {
            list.add(0, packed);
        }
        if (canonical != null) {
            // canonical is most preferred -> add at head of the list
            list.add(0, canonical);
        }
    }

}
