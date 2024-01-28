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

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

public class ImportedPackages {

    //import package order is not important so we can sort here by name
    private Map<String, ImportedPackage> packages = new TreeMap<>();

    public ImportedPackages(String manifestValue) {
        if (manifestValue != null) {
            Parameters header = OSGiHeader.parseHeader(manifestValue);
            for (Entry<String, Attrs> entry : header.entrySet()) {
                packages.put(entry.getKey(), new ImportedPackage(this, entry.getKey(), entry.getValue()));
            }
        }
    }

    public Stream<ImportedPackage> packages() {
        return packages.values().stream();

    }

}
