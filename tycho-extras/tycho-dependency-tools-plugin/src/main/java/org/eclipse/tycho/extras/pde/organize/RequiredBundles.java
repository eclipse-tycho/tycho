/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.organize;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.tycho.ArtifactKey;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class RequiredBundles {

    //The order of require bundle is important so we need to retain it
    private Map<String, RequiredBundle> bundles = new LinkedHashMap<>();

    public RequiredBundles(String manifestValue) {
        if (manifestValue != null) {
            Parameters header = OSGiHeader.parseHeader(manifestValue);
            for (Entry<String, Attrs> entry : header.entrySet()) {
                String bsn = entry.getKey();
                Attrs attrs = entry.getValue();
                bundles.put(bsn, new RequiredBundle(bsn, attrs));
            }
        }
    }

    public Stream<RequiredBundle> bundles() {
        return bundles.values().stream();

    }

    public void addPackageMapping(ImportedPackage pkg, ArtifactKey key, Mappings mappings) {
        RequiredBundle requiredBundle = bundles.get(key.getId());
        if (requiredBundle != null) {
            mappings.addContribution(pkg, requiredBundle);
        } else {
            //might be something contributed by a reexported bundle...
            bundles.values().stream().flatMap(rb -> rb.childs(true))
                    .filter(rb -> rb.getBundleSymbolicName().equals(key.getId())
                            && rb.getVersionRange().toString().equals(key.getVersion()))
                    .forEach(rb -> mappings.addContribution(pkg, rb));
        }
    }

    public boolean isEmpty() {
        return bundles.isEmpty();
    }

}
