/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.its;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;

public class CustomTouchpointAction extends ProvisioningAction {

    @Override
    public IStatus execute(Map<String, Object> parameters) {
        System.out.println("The custom touchpoint action has been executed");
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(Map<String, Object> parameters) {
        return Status.OK_STATUS;
    }
}
