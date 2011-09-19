/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.util.List;

import org.apache.maven.model.Dependency;

/**
 * Tycho P2 runtime metadata.
 */
public interface TychoP2RuntimeMetadata {
    /**
     * Main equinox runtime metadata. First element of return Dependency list is expected to point
     * at directory following eclipse installation layout (i.e. with config/, plugins/
     * subdirectories). org.eclipse.osgi is expected to be under plugins/ subdirectory
     */
    public static final String HINT_FRAMEWORK = "framework";

    /**
     * Bundle manifest attribute name, if set, bundle is not intended for use when Tycho is embedded
     * in another Equinox-based application. The only usecase currently is Equinox secure storage
     * provder implementation used by Tycho to suppress password requests for transient secure
     * storage.
     */
    public static final String NOEMBED = "Tycho-NoEmbed";

    /**
     * Returns list of Maven artifacts that will be installed in Tycho P2 runtime. Artifacts with
     * packaging=zip will be assumed to have eclipse installation layout and will be unpacked before
     * used. Artifacts with packaging=jar are assumed to be OSGi bundles and will be added to the
     * runtime as is.
     */
    public List<Dependency> getRuntimeArtifacts();
}
