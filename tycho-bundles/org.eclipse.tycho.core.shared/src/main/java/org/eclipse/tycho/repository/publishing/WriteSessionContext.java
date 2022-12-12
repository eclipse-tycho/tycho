/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.publishing;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * Interface for providing additional information required for publishing artifacts into a module's
 * artifact repository.
 */
public interface WriteSessionContext {

    public class ClassifierAndExtension {
        public final String classifier;
        public final String fileExtension;

        public ClassifierAndExtension(String classifier, String fileExtension) {
            this.classifier = classifier;
            this.fileExtension = fileExtension;
        }
    }

    /**
     * Returns the Maven classifier and file extension for a p2 artifact key to be added to the
     * artifact repository.
     */
    ClassifierAndExtension getClassifierAndExtensionForNewKey(IArtifactKey key);

}
