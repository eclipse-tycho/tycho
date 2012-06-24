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
package org.eclipse.tycho.repository.registry.facade;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Representation of the p2 repositories that receive the artifacts produced by the build.
 * <p>
 * This interface is a subset of
 * {@link org.eclipse.tycho.repository.publishing.PublishingRepository}, limited to methods required
 * from the Maven class loader.
 */
public interface PublishingRepositoryFacade {

    /**
     * The file system locations of the build artifacts, indexed by classifier.
     * 
     * @return a map from classifier (<code>null</code> for main artifact) to artifact file
     *         locations in the target directory
     */
    Map<String, File> getArtifactLocations();

    /**
     * Returns the <code>IInstallableUnit</code>s in the publishing repository.
     */
    Set<Object> getInstallableUnits();

    // TODO also store published seed IUs in the publishing repository?
    // currently in org.eclipse.tycho.plugins.p2.publisher.AbstractPublishMojo.postPublishedIUs(Collection<?>)
}
