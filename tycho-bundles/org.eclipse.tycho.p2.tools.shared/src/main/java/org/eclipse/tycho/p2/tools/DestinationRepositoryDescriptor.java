/*******************************************************************************
 * Copyright (c) 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DestinationRepositoryDescriptor {

    final File location;
    final String name;
    private final boolean compress;
    private final boolean xzCompress;
    private final boolean keepNonXzIndexFiles;
    private final boolean metaDataOnly;
    private final boolean append;
    private final Map<String, String> extraArtifactRepositoryProperties;
    private final List<RepositoryReference> repositoryReferences;

    public DestinationRepositoryDescriptor(File location, String name, boolean compress, boolean xzCompress,
            boolean keepNonXzIndexFiles, boolean metaDataOnly, boolean append,
            Map<String, String> extraArtifactRepositoryProperties, List<RepositoryReference> repositoryReferences) {
        this.location = location;
        this.name = name;
        this.compress = compress;
        this.xzCompress = xzCompress;
        this.keepNonXzIndexFiles = keepNonXzIndexFiles;
        this.metaDataOnly = metaDataOnly;
        this.append = append;
        this.extraArtifactRepositoryProperties = extraArtifactRepositoryProperties;
        this.repositoryReferences = repositoryReferences;
    }

    public DestinationRepositoryDescriptor(File location, String name) {
        this(location, name, true, true, false, false, true, Collections.emptyMap(), Collections.emptyList());
    }

    public File getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public boolean isCompress() {
        return compress;
    }

    public boolean isXZCompress() {
        return xzCompress;
    }

    public boolean shouldKeepNonXzIndexFiles() {
        return keepNonXzIndexFiles;
    }

    public boolean isMetaDataOnly() {
        return metaDataOnly;
    }

    public boolean isAppend() {
        return append;
    }

    public Map<String, String> getExtraArtifactRepositoryProperties() {
        return extraArtifactRepositoryProperties == null ? Collections.emptyMap() : extraArtifactRepositoryProperties;
    }

    public List<RepositoryReference> getRepositoryReferences() {
        return repositoryReferences == null ? Collections.emptyList() : repositoryReferences;
    }
}
