/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.runtime;

import java.util.List;

import org.apache.maven.model.Dependency;

public interface TychoOsgiRuntimeArtifacts {
    /**
     * Main Tycho OSGi runtime artifacts. First element of returned {@link Dependency} list is
     * expected to point at a directory following eclipse installation layout (i.e. with config/,
     * plugins/ subdirectories). org.eclipse.osgi is expected to be under plugins/ subdirectory
     */
    public static final String HINT_FRAMEWORK = "framework";

    /**
     * tycho shared runtime artifact
     */
    public static final String HINT_SHARED = "shared";

    /**
     * Bundle manifest attribute name, if set, bundle is not intended for use when Tycho is embedded
     * in another Equinox-based application. The only currently known use case is the Equinox secure
     * storage provider implementation used by Tycho to suppress password requests for transient
     * secure storage.
     */
    public static final String NOEMBED = "Tycho-NoEmbed";

    /**
     * Returns list of Maven artifacts that will be installed in Tycho's OSGi runtime. Artifacts
     * with packaging=zip will be assumed to have eclipse installation layout and will be unpacked
     * before used. Artifacts with packaging=jar are assumed to be OSGi bundles and will be added to
     * the runtime as is.
     */
    public List<Dependency> getRuntimeArtifacts();

    static Dependency newDependency(String groupId, String artifactId, String version, String type) {
        Dependency d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion(version);
        d.setType(type);
        return d;
    }
}
