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
        VersionRange originalVersionRangeObject = VersionRange.valueOf(originalVersionRange);
        Version originalReferencedVersionObject = parseBaseVersion(originalReferencedVersion);
        Version newReferencedVersionObject = parseBaseVersion(newReferencedVersion);

        VersionRange newVersionRangeObject = computeNewVersionRange(originalVersionRangeObject,
                originalReferencedVersionObject, newReferencedVersionObject);

        return newVersionRangeObject.toString();
    }

    private Version parseBaseVersion(String version) {
        String baseVersion = Versions.toBaseVersion(version);
        return Version.valueOf(baseVersion);
    }

    private VersionRange computeNewVersionRange(VersionRange versionRange, Version originalReferencedVersion,
            Version newReferencedVersion) {
        VersionRange newVersionRange;
        if (updateMatchingBounds) {
            newVersionRange = handleMatchingBouds(versionRange, originalReferencedVersion, newReferencedVersion);
        } else {
            newVersionRange = versionRange;
        }
        return handleNewlyOutOfScopeVersions(newVersionRange, originalReferencedVersion, newReferencedVersion);
    }

    private VersionRange handleMatchingBouds(VersionRange versionRange, Version originalReferencedVersion,
            Version newReferencedVersion) {
        Version newLeft;
        if (versionRange.getLeft().equals(originalReferencedVersion)) {
            newLeft = newReferencedVersion;
        } else {
            newLeft = versionRange.getLeft();
        }

        Version newRight;
        if (versionRange.getRight() != null && versionRange.getRight().equals(originalReferencedVersion)) {
            newRight = newReferencedVersion;
        } else {
            newRight = versionRange.getRight();
        }

        return new VersionRange(versionRange.getLeftType(), newLeft, newRight, versionRange.getRightType());
    }

    private VersionRange handleNewlyOutOfScopeVersions(VersionRange versionRange, Version originalReferencedVersion,
            Version newReferencedVersion) {
        if (versionRange.includes(originalReferencedVersion) && !versionRange.includes(newReferencedVersion)) {
            // newVersions becomes out of scope adapt range
            if (newReferencedVersion.compareTo(originalReferencedVersion) > 0) {
                // upgrading version adapt upper bound
                versionRange = updateRightBound(versionRange, VersionRange.RIGHT_CLOSED, newReferencedVersion);
            } else {
                // downgrading version adapt lower bound
                versionRange = updateLeftBound(versionRange, VersionRange.LEFT_CLOSED, newReferencedVersion);
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

    @Override
    public ImportRefVersionConstraint computeNewImportRefVersionConstraint(
            ImportRefVersionConstraint originalVersionConstraint, String originalReferencedVersion,
            String newReferencedVersion) {

        if (originalVersionConstraint.getVersion() == null) {
            return originalVersionConstraint;
        }

        ImportRefVersionConstraint versionConstraintUsingBaseVersion = toBaseVersionConstraint(
                originalVersionConstraint);
        String referencedBaseVersion = Versions.toBaseVersion(originalReferencedVersion);
        String newReferencedBaseVersion = Versions.toBaseVersion(newReferencedVersion);

        if ((updateMatchingBounds && versionConstraintUsingBaseVersion.getVersion().equals(referencedBaseVersion))
                || versionConstraintUsingBaseVersion.matches(referencedBaseVersion)
                        && !versionConstraintUsingBaseVersion.matches(newReferencedBaseVersion)) {
            return originalVersionConstraint.withVersion(newReferencedBaseVersion);
        } else {
            return originalVersionConstraint;
        }
    }

    private ImportRefVersionConstraint toBaseVersionConstraint(ImportRefVersionConstraint originalVersionConstraint) {
        if (originalVersionConstraint.getVersion() != null) {
            return new ImportRefVersionConstraint(Versions.toBaseVersion(originalVersionConstraint.getVersion()),
                    originalVersionConstraint.getMatch());
        } else {
            return originalVersionConstraint;
        }
    }
}
