/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class UITestApplication extends AbstractUITestApplication implements IApplication {

    private IApplicationContext fContext;

    public void stop() {
    }

    @Override
    protected void runApplication(Object application, String[] args) throws Exception {
        if (application instanceof IApplication) {
            ((IApplication) application).start(fContext);
        }
    }

    public Object start(IApplicationContext context) throws Exception {
        this.fContext = context;
        return run(Platform.getApplicationArgs());
    }

}
