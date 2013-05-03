/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider.formats;

import static org.eclipse.tycho.repository.p2base.artifact.provider.formats.RemoteArtifactTransferPolicyTest.asSet;
import static org.eclipse.tycho.repository.p2base.artifact.provider.formats.RemoteArtifactTransferPolicyTest.formatsOf;
import static org.eclipse.tycho.repository.p2base.artifact.provider.formats.RemoteArtifactTransferPolicyTest.loadDescriptorsFromRepository;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;

public class LocalArtifactTransferPolicyTest {

    @Rule
    public P2Context p2Context = new P2Context();
    private ArtifactTransferPolicy subject = new LocalArtifactTransferPolicy();

    @Test
    public void testPreferredOrder() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedCanonicalAndOther", p2Context);

        List<IArtifactDescriptor> result = subject.sortFormatsByPreference(descriptors);

        assertThat(result.get(0).getProperty(IArtifactDescriptor.FORMAT), is(nullValue()));
        assertThat(result.get(1).getProperty(IArtifactDescriptor.FORMAT), is(IArtifactDescriptor.FORMAT_PACKED));
        assertThat(formatsOf(result.get(2), result.get(3)), is(asSet("customFormat", "anotherFormat")));
        assertThat(result.size(), is(4));
    }

}
