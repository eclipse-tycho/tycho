/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.publisher;

import java.util.List;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IVersionedId;
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
        boolean isFragment = Boolean.parseBoolean(fragment);
        FeatureEntry entry = new FeatureEntry(pluginId, pluginVersion != null ? pluginVersion : GENERIC_VERSION_NUMBER,
                true);
        entry.setFragment(isFragment);

        String os = attributes.getValue(ATTRIBUTE_OS);
        String ws = attributes.getValue(ATTRIBUTE_WS);
        String arch = attributes.getValue(ATTRIBUTE_ARCH);
        if (os != null || ws != null || arch != null) {
            entry.setEnvironment(os, ws, arch, null);
        }

        if (isFragment) {
            fragments.add(entry);
        } else {
            plugins.add(entry);
        }
    }

    @Override
    public List<IVersionedId> getFeatures() {
        /*
         * Unlike the final IU, the dependency-only IU shall depend on root features so that the
         * dependency resolver correctly discovers dependencies to root features from the reactor.
         */
        return getFeatures(INCLUDED_FEATURES | ROOT_FEATURES);
    }

}
