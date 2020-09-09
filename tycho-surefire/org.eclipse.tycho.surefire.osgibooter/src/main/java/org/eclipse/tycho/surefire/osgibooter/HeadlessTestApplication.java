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

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class HeadlessTestApplication implements IApplication {

    public Object run(Object object) throws Exception {
        String[] args = Platform.getCommandLineArgs();
        return Integer.valueOf(OsgiSurefireBooter.run(args));
    }

    public Object start(IApplicationContext context) throws Exception {
        String[] args = Platform.getCommandLineArgs();
        return Integer.valueOf(OsgiSurefireBooter.run(args));
    }

    public void stop() {
        // nothing to be done here
    }

}
