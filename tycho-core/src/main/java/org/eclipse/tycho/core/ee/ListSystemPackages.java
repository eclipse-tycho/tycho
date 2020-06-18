/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
 * a copy of this class has to be compiled targeting Java 9 bytecode and jar'd, and included as resource in
 * the artifact jar root.
 * It's used to ask external JREs (defined in toolchains) for their system packages.
 */
public class ListSystemPackages {

    public static void main(String[] args) {
        getCurrentJREPackages().forEach(System.out::println);
    }

    public static Set<String> getCurrentJREPackages() {
        return ModuleLayer.boot().modules().stream().map(Module::getDescriptor) //
                .flatMap(desc -> desc.isAutomatic() ? //
                        desc.packages().stream() : //
                        desc.exports().stream()
                                .filter(Predicate.not(java.lang.module.ModuleDescriptor.Exports::isQualified))
                                .map(java.lang.module.ModuleDescriptor.Exports::source) //
                ).collect(Collectors.toSet());
    }

}
