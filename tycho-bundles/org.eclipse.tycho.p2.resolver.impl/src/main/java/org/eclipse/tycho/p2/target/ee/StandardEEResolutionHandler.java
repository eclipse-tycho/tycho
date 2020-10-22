/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

class StandardEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    public StandardEEResolutionHandler(ExecutionEnvironmentResolutionHints resolutionHints) {
        super(resolutionHints);
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        // standard EEs are fully specified - no need to read anything from the target platform
    }

}
