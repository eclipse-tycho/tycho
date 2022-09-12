/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;

import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.UpdateSite;

public interface ArtifactDependencyWalker {
    /**
     * Walks the visitor through artifact dependencies
     */
    void walk(ArtifactDependencyVisitor visitor);

    /**
     * Walks dependencies of specified feature. Visitor is able to manipulate content of the
     * provided feature via PluginRef and FeatureRef instances provided as via callback method
     * parameters.
     */
    void traverseFeature(File location, Feature feature, ArtifactDependencyVisitor visitor);

    void traverseUpdateSite(UpdateSite site, ArtifactDependencyVisitor visitor);

    void traverseProduct(ProductConfiguration productConfiguration, ArtifactDependencyVisitor visitor);

}
