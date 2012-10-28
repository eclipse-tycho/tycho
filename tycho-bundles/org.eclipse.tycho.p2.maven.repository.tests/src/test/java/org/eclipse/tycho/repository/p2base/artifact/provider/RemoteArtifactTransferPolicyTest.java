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

import static org.eclipse.tycho.repository.p2base.artifact.provider.LocalArtifactTransferPolicyTest.loadDescriptorsFromRepository;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.RemoteArtifactTransferPolicy;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;

public class RemoteArtifactTransferPolicyTest {

    @Rule
    public P2Context p2Context = new P2Context();
    private ArtifactTransferPolicy subject = new RemoteArtifactTransferPolicy();

    @Test
    public void testPreferenceForPack200() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedAndCanonical", p2Context);

        IArtifactDescriptor result = subject.pickFormat(descriptors);

        assertThat(result.getProperties().get(IArtifactDescriptor.FORMAT), is(IArtifactDescriptor.FORMAT_PACKED));
    }

    @Test
    public void testPreferenceForCanonicalIfNoPack200() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("canonicalAndOther", p2Context);

        IArtifactDescriptor result = subject.pickFormat(descriptors);

        assertThat(result.getProperties().get(IArtifactDescriptor.FORMAT), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyList() {
        subject.pickFormat(new IArtifactDescriptor[0]);
    }

}
