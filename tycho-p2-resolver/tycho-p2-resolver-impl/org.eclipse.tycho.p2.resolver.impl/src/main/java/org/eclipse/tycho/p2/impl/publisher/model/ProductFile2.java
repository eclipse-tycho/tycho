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

import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.xml.sax.Attributes;

@SuppressWarnings({ "restriction" })
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
        String pluginId = attributes.getValue(ATTRIBUTE_ID);
        String pluginVersion = attributes.getValue(ATTRIBUTE_VERSION);

        FeatureEntry entry = new FeatureEntry(pluginId, pluginVersion != null ? pluginVersion : GENERIC_VERSION_NUMBER,
                true);
        entry.setFragment(Boolean.valueOf(fragment).booleanValue());

        String os = attributes.getValue(ATTRIBUTE_OS);
        String ws = attributes.getValue(ATTRIBUTE_WS);
        String arch = attributes.getValue(ATTRIBUTE_ARCH);
        if (os != null || ws != null || arch != null) {
            entry.setEnvironment(os, ws, arch, null);
        }

        if (fragment != null && new Boolean(fragment).booleanValue()) {
            fragments.add(entry);
        } else {
            plugins.add(entry);
        }
    }
}
