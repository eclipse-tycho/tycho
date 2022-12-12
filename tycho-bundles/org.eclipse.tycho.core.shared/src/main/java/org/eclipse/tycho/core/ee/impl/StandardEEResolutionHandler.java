/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #519 - Provide better feedback to the user about the cause of a failed resolution
 *******************************************************************************/
package org.eclipse.tycho.core.ee.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.shared.MavenLogger;

class StandardEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    private ExecutionEnvironmentConfiguration environmentConfiguration;
    private MavenLogger logger;

    public StandardEEResolutionHandler(ExecutionEnvironmentResolutionHints resolutionHints,
            ExecutionEnvironmentConfiguration environmentConfiguration, MavenLogger logger) {
        super(resolutionHints);
        this.environmentConfiguration = environmentConfiguration;
        this.logger = logger;
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        if (environmentConfiguration.ignoreExecutionEnvironment()) {
            //and we might want to inform the user about ignored items...
            logger.info("The following Execution Environments are currently known but are ignored by configuration:");
            Map<String, Collection<String>> map = targetPlatformContent.stream()//
                    .filter(ExecutionEnvironmentResolutionHints::isJreUnit)//
                    .flatMap(StandardEEResolutionHandler::getEE).collect(Collectors.groupingBy(Entry::getValue,
                            TreeMap::new, Collectors.mapping(Entry::getKey, Collectors.toCollection(TreeSet::new))));
            map.entrySet().forEach(entry -> {
                logger.info(
                        "    " + entry.getKey() + " -> " + entry.getValue().stream().collect(Collectors.joining(", ")));
            });
            return;
        }
        // standard EEs are fully specified - no need to read anything from the target platform
    }

    private static Stream<Entry<String, String>> getEE(IInstallableUnit specificationUnit) {

        return specificationUnit.getProvidedCapabilities().stream()
                .filter(capability -> JREAction.NAMESPACE_OSGI_EE.equals(capability.getNamespace())).map(capability -> {
                    return new SimpleEntry<>(capability.getName(), capability.getVersion().toString());
                });
    }

}
