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

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

final class RemoteArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonicalAndPacked(IArtifactDescriptor canonical, IArtifactDescriptor packed,
            LinkedList<IArtifactDescriptor> list) {
        boolean isPack200able = Runtime.version().feature() < 14;
        if (packed != null && isPack200able) {
            // packed is most preferred on Java 14+ -> add it first
            list.add(packed);
        }
        if (canonical != null) {
            list.add(0, canonical);
        }
        if (packed != null && !isPack200able) {
            // still consider for transtive inclusion in features on Java 14+
            list.add(packed);
        }

    }

}
