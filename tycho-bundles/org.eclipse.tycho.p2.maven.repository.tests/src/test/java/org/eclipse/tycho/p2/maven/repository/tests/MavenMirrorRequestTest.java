/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.tests;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.MavenMirrorRequest;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("restriction")
public class MavenMirrorRequestTest extends BaseMavenRepositoryTest {
    private IProgressMonitor monitor = new NullProgressMonitor();

    @Test
    public void testMirror() throws Exception {
        IProvisioningAgent agent = Activator.getProvisioningAgent();
        Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);

        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);

        IArtifactRepository repository = manager.loadRepository(new File("resources/repositories/e342").toURI(),
                monitor);

        LocalArtifactRepository localRepository = new LocalArtifactRepository(localRepoIndices);

        IArtifactKey key = new ArtifactKey("osgi.bundle", "org.eclipse.osgi",
                Version.parseVersion("3.4.3.R34x_v20081215-1030"));

        MavenMirrorRequest request = new MavenMirrorRequest(key, localRepository, transport);

        repository.getArtifacts(new IArtifactRequest[] { request }, monitor);

        Assert.assertEquals(1, localRepository.getArtifactDescriptors(key).length);
    }

}
