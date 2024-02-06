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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Mappings {

    private Map<RequiredBundle, Collection<ImportedPackage>> contributedPackages = new HashMap<>();

    private Map<ImportedPackage, Collection<RequiredBundle>> contributingBundles = new HashMap<>();

    public Stream<ImportedPackage> contributedPackages(RequiredBundle bundle) {
        return contributedPackages.getOrDefault(bundle, List.of()).stream()
                .sorted(Comparator.comparing(ImportedPackage::getPackageName));
    }

    public void addContribution(ImportedPackage pkg, RequiredBundle requiredBundle) {
        contributedPackages.computeIfAbsent(requiredBundle, x -> new HashSet<>()).add(pkg);
        contributingBundles.computeIfAbsent(pkg, x -> new HashSet<>()).add(requiredBundle);
    }

}
