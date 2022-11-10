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

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.shared.MavenLogger;

public abstract class ExecutionEnvironmentResolutionHandler {

    public static ExecutionEnvironmentResolutionHandler adapt(ExecutionEnvironmentConfiguration eeConfiguration,
            MavenLogger logger) {
        if (eeConfiguration.ignoreExecutionEnvironment()) {
            return new StandardEEResolutionHandler(NoExecutionEnvironmentResolutionHints.INSTANCE, eeConfiguration,
                    logger);
        }
        if (eeConfiguration.isIgnoredByResolver()) {
            return new StandardEEResolutionHandler(new AllKnownEEsResolutionHints(eeConfiguration.getAllKnownEEs()),
                    eeConfiguration, logger);
        } else if (eeConfiguration.isCustomProfile()) {
            // TODO consider whether custom and standard EE couldn't build their "hints" the same way
            return new CustomEEResolutionHandler(eeConfiguration);
        } else {
            return new StandardEEResolutionHandler(
                    new StandardEEResolutionHints(eeConfiguration.getFullSpecification()), eeConfiguration, logger);
        }
    }

    // BEGIN abstract base class

    private final ExecutionEnvironmentResolutionHints resolutionHints;

    public ExecutionEnvironmentResolutionHandler(ExecutionEnvironmentResolutionHints resolutionHints) {
        this.resolutionHints = resolutionHints;
    }

    public final ExecutionEnvironmentResolutionHints getResolutionHints() {
        return resolutionHints;
    }

    public abstract void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent);

}
