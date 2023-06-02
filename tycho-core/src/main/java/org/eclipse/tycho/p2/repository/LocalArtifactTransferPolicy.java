/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class LocalArtifactTransferPolicy extends ArtifactTransferPolicyBase {

    @Override
    protected void insertCanonical(List<IArtifactDescriptor> canonical, LinkedList<IArtifactDescriptor> list) {
        if (canonical != null) {
            // canonical is most preferred -> add at head of the list
            list.addAll(0, canonical);
        }
    }

}
