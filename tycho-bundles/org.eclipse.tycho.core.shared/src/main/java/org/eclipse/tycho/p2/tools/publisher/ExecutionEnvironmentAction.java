/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.core.ee.impl.StandardEEResolutionHints;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;

public class ExecutionEnvironmentAction implements IPublisherAction {

    private ExecutionEnvironmentResolutionHints ee;

    public ExecutionEnvironmentAction(ExecutionEnvironment ee) {
        this.ee = new StandardEEResolutionHints(ee);
    }

    @Override
    public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
        results.addIUs(ee.getMandatoryUnits(), IPublisherResult.NON_ROOT);
        return Status.OK_STATUS;
    }

}
