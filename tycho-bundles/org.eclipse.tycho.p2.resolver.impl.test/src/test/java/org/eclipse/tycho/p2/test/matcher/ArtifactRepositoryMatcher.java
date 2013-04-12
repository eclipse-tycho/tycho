/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.test.matcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArtifactRepositoryMatcher {

    static String keyToString(IArtifactKey artifactKey) {
        return artifactKey.getClassifier() + ":" + artifactKey.getId() + ":" + artifactKey.getVersion();
    }

    public static Matcher<IArtifactRepository> containsEntry(final IArtifactKey artifactKey) {
        return new TypeSafeMatcher<IArtifactRepository>() {

            public void describeTo(Description description) {
                description.appendText("artifact repository with entry " + keyToString(artifactKey));
            }

            @Override
            public boolean matchesSafely(IArtifactRepository repo) {
                return repo.contains(artifactKey);
            }
        };
    }

    public static EntryMatcher entry(IArtifactKey artifactKey) {
        return new EntryMatcher(artifactKey);
    }

    public static class EntryMatcher {

        private final IArtifactKey artifactKey;

        public EntryMatcher(IArtifactKey artifactKey) {
            this.artifactKey = artifactKey;
        }

        public Matcher<IArtifactRepository> hasContent(final File expectedArtifactContent) {
            if (!expectedArtifactContent.canRead())
                throw new IllegalArgumentException("Cannot read expected content from " + expectedArtifactContent);

            return new TypeSafeMatcher<IArtifactRepository>() {

                public void describeTo(Description description) {
                    description.appendText("artifact repository with entry " + keyToString(artifactKey)
                            + " whose artifact is equal to " + expectedArtifactContent);
                }

                @Override
                public boolean matchesSafely(IArtifactRepository repo) {
                    IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(artifactKey);
                    if (descriptors.length == 0)
                        return false;

                    OutputStream assertingSink = new ComparingOutputStream(expectedArtifactContent);
                    try {
                        IStatus status = repo.getArtifact(descriptors[0], assertingSink, null);
                        assertingSink.close();
                        return status.isOK();
                    } catch (IOException e) {
                        return false;
                    }
                }
            };
        }
    }
}
