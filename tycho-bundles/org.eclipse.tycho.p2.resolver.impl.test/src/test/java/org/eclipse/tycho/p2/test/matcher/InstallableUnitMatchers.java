/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.test.matcher;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

@SuppressWarnings("restriction")
public class InstallableUnitMatchers {
    private static final String TYPE = "IInstallableUnit";

    public static Matcher<IInstallableUnit> unitWithId(final String id) {
        return new TypeSafeMatcher<IInstallableUnit>(IInstallableUnit.class) {

            public void describeTo(Description description) {
                description.appendText(TYPE + " with ID " + id);
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                return id.equals(item.getId());
            }
        };
    }

    public static Matcher<IInstallableUnit> unit(final String id, final String version) {
        final Version parsedVersion = Version.parseVersion(version);

        return new TypeSafeMatcher<IInstallableUnit>() {

            public void describeTo(Description description) {
                description.appendText(TYPE + " with with ID " + id + " and version " + parsedVersion);
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                return id.equals(item.getId()) && parsedVersion.equals(item.getVersion());
            }
        };
    }

    public static Matcher<IInstallableUnit> hasGAV(String groupId, String artifactId, String version) {
        return hasGAV(groupId, artifactId, version, null);
    }

    public static Matcher<IInstallableUnit> hasGAV(final String groupId, final String artifactId, final String version,
            final String classifier) {

        return new TypeSafeMatcher<IInstallableUnit>() {

            public void describeTo(Description description) {
                description.appendText(TYPE + " with GAV " + gavString(groupId, artifactId, version, classifier));
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                String actualGroupId = item.getProperty(RepositoryLayoutHelper.PROP_GROUP_ID);
                String actualArtifactId = item.getProperty(RepositoryLayoutHelper.PROP_ARTIFACT_ID);
                String actualVersion = item.getProperty(RepositoryLayoutHelper.PROP_VERSION);
                String actualClassifier = item.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
                return isEqual(groupId, actualGroupId) && isEqual(artifactId, actualArtifactId)
                        && isEqual(version, actualVersion) && isEqual(classifier, actualClassifier);
            }
        };
    }

    static String gavString(String groupId, String artifactId, String version, String classifier) {
        return groupId + ":" + artifactId + ":" + version + (classifier == null ? "" : ":" + classifier);
    }

    static <T> boolean isEqual(T left, T right) {
        if (left == right) {
            return true;
        }
        if (left == null) {
            return false;
        }
        return left.equals(right);
    }
}
