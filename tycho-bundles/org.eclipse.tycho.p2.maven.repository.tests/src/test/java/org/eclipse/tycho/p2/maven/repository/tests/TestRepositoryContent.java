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
package org.eclipse.tycho.p2.maven.repository.tests;

import java.net.URI;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil.P2Repositories;

@SuppressWarnings("restriction")
public final class TestRepositoryContent {

    public static final URI REPO_BUNDLE_A = P2Repositories.ECLIPSE_342.toURI();
    public static final URI REPO_BUNDLE_AB = P2Repositories.PACK_GZ.toURI();

    public static final IArtifactKey BUNDLE_A_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
            Version.parseVersion("3.4.3.R34x_v20081215-1030"));
    public static final IArtifactKey BUNDLE_B_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.ecf",
            Version.parseVersion("3.1.300.v20120319-0616"));

    public static final String BUNDLE_A_CONTENT_MD5 = "cdc528b62f2eeea0ffbbc4d0bdcd8f83";
    public static final String BUNDLE_B_CONTENT_MD5 = "1d5b18f2f8c3dfc70b7b08a349a9632d";
    public static final String BUNDLE_B_PACKED_CONTENT_MD5 = "d827085062b9cceff5401db6eb2e5860";

    // constants for negative test cases

    /** Artifact which is contained in none of the repositories. */
    public static final IArtifactKey NOT_CONTAINED_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "not-in-repo",
            Version.parseVersion("1"));

    /** Repository that claims to contain bundle A, but accesses to the artifact file will fail */
    public static final URI REPO_BUNDLE_A_CORRUPT = ResourceUtil.resourceFile("repositories/e342_missing_file").toURI();
}
