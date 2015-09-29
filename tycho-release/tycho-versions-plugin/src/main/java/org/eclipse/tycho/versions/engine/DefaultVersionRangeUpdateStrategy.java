/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Arod - Initial implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class DefaultVersionRangeUpdateStrategy implements VersionRangeUpdateStrategy {

    private final boolean updateMatchingBounds;

    public DefaultVersionRangeUpdateStrategy(boolean updateMatchingBounds) {
        this.updateMatchingBounds = updateMatchingBounds;
    }

    @Override
    public String computeNewVersionRange(String originalVersionRange, String originalReferencedVersion,
            String newReferencedVersion) {
        if (originalVersionRange == null) {
            return null;
        }
        VersionRange referencingVersionRange = VersionRange.valueOf(originalVersionRange);
        Version referencedVersion = parseBaseVersion(originalReferencedVersion);
        Version referencedNewVersion = parseBaseVersion(newReferencedVersion);

        referencingVersionRange = computeNewVersionRange(referencingVersionRange, referencedVersion,
                referencedNewVersion);

        return referencingVersionRange.toString();
    }

    private Version parseBaseVersion(String originalReferencedVersion) {
        String baseVersion = Versions.toBaseVersion(originalReferencedVersion);
        return Version.valueOf(baseVersion);
    }

    private VersionRange computeNewVersionRange(VersionRange versionRange, Version referencedVersion,
            Version referencedNewVersion) {
        if (updateMatchingBounds) {
            versionRange = handleMatchingBouds(versionRange, referencedVersion, referencedNewVersion);
        }

        versionRange = handleNewlyOutOfScopeVersions(versionRange, referencedVersion, referencedNewVersion);
        return versionRange;
    }

    private VersionRange handleMatchingBouds(VersionRange versionRange, Version referencedVersion,
            Version referencedNewVersion) {
        if (versionRange.getLeft().equals(referencedVersion)) {
            versionRange = updateLeftBound(versionRange, versionRange.getLeftType(), referencedNewVersion);
        }
        if (versionRange.getRight() != null && versionRange.getRight().equals(referencedVersion)) {
            versionRange = updateRightBound(versionRange, versionRange.getRightType(), referencedNewVersion);
        }
        return versionRange;
    }

    private VersionRange handleNewlyOutOfScopeVersions(VersionRange versionRange, Version referencedVersion,
            Version referencedNewVersion) {
        if (versionRange.includes(referencedVersion) && !versionRange.includes(referencedNewVersion)) {
            // newVersions becomes out of scope adapt range
            if (referencedNewVersion.compareTo(referencedVersion) > 0) {
                // upgrading version adapt upper bound
                versionRange = updateRightBound(versionRange, VersionRange.RIGHT_CLOSED, referencedNewVersion);
            } else {
                // downgrading version adapt lower bound
                versionRange = updateLeftBound(versionRange, VersionRange.LEFT_CLOSED, referencedNewVersion);
            }
        }
        return versionRange;
    }

    private VersionRange updateLeftBound(VersionRange range, char leftType, Version leftVersion) {
        return new VersionRange(leftType, leftVersion, range.getRight(), range.getRightType());
    }

    private VersionRange updateRightBound(VersionRange range, char rightType, Version rightVersion) {
        return new VersionRange(range.getLeftType(), range.getLeft(), rightVersion, rightType);
    }
}
