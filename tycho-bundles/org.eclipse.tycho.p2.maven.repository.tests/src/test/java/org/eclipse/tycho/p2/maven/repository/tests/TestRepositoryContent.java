/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;

@SuppressWarnings("restriction")
public final class TestRepositoryContent {

    public static final IArtifactKey BUNDLE_A_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
            Version.parseVersion("3.4.3.R34x_v20081215-1030"));
    public static final IArtifactKey BUNDLE_B_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.ecf",
            Version.parseVersion("3.1.300.v20120319-0616"));

    public static final Set<String> BUNDLE_A_FILES = new HashSet<String>(Arrays.asList(new String[] { "about_files/",
            "META-INF/", "META-INF/MANIFEST.MF", "org/", "org/eclipse/", "org/eclipse/core/",
            "org/eclipse/core/runtime/", "org/eclipse/core/runtime/adaptor/", "org/eclipse/core/runtime/internal/",
            "org/eclipse/core/runtime/internal/adaptor/", "org/eclipse/core/runtime/internal/stats/",
            "org/eclipse/osgi/", "org/eclipse/osgi/baseadaptor/", "org/eclipse/osgi/baseadaptor/bundlefile/",
            "org/eclipse/osgi/baseadaptor/hooks/", "org/eclipse/osgi/baseadaptor/loader/", "org/eclipse/osgi/event/",
            "org/eclipse/osgi/framework/", "org/eclipse/osgi/framework/adaptor/",
            "org/eclipse/osgi/framework/console/", "org/eclipse/osgi/framework/debug/",
            "org/eclipse/osgi/framework/eventmgr/", "org/eclipse/osgi/framework/internal/",
            "org/eclipse/osgi/framework/internal/core/", "org/eclipse/osgi/framework/internal/protocol/",
            "org/eclipse/osgi/framework/internal/protocol/bundleentry/",
            "org/eclipse/osgi/framework/internal/protocol/bundleresource/",
            "org/eclipse/osgi/framework/internal/protocol/reference/",
            "org/eclipse/osgi/framework/internal/reliablefile/", "org/eclipse/osgi/framework/launcher/",
            "org/eclipse/osgi/framework/log/", "org/eclipse/osgi/framework/util/", "org/eclipse/osgi/internal/",
            "org/eclipse/osgi/internal/baseadaptor/", "org/eclipse/osgi/internal/module/",
            "org/eclipse/osgi/internal/profile/", "org/eclipse/osgi/internal/provisional/",
            "org/eclipse/osgi/internal/provisional/service/",
            "org/eclipse/osgi/internal/provisional/service/security/",
            "org/eclipse/osgi/internal/provisional/verifier/", "org/eclipse/osgi/internal/resolver/",
            "org/eclipse/osgi/internal/service/", "org/eclipse/osgi/internal/service/security/",
            "org/eclipse/osgi/internal/signedcontent/", "org/eclipse/osgi/service/",
            "org/eclipse/osgi/service/datalocation/", "org/eclipse/osgi/service/debug/",
            "org/eclipse/osgi/service/environment/", "org/eclipse/osgi/service/localization/",
            "org/eclipse/osgi/service/pluginconversion/", "org/eclipse/osgi/service/resolver/",
            "org/eclipse/osgi/service/runnable/", "org/eclipse/osgi/service/security/",
            "org/eclipse/osgi/service/urlconversion/", "org/eclipse/osgi/signedcontent/",
            "org/eclipse/osgi/storagemanager/", "org/eclipse/osgi/util/", "org/osgi/", "org/osgi/framework/",
            "org/osgi/service/", "org/osgi/service/condpermadmin/", "org/osgi/service/packageadmin/",
            "org/osgi/service/permissionadmin/", "org/osgi/service/startlevel/", "org/osgi/service/url/",
            "org/osgi/util/", "org/osgi/util/tracker/", "systembundle.properties" }));
    public static final Set<String> BUNDLE_B_FILES = new HashSet<String>(Arrays.asList(new String[] { "META-INF/",
            "META-INF/eclipse.inf", "META-INF/MANIFEST.MF" }));

    public static final String BUNDLE_A_CONTENT_MD5 = "58057045158895009b845b9a93f3eb6e";
    public static final String BUNDLE_A_PACKED_CONTENT_MD5 = "497882c1f6919994245fcbe9f98441df";
    // no BUNDLE_A_CONTENT_MD5 because bundle B is only available in pack200 format -> don't do binary assertions on unpack200 result 
    public static final String BUNDLE_B_PACKED_CONTENT_MD5 = "d827085062b9cceff5401db6eb2e5860";

    // constants for negative test cases

    /** Artifact which is contained in none of the repositories. */
    public static final IArtifactKey NOT_CONTAINED_ARTIFACT_KEY = new ArtifactKey("osgi.bundle", "not-in-repo",
            Version.parseVersion("1"));

    // repositories (of regular p2 type) containing the test data

    /** Repository with bundle A */
    public static final URI REPO_BUNDLE_A = ResourceUtil.P2Repositories.ECLIPSE_342.toURI();
    /**
     * Repository with bundles A and B. Bundle A is available both in packed and canonical format,
     * bundle B only in packed format.
     */
    public static final URI REPO_BUNDLE_AB = ResourceUtil.P2Repositories.PACK_GZ.toURI();

    /** Repository that claims to contain bundle A, but accesses to the artifact file will fail */
    public static final URI REPO_BUNDLE_A_CORRUPT = ResourceUtil.resourceFile("repositories/e342_missing_file").toURI();
    /**
     * Repository that claims to contain both bundles A and B. Bundle B is contained only in packed
     * format, but the artifact is broken. Bundle A is contained in packed format (artifact missing)
     * and in canonical format (working).
     */
    public static final URI REPO_BUNLDE_AB_PACK_CORRUPT = ResourceUtil.resourceFile("repositories/packgz_corrupt")
            .toURI();

}
