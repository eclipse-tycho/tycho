/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests.spi;

import java.io.File;

import org.eclipse.pde.core.target.ITargetLocation;

/**
 * In cases where we want to reuse the test-cases (specifically Tycho), one can plug in an own
 * loader that transform the xml into a suitable location for the tests.
 *
 */
public interface TargetLocationLoader extends Comparable<TargetLocationLoader> {

    static final String MAVEN_LOCATION_TYPE = "Maven";

    /**
     * @return the priority of this loader in case there are multiple ones, the one with the highest
     *         priority will be chosen for the test
     */
    int getPriority();

    /**
     * Resolve the given target xml fragment into a resolved location, the expectation is that:
     * <ul>
     * <li>The location is resolved</li>
     * <li>The status can be queried and return a result reflecting the outcome of< the
     * operation</li>
     * <li>getBundles() / getFeatures() return the appropriate items</li>
     * </ul>
     * 
     * @param targetXML
     * @return the resolved location
     * @throws Exception
     */
    ITargetLocation resolveMavenTarget(String targetXML, File tempDir) throws Exception;

    @Override
    default int compareTo(TargetLocationLoader o) {
        return Integer.compare(o.getPriority(), getPriority());
    }

}
