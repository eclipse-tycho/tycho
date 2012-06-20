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
package org.eclipse.tycho.p2.tools.publisher;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;

/**
 * {@link WriteSessionContext} for publishing the binary executables artifacts.
 */
@SuppressWarnings("restriction")
public class ProductBinariesWriteSession implements WriteSessionContext {

    private final String artifactPrefix;

    public ProductBinariesWriteSession(String productId) {
        this.artifactPrefix = productId + '.';
    }

    public ClassifierAndExtension getClassifierAndExtensionForNewKey(IArtifactKey key) {
        if (PublisherHelper.BINARY_ARTIFACT_CLASSIFIER.equals(key.getClassifier())) {

            String artifactId = key.getId();
            if (artifactId.startsWith(artifactPrefix)) {
                // TODO strip product id from classifier (once we allow only one product per module)
                return new ClassifierAndExtension(artifactId, "zip"); //.substring(artifactPrefix.length());
            } else {
                throw new AssertionFailedException("Unexpected artifact: " + key);
            }

        } else {
            throw new AssertionFailedException("Unexpected artifact: " + key);
        }
    }
}
