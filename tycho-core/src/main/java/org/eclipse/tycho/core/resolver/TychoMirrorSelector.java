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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.DefaultMirrorSelector;
import org.apache.maven.repository.MirrorSelector;
import org.apache.maven.settings.Mirror;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * A Tycho specific {@link MirrorSelector} in order to support mirror definitions based on a prefix
 * URL for full or partial mirror sites.
 * 
 * <p>
 * E.g. a mirror definition like that:
 * 
 * <pre>
 * &lt;mirror&gt;
 *   &lt;id&gt;example-mirror&lt;/id&gt;
 *   &lt;mirrorOf&gt;https://download.eclipse.org&lt;/mirrorOf&gt;
 *   &lt;url&gt;http://mirror.example.org/eclipse-mirror&lt;/url&gt;
 *   &lt;layout&gt;p2&lt;/layout&gt;
 *   &lt;mirrorOfLayouts&gt;p2&lt;/mirrorOfLayouts&gt;
 *  &lt;/mirror&gt;
 * </pre>
 * </p>
 * 
 * will cause the repository URL https://download.eclipse.org/eclipse/updates/4.6 to be mirrored to
 * http://mirror.example.org/eclipse-mirror/eclipse/updates/4.6 <br/>
 * 
 * 
 * (see #501809)
 *
 */
@Component(role = MirrorSelector.class, hint = "tycho")
public class TychoMirrorSelector extends DefaultMirrorSelector {

    @Override
    public Mirror getMirror(ArtifactRepository repository, List<Mirror> mirrors) {
        // if we find a mirror the default way (the maven way) we will use that mirror
        Mirror mavenMirror = super.getMirror(repository, mirrors);
        if (mavenMirror != null || mirrors == null) {
            return mavenMirror;
        }
        for (Mirror mirror : mirrors) {
            if (isPrefixMirrorOf(repository, mirror)) {
                // We will create a new Mirror that does
                // have the artifacts URL replaced with the Prefix URL from the mirror
                return createMirror(repository, mirror);
            }
        }
        return null;
    }

    private boolean isPrefixMirrorOf(ArtifactRepository repo, Mirror mirror) {
        boolean isMirrorOfRepoUrl = repo.getUrl() != null && repo.getUrl().startsWith(mirror.getMirrorOf());
        boolean matchesLayout = repo.getLayout() != null
                && repo.getLayout().getId().equals(mirror.getMirrorOfLayouts());
        return isMirrorOfRepoUrl && matchesLayout;
    }

    // We have to create a new Mirror
    private Mirror createMirror(ArtifactRepository repo, Mirror toMirror) {
        Mirror mirror = toMirror.clone();
        String urlToReplace = toMirror.getMirrorOf();
        String newUrl = StringUtils.replaceOnce(repo.getUrl(), urlToReplace, toMirror.getUrl());
        mirror.setUrl(newUrl);
        mirror.setId(toMirror.getId());
        return mirror;
    }

}
