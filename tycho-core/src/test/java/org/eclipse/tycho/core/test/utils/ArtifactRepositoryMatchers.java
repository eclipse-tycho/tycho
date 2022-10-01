/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
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
package org.eclipse.tycho.core.test.utils;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.p2.artifact.provider.IArtifactProvider;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArtifactRepositoryMatchers {

    static String keyToString(IArtifactKey artifactKey) {
        return artifactKey.getClassifier() + ":" + artifactKey.getId() + ":" + artifactKey.getVersion();
    }

    public static Matcher<IArtifactProvider> contains(final IArtifactKey artifactKey) {
        return new TypeSafeMatcher<IArtifactProvider>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("artifact repository with entry " + keyToString(artifactKey));
            }

            @Override
            public boolean matchesSafely(IArtifactProvider repo) {
                return repo.contains(artifactKey);
            }
        };
    }

}
