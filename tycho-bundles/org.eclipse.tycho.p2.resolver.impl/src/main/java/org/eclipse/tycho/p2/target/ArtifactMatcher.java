/*******************************************************************************
 * Copyright (c) 2014, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Issue #845 - Feature restrictions are not taken into account when using emptyVersion
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.IllegalArtifactReferenceException;

@SuppressWarnings("restriction")
public class ArtifactMatcher {

    public static IInstallableUnit resolveReference(String type, String id, VersionRange versionRange,
            LinkedHashSet<IInstallableUnit> candidateUnits) throws IllegalArtifactReferenceException {
        if (id == null) {
            throw new IllegalArtifactReferenceException("ID is required");
        }

        IQuery<IInstallableUnit> query = QueryUtil
                .createLatestQuery(ArtifactTypeHelper.createQueryFor(type, id, versionRange));

        IQueryResult<IInstallableUnit> matchingIUs = query.perform(candidateUnits.iterator());
        if (matchingIUs.isEmpty()) {
            return null;
        }
        Set<IInstallableUnit> ius = matchingIUs.toSet();
        if (ius.size() == 1) {
            return ius.iterator().next();
        }
        if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(type)) {
            return ius.stream().flatMap(iu -> getPackageVersion(iu, id).map(v -> new SimpleEntry<>(iu, v)).stream())
                    .max((o1, o2) -> {
                        return o1.getValue().compareTo(o2.getValue());
                    }).map(Entry::getKey).orElse(null);
        } else {
            return ius.iterator().next();
        }
    }

    private static Optional<Version> getPackageVersion(IInstallableUnit unit, String packageName) {

        return unit.getProvidedCapabilities().stream()
                .filter(capability -> PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(capability.getNamespace()))
                .filter(capability -> packageName.equals(capability.getName())).map(IProvidedCapability::getVersion)
                .max(Comparator.naturalOrder());
    }

    public static Version parseAsOSGiVersion(String version) throws IllegalArtifactReferenceException {
        if (version == null) {
            return Version.emptyVersion;
        }
        try {
            return Version.parseVersion(version);
        } catch (IllegalArgumentException e) {
            throw new IllegalArtifactReferenceException("The string \"" + version + "\" is not a valid OSGi version");
        }
    }

    public static VersionRange getVersionRangeFromReference(Version version) {
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

    public static VersionRange getVersionRangeFromImport(String version, String rule) {
        class DummyFeatureAction extends FeaturesAction {

            public DummyFeatureAction() {
                super(new Feature[0]);
            }

            @Override
            protected VersionRange getVersionRange(FeatureEntry entry) {
                VersionRange versionRange = super.getVersionRange(entry);
                if (versionRange == null) {
                    return VersionRange.emptyRange;
                }
                return versionRange;
            }

        }
        FeatureEntry entry = FeatureEntry.createRequires("dummy", version, rule, null, false);

        return new DummyFeatureAction().getVersionRange(entry);
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
        VersionRange range = new VersionRange(Version.createOSGi(major, minor, micro), true,
                Version.createOSGi(major, minor, micro + 1), false);
        return range;
    }

}
