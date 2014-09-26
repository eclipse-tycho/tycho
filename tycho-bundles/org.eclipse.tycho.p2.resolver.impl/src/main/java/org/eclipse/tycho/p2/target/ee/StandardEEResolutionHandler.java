/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
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
