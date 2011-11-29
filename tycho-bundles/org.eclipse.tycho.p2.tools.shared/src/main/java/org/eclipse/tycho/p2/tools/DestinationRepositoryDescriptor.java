/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

import java.io.File;

public class DestinationRepositoryDescriptor {

    final File location;
    final String name;
    private final boolean compress;
    private final boolean metaDataOnly;
    private final boolean append;

    public DestinationRepositoryDescriptor(File location, String name, boolean compress, boolean metaDataOnly,
            boolean append) {
        this.location = location;
        this.name = name;
        this.compress = compress;
        this.metaDataOnly = metaDataOnly;
        this.append = append;
    }

    public DestinationRepositoryDescriptor(File location, String name) {
        this(location, name, true, false, true);
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

    public boolean isMetaDataOnly() {
        return metaDataOnly;
    }

    public boolean isAppend() {
        return append;
    }

}
