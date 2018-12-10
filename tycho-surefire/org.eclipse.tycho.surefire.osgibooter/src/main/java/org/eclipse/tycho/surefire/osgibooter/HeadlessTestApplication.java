/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
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

@SuppressWarnings("deprecation")
public class HeadlessTestApplication implements IPlatformRunnable {

    public Object run(Object object) throws Exception {
        String[] args = Platform.getCommandLineArgs();

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=540222: log stack traces when we are nearing the test timeout
        String timeoutParameter = getParameterValue(args, "-timeout");
        if (timeoutParameter != null) {
            DumpStackTracesTimer.startStackDumpTimeoutTimer(timeoutParameter);
        }

        return Integer.valueOf(OsgiSurefireBooter.run(args));
    }

    private static String getParameterValue(String[] args, String parameterName) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(parameterName) && i < args.length - 1) //$NON-NLS-1$
                return args[i + 1];
        }
        return null;
    }
}
