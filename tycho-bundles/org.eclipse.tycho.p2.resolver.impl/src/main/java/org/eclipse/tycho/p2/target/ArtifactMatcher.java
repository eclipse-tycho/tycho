/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.LinkedHashSet;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;

public class ArtifactMatcher {

    public static org.eclipse.tycho.ArtifactKey resolveReference(String type, String id, String version,
            LinkedHashSet<IInstallableUnit> candidateUnits) throws IllegalArtifactReferenceException {
        VersionRange versionRange = getVersionRangeFromReference(parseAsOSGiVersion(version));
        IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(ArtifactTypeHelper.createQueryFor(type, id,
                versionRange));

        IQueryResult<IInstallableUnit> matchingIUs = query.perform(candidateUnits.iterator());
        if (matchingIUs.isEmpty()) {
            throw new RuntimeException("Cannot resolve reference to " + type + " with ID '" + id + "' and version '"
                    + version + "'");
            // TODO list other available versions for debugging?
        }

        return new DefaultArtifactKey(type, id, matchingIUs.iterator().next().getVersion().toString());
    }

    private static Version parseAsOSGiVersion(String version) throws IllegalArtifactReferenceException {
        try {
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new IllegalArtifactReferenceException("The version \"" + version + "\" is not a valid OSGi version");
        }
    }

    private static VersionRange getVersionRangeFromReference(Version version) {
        VersionRange range;
        if (version.getSegmentCount() > 3 && "qualifier".equals(version.getSegment(3))) {
            range = getRangeOfEquivalentVersions(version);
        } else if (Version.emptyVersion.equals(version)) {
            range = VersionRange.emptyRange;
        } else {
            range = getStrictRange(version);
        }
        return range;
    }

    private static VersionRange getStrictRange(Version version) {
        return new VersionRange(version, true, version, true);
    }

    /**
     * Returns a version range which includes "equivalent" versions, i.e. versions with the same
     * major, minor, and micro version.
     */
    private static VersionRange getRangeOfEquivalentVersions(Version version) {
        Integer major = (Integer) version.getSegment(0);
        Integer minor = (Integer) version.getSegment(1);
        Integer micro = (Integer) version.getSegment(2);
        VersionRange range = new VersionRange(Version.createOSGi(major, minor, micro), true, Version.createOSGi(major,
                minor, micro + 1), false);
        return range;
    }

}
