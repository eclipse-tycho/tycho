/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.eclipse.tycho.p2maven.repository.P2ArtifactRepositoryLayout;
import org.junit.Assert;
import org.junit.Test;

public class TychoMirrorSelectorTest {

    private TychoMirrorSelector selector = new TychoMirrorSelector();

    @Test
    public void testWithMatchingMirrorOfIds() {
        ArtifactRepository repository = createArtifactRepository("neon-repo",
                "https://download.eclipse.org/eclipse/update/4.6");
        Mirror mirrorWithMatchingMirrorOfIds = createMirror("myId", "https://foo.bar", "neon-repo");
        Mirror selectedMirror = selector.getMirror(repository, Arrays.asList(mirrorWithMatchingMirrorOfIds));
        Assert.assertEquals(mirrorWithMatchingMirrorOfIds, selectedMirror);
    }

    @Test
    public void testWithPrefixMirror() {
        ArtifactRepository repository = createArtifactRepository("neon-repo",
                "https://download.eclipse.org/eclipse/update/4.6");
        Mirror prefixMatchingMirror1 = createMirror("myId1", "https://foo.bar", "https://download.eclipse.org");
        Mirror prefixMatchingMirror2 = createMirror("myId2", "https://foo1.bar1", "https://abc.vxz");
        Mirror selectedMirror = selector.getMirror(repository,
                Arrays.asList(prefixMatchingMirror1, prefixMatchingMirror2));
        Assert.assertNotNull(selectedMirror);
        Assert.assertEquals("https://foo.bar/eclipse/update/4.6", selectedMirror.getUrl());
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
