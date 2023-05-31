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
package org.eclipse.tycho.p2.tools.publisher;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.repository.publishing.WriteSessionContext;

/**
 * {@link WriteSessionContext} for publishing the binary executables artifacts.
 */
public class ProductBinariesWriteSession implements WriteSessionContext {

    private final String artifactPrefix;

    public ProductBinariesWriteSession(String productId) {
        this.artifactPrefix = productId + '.';
    }

    @Override
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
