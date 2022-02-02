/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.testutil;

import java.util.Map;

import org.eclipse.tycho.TychoConstants;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArtifactPropertiesMatchers {

    public static Matcher<Map<String, String>> containsGAV(String groupId, String artifactId, String version) {
        return containsGAV(groupId, artifactId, version, null);
    }

    public static Matcher<Map<String, String>> containsGAV(final String groupId, final String artifactId,
            final String version, final String classifier) {
        return new TypeSafeMatcher<Map<String, String>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("properties specifying the GAV "
                        + gavString(groupId, artifactId, version, classifier));
            }

            @Override
            public boolean matchesSafely(Map<String, String> map) {
                String actualGroupId = map.get(TychoConstants.PROP_GROUP_ID);
                String actualArtifactId = map.get(TychoConstants.PROP_ARTIFACT_ID);
                String actualVersion = map.get(TychoConstants.PROP_VERSION);
                String actualClassifier = map.get(TychoConstants.PROP_CLASSIFIER);
                return isEqual(groupId, actualGroupId) && isEqual(artifactId, actualArtifactId)
                        && isEqual(version, actualVersion) && isEqual(classifier, actualClassifier);
            }
        };
    }

    public static Matcher<Map<String, String>> hasProperty(final String key, final String value) {
        return new TypeSafeMatcher<Map<String, String>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("properties with the entry ").appendValue(key).appendText("=")
                        .appendValue(value);
            }

            @Override
            protected boolean matchesSafely(Map<String, String> map) {
                return map.containsKey(key) && isEqual(value, map.get(key));
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
