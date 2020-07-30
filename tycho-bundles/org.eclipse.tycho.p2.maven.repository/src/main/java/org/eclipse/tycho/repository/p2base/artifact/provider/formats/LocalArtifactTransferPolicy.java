/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider.formats;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

class LocalArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonicalAndPacked(List<IArtifactDescriptor> canonical, List<IArtifactDescriptor> packed,
            LinkedList<IArtifactDescriptor> list) {
        if (packed != null) {
            list.addAll(0, packed);
        }
        if (canonical != null) {
            // canonical is most preferred -> add at head of the list
            list.addAll(0, canonical);
        }
    }

}
