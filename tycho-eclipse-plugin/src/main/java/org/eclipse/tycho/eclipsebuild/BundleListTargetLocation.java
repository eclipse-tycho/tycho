/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;

class BundleListTargetLocation implements ITargetLocation {

    private TargetBundle[] bundles;

    public BundleListTargetLocation(TargetBundle[] bundles) {
        this.bundles = bundles;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
        return Status.OK_STATUS;
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    @Override
    public IStatus getStatus() {
        return Status.OK_STATUS;
    }

    @Override
    public String getType() {
        return "BundleList"; //$NON-NLS-1$
    }

    @Override
    public String getLocation(boolean resolve) throws CoreException {
        return null;
    }

    @Override
    public TargetBundle[] getBundles() {
        return this.bundles;
    }

    @Override
    public TargetFeature[] getFeatures() {
        return null;
    }

    @Override
    public String[] getVMArguments() {
        return null;
    }

    @Override
    public String serialize() {
        return null;
    }

}
