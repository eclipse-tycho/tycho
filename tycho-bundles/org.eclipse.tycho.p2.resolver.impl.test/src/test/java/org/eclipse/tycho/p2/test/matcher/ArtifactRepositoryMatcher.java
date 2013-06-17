/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.test.matcher;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.repository.p2base.artifact.provider.IArtifactProvider;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArtifactRepositoryMatcher {

    static String keyToString(IArtifactKey artifactKey) {
        return artifactKey.getClassifier() + ":" + artifactKey.getId() + ":" + artifactKey.getVersion();
    }

    public static Matcher<IArtifactProvider> contains(final IArtifactKey artifactKey) {
        return new TypeSafeMatcher<IArtifactProvider>() {

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
