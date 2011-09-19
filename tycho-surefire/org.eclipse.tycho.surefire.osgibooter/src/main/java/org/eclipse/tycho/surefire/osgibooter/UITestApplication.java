/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

@SuppressWarnings("deprecation")
public class UITestApplication extends AbstractUITestApplication implements IApplication {

    private IApplicationContext fContext;

    public void stop() {
    }

    @Override
    protected void runApplication(Object application, String[] args) throws Exception {
        if (application instanceof IPlatformRunnable) {
            ((IPlatformRunnable) application).run(args);
        } else if (application instanceof IApplication) {
            ((IApplication) application).start(fContext);
        }
    }

    public Object start(IApplicationContext context) throws Exception {
        this.fContext = context;
        return run(Platform.getApplicationArgs());
    }

}
