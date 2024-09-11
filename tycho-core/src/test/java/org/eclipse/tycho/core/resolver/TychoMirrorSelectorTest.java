/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.tycho.p2maven.repository.DefaultMavenRepositorySettings;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TychoMirrorSelectorTest {

    private DefaultMavenRepositorySettings selector;

    @Before
    public void setup() {
        MirrorSelector ms = mock(MirrorSelector.class);
        RepositorySystemSession repo = mock(RepositorySystemSession.class);
        when(repo.getMirrorSelector()).thenReturn(ms);
        doAnswer(invocation -> {

            final Object[] args = invocation.getArguments();
            if (args[0] instanceof RemoteRepository rr && rr.getId().equals("neon-repo-with-id")) {
                return new RemoteRepository.Builder("myId", rr.getContentType(), "http://foo.bar").build();
            }
            return null;
        }).when(ms).getMirror(any());
        selector = new DefaultMavenRepositorySettings(repo);

    }

    @Test
    public void testWithMatchingMirrorOfIds() {
        ArtifactRepository repository = createArtifactRepository("neon-repo-with-id",
                "https://download.eclipse.org/eclipse/update/4.6");
        Mirror mirrorWithMatchingMirrorOfIds = createMirror("myId", "http://foo.bar", "neon-repo-with-id");
        Mirror selectedMirror = selector.getTychoMirror(repository, Arrays.asList(mirrorWithMatchingMirrorOfIds));
        Assert.assertEquals(mirrorWithMatchingMirrorOfIds.getId(), selectedMirror.getId());
    }

    @Test
    public void testWithPrefixMirror() {
        ArtifactRepository repository = createArtifactRepository("neon-repo",
                "https://download.eclipse.org/eclipse/update/4.6");
        Mirror prefixMatchingMirror1 = createMirror("myId1", "http://foo.bar", "https://download.eclipse.org");
        Mirror prefixMatchingMirror2 = createMirror("myId2", "http://foo1.bar1", "http://abc.vxz");
        Mirror selectedMirror = selector.getTychoMirror(repository,
                Arrays.asList(prefixMatchingMirror1, prefixMatchingMirror2));
        Assert.assertNotNull(selectedMirror);
        Assert.assertEquals("http://foo.bar/eclipse/update/4.6", selectedMirror.getUrl());
    }

    private ArtifactRepository createArtifactRepository(String id, String url) {
        ArtifactRepository repository = new MavenArtifactRepository();
        repository.setId(id);
        repository.setUrl(url);
        repository.setLayout(new P2ArtifactRepositoryLayout());
        return repository;
    }

    private Mirror createMirror(String id, String url, String mirrorOf) {
        Mirror mirror = new Mirror();
        mirror.setId(id);
        mirror.setUrl(url);
        mirror.setMirrorOf(mirrorOf);
        mirror.setLayout(P2ArtifactRepositoryLayout.ID);
        mirror.setMirrorOfLayouts(P2ArtifactRepositoryLayout.ID);
        return mirror;
    }

}
