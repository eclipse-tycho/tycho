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

public class HeadlessTestApplication implements IApplication {

    public Object start(IApplicationContext context) throws Exception {
        String[] args = Platform.getCommandLineArgs();
        return Integer.valueOf(OsgiSurefireBooter.run(args));
    }

    public void stop() {
        // nothing to be done here
    }

}
