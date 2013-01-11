/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

@SuppressWarnings("restriction")
public final class ArtifactProviderTestUtils {

    public static IArtifactDescriptor canonicalDescriptorFor(IArtifactKey key) {
        return new ArtifactDescriptor(key);
    }

    public static Matcher<? extends IArtifactDescriptor> inCanonicalFormat() {
        return new TypeSafeMatcher<IArtifactDescriptor>() {

            public void describeTo(Description description) {
                description.appendText("a canonical artifact");
            }

            @Override
            public boolean matchesSafely(IArtifactDescriptor item) {
                return ArtifactTransferPolicy.isCanonicalFormat(item);
            }
        };
    }

    public static Matcher<? extends IArtifactDescriptor> inPackedFormat() {
        return new TypeSafeMatcher<IArtifactDescriptor>() {

            public void describeTo(Description description) {
                description.appendText("a pack200 artifact");
            }

            @Override
            public boolean matchesSafely(IArtifactDescriptor item) {
                return ArtifactTransferPolicy.isPack200Format(item);
            }
        };
    }

    public static final IQuery<IArtifactKey> ANY_ARTIFACT_KEY_QUERY = QueryUtil.createMatchQuery(IArtifactKey.class,
            "true");

}
