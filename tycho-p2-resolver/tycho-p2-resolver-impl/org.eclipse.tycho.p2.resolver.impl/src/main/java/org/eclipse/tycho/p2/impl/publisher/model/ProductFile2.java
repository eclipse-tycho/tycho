/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.model;

import java.util.ArrayList;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.xml.sax.Attributes;

@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
public class ProductFile2 extends ProductFile {
    protected static final String ATTRIBUTE_OS = "os";

    protected static final String ATTRIBUTE_WS = "ws";

    protected static final String ATTRIBUTE_ARCH = "arch";

    public ProductFile2(String location) throws Exception {
        super(location);
    }

    @Override
    protected void processPlugin(Attributes attributes) {
        String fragment = attributes.getValue(ATTRIBUTE_FRAGMENT);
        String id = attributes.getValue(ATTRIBUTE_ID);
        String version = attributes.getValue(ATTRIBUTE_VERSION);
        String os = attributes.getValue(ATTRIBUTE_OS);
        String ws = attributes.getValue(ATTRIBUTE_WS);
        String arch = attributes.getValue(ATTRIBUTE_ARCH);
        IVersionedId name;
        if (os != null || ws != null || arch != null) {
            name = new VersionedName2(id, version, os, ws, arch);
        } else {
            name = new VersionedId(id, version);
        }
        if (fragment != null && new Boolean(fragment).booleanValue()) {
            if (fragments == null)
                fragments = new ArrayList();
            fragments.add(name);
        } else {
            if (plugins == null)
                plugins = new ArrayList();
            plugins.add(name);
        }
    }
}
