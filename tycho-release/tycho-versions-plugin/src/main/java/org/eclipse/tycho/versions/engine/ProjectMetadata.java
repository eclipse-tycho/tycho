/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectMetadata {
    private final File basedir;

    private Map<Object, Object> metadata = new LinkedHashMap<>();

    public ProjectMetadata(File basedir) {
        this.basedir = basedir;
    }

    public <T> T getMetadata(Class<T> type) {
        return type.cast(metadata.get(type));
    }

    public <T> void putMetadata(T metadata) {
        this.metadata.put(metadata.getClass(), metadata);
    }

    public File getBasedir() {
        return basedir;
    }

    @Override
    public String toString() {
        return basedir.toString();
    }

}
