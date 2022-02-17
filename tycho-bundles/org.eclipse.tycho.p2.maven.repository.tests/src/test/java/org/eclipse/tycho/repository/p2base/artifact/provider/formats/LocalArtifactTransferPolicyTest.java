/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
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
package org.eclipse.tycho.repository.p2base.artifact.provider.formats;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.tests.ResourceUtil;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;

public class LocalArtifactTransferPolicyTest {

    static final IArtifactKey DEFAULT_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
            Version.parseVersion("3.4.3.R34x_v20081215-1030"));

    @Rule
    public P2Context p2Context = new P2Context();
    private ArtifactTransferPolicy subject = new LocalArtifactTransferPolicy();

    @Test
    public void testPreferredOrder() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedCanonicalAndOther", p2Context);

        List<IArtifactDescriptor> result = subject.sortFormatsByPreference(descriptors);

        assertThat(result.get(0).getProperty(IArtifactDescriptor.FORMAT), is(nullValue()));
        assertThat(formatsOf(result.get(1), result.get(2)), is(asSet("customFormat", "anotherFormat")));
        assertThat(result.size(), is(3));
    }

    static Set<String> formatsOf(IArtifactDescriptor... descriptors) {
        Set<String> result = new HashSet<>();
        for (IArtifactDescriptor descriptor : descriptors) {
            result.add(descriptor.getProperty(IArtifactDescriptor.FORMAT));
        }
        return result;
    }

    static Set<String> asSet(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    static IArtifactDescriptor[] loadDescriptorsFromRepository(String repository, P2Context p2Context)
            throws Exception {
        IArtifactRepositoryManager repoManager = p2Context.getAgent().getService(IArtifactRepositoryManager.class);
        File repoPath = ResourceUtil.resourceFile("repositories/rawformats/" + repository);
        IArtifactRepository loadedRepo = repoManager.loadRepository(repoPath.toURI(), new NullProgressMonitor());
        return loadedRepo.getArtifactDescriptors(DEFAULT_KEY);
    }

}
