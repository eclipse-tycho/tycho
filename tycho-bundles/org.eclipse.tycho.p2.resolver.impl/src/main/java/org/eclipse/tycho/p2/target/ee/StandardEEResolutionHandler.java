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
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

class StandardEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    private ExecutionEnvironmentConfiguration environmentConfiguration;

    public StandardEEResolutionHandler(ExecutionEnvironmentResolutionHints resolutionHints,
            ExecutionEnvironmentConfiguration environmentConfiguration) {
        super(resolutionHints);
        this.environmentConfiguration = environmentConfiguration;
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        if (environmentConfiguration.ignoreExecutionEnvironment()) {
            //if it is ignored not setting the specification leads to strange errors in downstream mojos...
            environmentConfiguration.setFullSpecificationForCustomProfile(Collections.emptyList());
            return;
        }
        // standard EEs are fully specified - no need to read anything from the target platform
    }

}
