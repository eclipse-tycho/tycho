/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalArtifactRepositoryFactory;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.test.util.TemporaryLocalMavenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

@ExtendWith(TychoPlexusExtension.class)
public class LocalArtifactRepositoryFactoryTest {

    @Inject
    protected PlexusContainer container;

    @RegisterExtension
    public TemporaryLocalMavenRepository tempLocalMavenRepository = new TemporaryLocalMavenRepository();
    private LocalArtifactRepositoryFactory subject;

    @BeforeEach
    public void setUp() {
        subject = new LocalArtifactRepositoryFactory() {

            @Override
            protected LocalRepositoryP2Indices lookupLocalRepoIndices() {
                return tempLocalMavenRepository.getLocalRepositoryIndex();
            }
        };
    }

    @Test
    public void testCreate() throws ProvisionException {
        assertThrows(ProvisionException.class, () -> subject.create(null, null, null, null));
    }

    @Test
    public void testLoadWrongLocation() throws ProvisionException {
        Assertions.assertNull(subject.load(URI.create("file:/testFileUri"), 0, new NullProgressMonitor()));
    }

    @Test
    public void testLoad() throws ProvisionException, ComponentLookupException {
        LocalArtifactRepository repo = new LocalArtifactRepository(container.lookup(IProvisioningAgent.class),
                tempLocalMavenRepository.getLocalRepositoryIndex());
        repo.save();
        IArtifactRepository repo2 = subject.load(tempLocalMavenRepository.getLocalRepositoryRoot().toURI(), 0,
                new NullProgressMonitor());
        Assertions.assertEquals(repo, repo2);
    }
}
