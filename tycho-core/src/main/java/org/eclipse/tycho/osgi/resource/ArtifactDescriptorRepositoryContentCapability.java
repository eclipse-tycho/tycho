/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;

/**
 * Implements a {@link Capability} that supply {@link RepositoryContent}
 */
public class ArtifactDescriptorRepositoryContentCapability implements Capability, RepositoryContent {

    private IArtifactDescriptor descriptor;
    private IArtifactRepository artifactRepository;
    private Map<String, Object> attributes;
    private Resource resource;

    public ArtifactDescriptorRepositoryContentCapability(Resource resource, IArtifactDescriptor descriptor,
            IArtifactRepository artifactRepository) {
        this.resource = resource;
        this.descriptor = descriptor;
        this.artifactRepository = artifactRepository;
//      <capability namespace='osgi.content'>
//      <attribute name='osgi.content' value='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
//      <attribute name='url' value='http://www.acme.com/repository/org/acme/pool/org.acme.pool-1.5.6.jar'/>
//      <attribute name='size' type='Long' value='4405'/>
//      <attribute name='mime' value='application/vnd.osgi.bundle'/>
//    </capability>
        HashMap<String, Object> map = new HashMap<>(descriptor.getProperties());
        String content = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_CHECKSUM + ".sha-256");
        if (content != null) {
            map.put("osgi.content", content);
        }
        //TODO can we get the URL from P2?
        String size = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE);
        if (size != null) {
            map.put("size", Long.parseLong(size));
        }
        String contentType = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE);
        if (contentType != null) {
            map.put("mime", contentType);
        }
        this.attributes = Map.copyOf(map);
    }

    @Override
    public InputStream getContent() {
        // TODO can we get this a bit more efficient without copy around?
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IStatus status = artifactRepository.getArtifact(descriptor, outputStream, null);
        if (!status.isOK()) {
            return new InputStream() {

                @Override
                public int read() throws IOException {
                    throw new IOException(status.toString(), status.getException());
                }
            };
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public String getNamespace() {
        return "osgi.content";
    }

    @Override
    public Map<String, String> getDirectives() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

}
