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
package org.eclipse.tycho.repository.general;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

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

@SuppressWarnings("restriction")
public class LocalArtifactTransferPolicyTest {

    static final IArtifactKey DEFAULT_KEY = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
            Version.parseVersion("3.4.3.R34x_v20081215-1030"));

    @Rule
    public P2Context p2Context = new P2Context();
    private ArtifactTransferPolicy subject = new LocalArtifactTransferPolicy();

    @Test
    public void testPreferenceForCanonical() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedAndCanonical", p2Context);

        IArtifactDescriptor result = subject.pickFormat(descriptors);

        assertThat(result.getProperties().get(IArtifactDescriptor.FORMAT), is(nullValue()));
    }

    @Test
    public void testPreferenceForPack200IfNoCanonical() throws Exception {
        IArtifactDescriptor[] descriptors = loadDescriptorsFromRepository("packedAndOther", p2Context);

        IArtifactDescriptor result = subject.pickFormat(descriptors);

        assertThat(result.getProperties().get(IArtifactDescriptor.FORMAT), is(IArtifactDescriptor.FORMAT_PACKED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyList() {
        subject.pickFormat(new IArtifactDescriptor[0]);
    }

    static IArtifactDescriptor[] loadDescriptorsFromRepository(String repository, P2Context p2Context) throws Exception {
        IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) p2Context.getAgent().getService(
                IArtifactRepositoryManager.SERVICE_NAME);
        File repoPath = ResourceUtil.resourceFile("repositories/rawformats/" + repository);
        IArtifactRepository loadedRepo = repoManager.loadRepository(repoPath.toURI(), new NullProgressMonitor());
        return loadedRepo.getArtifactDescriptors(DEFAULT_KEY);
    }

}
