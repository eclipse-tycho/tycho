/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.testutil;

import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArtifactRepositoryTestUtils {

    public static final IQuery<IArtifactKey> ANY_ARTIFACT_KEY_QUERY = QueryUtil.createMatchQuery(IArtifactKey.class,
            "true");

    public static Set<IArtifactKey> allKeysIn(IArtifactRepository repository) {
        IQueryResult<IArtifactKey> queryResult = repository.query(ANY_ARTIFACT_KEY_QUERY, null);
        return queryResult.toUnmodifiableSet();
    }

    public static IArtifactDescriptor canonicalDescriptorFor(IArtifactKey key) {
        return new ArtifactDescriptor(key);
    }

    public static IArtifactDescriptor packedDescriptorFor(IArtifactKey key) {
        ArtifactDescriptor result = new ArtifactDescriptor(key);
        result.setProcessingSteps(new IProcessingStepDescriptor[] { new ProcessingStepDescriptor(
                "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) });
        result.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
        return result;
    }

    public static Matcher<? super IArtifactDescriptor> inCanonicalFormat() {
        return new TypeSafeMatcher<IArtifactDescriptor>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a canonical artifact");
            }

            @Override
            public boolean matchesSafely(IArtifactDescriptor item) {
                return ArtifactTransferPolicy.isCanonicalFormat(item);
            }
        };
    }

    public static Matcher<? super IArtifactDescriptor> inPackedFormat() {
        return new TypeSafeMatcher<IArtifactDescriptor>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a pack200 artifact");
            }

            @Override
            public boolean matchesSafely(IArtifactDescriptor item) {
                return ArtifactTransferPolicy.isPack200Format(item);
            }
        };
    }

}
