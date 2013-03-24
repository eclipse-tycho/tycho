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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
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

        assertThat(result.getProperty(IArtifactDescriptor.FORMAT), is(IArtifactDescriptor.FORMAT_PACKED));
    }

    @Test
    public void testPreferenceForCanonicalIfNoPack200() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("canonicalAndOther", p2Context);

        IArtifactDescriptor result = subject.pickFormat(descriptors);

        assertThat(result.getProperty(IArtifactDescriptor.FORMAT), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyList() {
        subject.pickFormat(new IArtifactDescriptor[0]);
    }

    @Test
    public void testPreferredOrderOfEmptyList() {
        assertThat(subject.sortFormatsByPreference(new IArtifactDescriptor[0]),
                is(Collections.<IArtifactDescriptor> emptyList()));
    }

    @Test
    public void testPreferredOrder() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedCanonicalAndOther", p2Context);

        List<IArtifactDescriptor> result = subject.sortFormatsByPreference(descriptors);

        assertThat(result.get(0).getProperty(IArtifactDescriptor.FORMAT), is(IArtifactDescriptor.FORMAT_PACKED));
        assertThat(result.get(1).getProperty(IArtifactDescriptor.FORMAT), is(nullValue()));
        assertThat(getFormats(result.get(2), result.get(3)), is(asSet("customFormat", "anotherFormat")));
        assertThat(result.size(), is(4));
    }

    @Test
    public void testPreferredOrderWithoutBestChoice() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("canonicalAndOther", p2Context);

        List<IArtifactDescriptor> result = subject.sortFormatsByPreference(descriptors);

        assertThat(result.get(0).getProperty(IArtifactDescriptor.FORMAT), is(nullValue()));
        assertThat(result.get(1).getProperty(IArtifactDescriptor.FORMAT), is("anotherFormat"));
        assertThat(result.size(), is(2));
    }

    static Set<String> getFormats(IArtifactDescriptor... descriptors) {
        Set<String> result = new HashSet<String>();
        for (IArtifactDescriptor descriptor : descriptors) {
            result.add(descriptor.getProperty(IArtifactDescriptor.FORMAT));
        }
        return result;
    }

    static Set<String> asSet(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

}
