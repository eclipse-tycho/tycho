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
