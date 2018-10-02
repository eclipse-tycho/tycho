/*******************************************************************************
 * Copyright (c) 2011 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DestinationRepositoryDescriptor {

    final File location;
    final String name;
    private final boolean compress;
    private final boolean xzCompress;
    private final boolean keepNonXzIndexFiles;
    private final boolean metaDataOnly;
    private final boolean append;
    private Map<String, String> extraArtifactRepositoryProperties;

    public DestinationRepositoryDescriptor(File location, String name, boolean compress, boolean xzCompress,
            boolean keepNonXzIndexFiles, boolean metaDataOnly, boolean append,
            Map<String, String> extraArtifactRepositoryProperties) {
        this.location = location;
        this.name = name;
        this.compress = compress;
        this.xzCompress = xzCompress;
        this.keepNonXzIndexFiles = keepNonXzIndexFiles;
        this.metaDataOnly = metaDataOnly;
        this.append = append;
        this.extraArtifactRepositoryProperties = extraArtifactRepositoryProperties;
    }

    public DestinationRepositoryDescriptor(File location, String name) {
        this(location, name, true, true, false, false, true, Collections.emptyMap());
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

}
