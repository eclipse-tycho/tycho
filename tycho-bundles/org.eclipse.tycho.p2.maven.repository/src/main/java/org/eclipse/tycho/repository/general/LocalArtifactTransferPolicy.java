/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.general;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class LocalArtifactTransferPolicy extends ArtifactTransferPolicy {

    @Override
    public IArtifactDescriptor pickFormat(IArtifactDescriptor[] formats) throws IllegalArgumentException {
        if (formats.length < 1) {
            throw new IllegalArgumentException("List of artifact formats is empty");
        }

        // prefer canonical then pack200
        IArtifactDescriptor pack200 = null;
        for (IArtifactDescriptor format : formats) {
            if (isCanonicalFormat(format)) {
                return format;
            } else if (isPack200Format(format)) {
                pack200 = format;
            }
        }

        if (pack200 != null) {
            return pack200;
        } else {
            // there were only formats other than pack200 and canonical -> this case should be rare
            return formats[0];
        }
    }

}
