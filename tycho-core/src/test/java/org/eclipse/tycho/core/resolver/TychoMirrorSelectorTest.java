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

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.repository.DefaultMirrorSelector;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.eclipse.tycho.p2maven.repository.DefaultMavenRepositorySettings;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TychoMirrorSelectorTest {

    private DefaultMavenRepositorySettings selector;

    @Before
    public void setup() {
        RepositorySystem repo = mock(RepositorySystem.class);
        doAnswer(new Answer<Mirror>() {

            @Override
            public Mirror answer(InvocationOnMock invocation) throws Throwable {

                final Object[] args = invocation.getArguments();
                return new DefaultMirrorSelector().getMirror((ArtifactRepository) args[0], (List<Mirror>) args[1]);
            }
        }).when(repo).getMirror(any(), any());
        selector = new DefaultMavenRepositorySettings(repo);

    }

    @Test
    public void testWithMatchingMirrorOfIds() {
        ArtifactRepository repository = createArtifactRepository("neon-repo",
                "https://download.eclipse.org/eclipse/update/4.6");
        Mirror mirrorWithMatchingMirrorOfIds = createMirror("myId", "http://foo.bar", "neon-repo");
        Mirror selectedMirror = selector.getTychoMirror(repository, Arrays.asList(mirrorWithMatchingMirrorOfIds));
        Assert.assertEquals(mirrorWithMatchingMirrorOfIds, selectedMirror);
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
