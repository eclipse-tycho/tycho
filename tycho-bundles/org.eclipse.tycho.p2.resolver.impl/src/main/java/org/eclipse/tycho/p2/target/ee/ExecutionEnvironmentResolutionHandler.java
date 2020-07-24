/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

public abstract class ExecutionEnvironmentResolutionHandler {

    public static ExecutionEnvironmentResolutionHandler adapt(ExecutionEnvironmentConfiguration eeConfiguration) {
        if (eeConfiguration.ignoreExecutionEnvironment()) {
            return new StandardEEResolutionHandler(NoExecutionEnvironmentResolutionHints.INSTANCE);
        }
        if (eeConfiguration.isIgnoredByResolver()) {
            return new StandardEEResolutionHandler(new AllKnownEEsResolutionHints(eeConfiguration.getAllKnownEEs()));
        } else if (eeConfiguration.isCustomProfile()) {
            // TODO consider whether custom and standard EE couldn't build their "hints" the same way
            return new CustomEEResolutionHandler(eeConfiguration);
        } else {
            return new StandardEEResolutionHandler(
                    new StandardEEResolutionHints(eeConfiguration.getFullSpecification()));
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
